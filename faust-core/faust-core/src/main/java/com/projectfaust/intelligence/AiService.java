package com.projectfaust.intelligence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Low-level AI gateway service for Project Faust.
 *
 * <p><b>Priority chain:</b></p>
 * <ol>
 *   <li><b>Claude (Anthropic)</b> — primary. Best for structured analytical
 *       text, intelligence briefs, and reasoning. REST endpoint:
 *       {@code POST https://api.anthropic.com/v1/messages}.</li>
 *   <li><b>OpenAI GPT-4o</b> — first fallback when Claude quota is exhausted
 *       or key is not configured.</li>
 *   <li><b>Google Gemini</b> — last resort fallback.</li>
 * </ol>
 *
 * <p>All model identifiers are configurable via {@code application.yaml}:</p>
 * <pre>
 * faust:
 *   ai:
 *     claude-model: claude-sonnet-4-6
 *     openai-model: gpt-4o
 *     gemini-model: gemini-2.5-flash
 *     claude-api-key: sk-ant-...
 *     max-tokens: 2048
 * </pre>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
public class AiService {

    // ── Clients ───────────────────────────────────────────────────────────────
    private final RestClient claudeClient;
    private final RestClient openAiClient;
    private final RestClient googleClient;

    // ── Config ────────────────────────────────────────────────────────────────
    private final String claudeKey;
    private final String googleKey;

    @Value("${faust.ai.claude-model:claude-sonnet-4-6}")
    private String claudeModel;

    @Value("${faust.ai.openai-model:gpt-4o}")
    private String openAiModel;

    @Value("${faust.ai.gemini-model:gemini-2.5-flash}")
    private String geminiModel;

    @Value("${faust.ai.max-tokens:2048}")
    private int maxTokens;

    public AiService(
            @Value("${faust.ai.claude-api-key:}") String claudeKey,
            @Value("${spring.ai.openai.api-key:}") String openAiKey,
            @Value("${spring.ai.google.api-key:}") String googleKey) {

        this.claudeKey = claudeKey;
        this.googleKey = googleKey;

        log.info("AI_SERVICE: Claude key configured:  {}",  !claudeKey.isBlank());
        log.info("AI_SERVICE: OpenAI key configured:  {}",  !openAiKey.isBlank());
        log.info("AI_SERVICE: Google key configured:  {}",  !googleKey.isBlank());

        this.claudeClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com/v1/messages")
                .defaultHeader("x-api-key", claudeKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("content-type", "application/json")
                .build();

        this.openAiClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .defaultHeader("Authorization", "Bearer " + openAiKey)
                .build();

        this.googleClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    /**
     * Sends a prompt to the AI layer.
     * Tries Claude → OpenAI → Gemini in order until one succeeds.
     *
     * @param message the prompt text.
     * @return the AI-generated response text.
     */
    public String askGpt(String message) {
        // 1. Claude — primary
        if (claudeKey != null && !claudeKey.isBlank()) {
            try {
                log.info("AI_SERVICE: Sending prompt to Claude ({}).", claudeModel);
                return callClaude(message);
            } catch (Exception e) {
                log.warn("AI_SERVICE: Claude failed — falling back to OpenAI. Reason: {}",
                        e.getMessage());
            }
        } else {
            log.debug("AI_SERVICE: Claude key not configured — skipping.");
        }

        // 2. OpenAI — first fallback
        try {
            log.info("AI_SERVICE: Sending prompt to OpenAI ({}).", openAiModel);
            return callOpenAi(message);
        } catch (Exception e) {
            log.warn("AI_SERVICE: OpenAI failed — falling back to Gemini. Reason: {}",
                    e.getMessage());
        }

        // 3. Gemini — last resort
        return callGemini(message);
    }

    /**
     * Returns the name of whichever model is currently configured as primary.
     * Used by {@link AiAnalystService} to persist the correct model version
     * on generated intelligence reports.
     */
    public String getActiveModelName() {
        if (claudeKey != null && !claudeKey.isBlank()) return claudeModel;
        return geminiModel; // openAI has no reliable "active" signal without a call
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — Claude
    // ─────────────────────────────────────────────────────────────────────────

    private String callClaude(String message) {
        Map<String, Object> body = Map.of(
                "model", claudeModel,
                "max_tokens", maxTokens,
                "messages", List.of(Map.of("role", "user", "content", message))
        );

        ClaudeResponse res = claudeClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ClaudeResponse.class);

        if (res == null || res.content() == null || res.content().isEmpty()) {
            throw new IllegalStateException("AI_SERVICE: Claude returned empty response.");
        }

        return res.content().stream()
                .filter(c -> "text".equals(c.type()))
                .map(ClaudeContent::text)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "AI_SERVICE: No text block in Claude response."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — OpenAI
    // ─────────────────────────────────────────────────────────────────────────

    private String callOpenAi(String message) {
        Map<String, Object> body = Map.of(
                "model", openAiModel,
                "messages", List.of(Map.of("role", "user", "content", message))
        );

        OpenAiResponse res = openAiClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OpenAiResponse.class);

        if (res == null || res.choices() == null || res.choices().isEmpty()) {
            throw new IllegalStateException("AI_SERVICE: OpenAI returned empty response.");
        }

        return res.choices().get(0).message().content();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — Gemini
    // ─────────────────────────────────────────────────────────────────────────

    private String callGemini(String message) {
        if (googleKey == null || googleKey.isBlank()) {
            log.error("AI_SERVICE: All AI providers exhausted and Google key not configured.");
            return "ERROR: No AI provider available. Configure at least one API key.";
        }

        String uri = "/v1/models/" + geminiModel + ":generateContent?key=" + googleKey;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", message)))
                )
        );

        try {
            log.info("AI_SERVICE: Sending prompt to Gemini ({}).", geminiModel);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = googleClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return extractGeminiText(response);

        } catch (Exception e) {
            log.error("AI_SERVICE: All AI providers failed. Last error: {}", e.getMessage());
            return "ERROR: All AI providers failed. Last error: " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractGeminiText(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content =
                    (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts =
                    (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            log.error("AI_SERVICE: Failed to parse Gemini response — {}", e.getMessage());
            return "ERROR: Failed to parse AI response.";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner records — response shapes
    // ─────────────────────────────────────────────────────────────────────────

    /** Claude /v1/messages response */
    private record ClaudeResponse(List<ClaudeContent> content, String model) {}
    private record ClaudeContent(String type, String text) {}

    /** OpenAI /v1/chat/completions response */
    private record OpenAiResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
    private record Message(String content) {}
}