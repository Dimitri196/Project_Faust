package com.projectfaust.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final RestClient openAiClient;
    private final RestClient googleClient;
    private final String googleKey;

    public AiService(
            @Value("${spring.ai.openai.api-key:}") String openAiKey,
            @Value("${spring.ai.google.api-key:}") String googleKey) {

        this.googleKey = googleKey;

        System.out.println("DEBUG: Google Key načten: " + (googleKey != null && !googleKey.isEmpty() ? "ANO" : "NE"));

        this.openAiClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .defaultHeader("Authorization", "Bearer " + openAiKey)
                .build();

        this.googleClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
                .build();
    }

    public String askGpt(String message) {
        try {
            System.out.println("Zkouším OpenAI...");
            return callOpenAi(message);
        } catch (Exception e) {
            System.err.println("OpenAI selhalo (Quota). Zkouším Google Gemini...");
            return callGemini(message);
        }
    }

    private String callOpenAi(String message) {
        Map<String, Object> body = Map.of(
                "model", "gpt-4o",
                "messages", List.of(Map.of("role", "user", "content", message))
        );

        OpenAiResponse res = openAiClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OpenAiResponse.class);

        return res.choices().get(0).message().content();
    }

    private String callGemini(String message) {
        if (googleKey == null || googleKey.isBlank()) {
            return "CHYBA: Google API klíč není nastaven!";
        }

        String absoluteUri = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + googleKey;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", message)))
                )
        );

        try {
            System.out.println("DEBUG: Odesílám požadavek na Gemini 2.5 Flash...");

            Map response = googleClient.post()
                    .uri(absoluteUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return extractGeminiText(response);

        } catch (Exception e) {
            return "Selhalo i Gemini 2.5. Chyba: " + e.getMessage();
        }
    }

    private String extractGeminiText(Map response) {
        try {
            List candidates = (List) response.get("candidates");
            Map firstCandidate = (Map) candidates.get(0);
            Map content = (Map) firstCandidate.get("content");
            List parts = (List) content.get("parts");
            Map firstPart = (Map) parts.get(0);
            return (String) firstPart.get("text");
        } catch (Exception e) {
            return "Chyba při parsování odpovědi od Gemini. Raw response: " + response;
        }
    }

    private record OpenAiResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
    private record Message(String content) {}
}
