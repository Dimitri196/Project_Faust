package com.projectfaust.intelligence;

import com.projectfaust.person.Person;
import com.projectfaust.person.PersonRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service orchestrating AI-driven intelligence brief generation for persons
 * within Project Faust.
 *
 * <p>Implements a 7-day report cache — if a fresh report exists for the subject
 * it is returned immediately without re-querying the AI model. New reports are
 * persisted to the {@link IntelligenceReport} table for historical tracking.</p>
 *
 * <p><b>FIXED:</b> {@code modelVersion} is now sourced from
 * {@link AiService#getActiveModelName()} rather than being hardcoded as
 * {@code "gemini-3-flash"} — which was both wrong (the actual model called is
 * {@code gemini-2.5-flash}) and fragile (would silently persist the wrong model
 * name every time the model was upgraded in {@code application.yaml}).</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalystService {

    private final PersonRepository personRepository;
    private final IntelligenceReportRepository reportRepository;
    private final AiService aiService;

    /**
     * Generates or retrieves a cached AI intelligence brief for the specified person.
     *
     * <p>If a report generated within the last 7 days exists, it is returned
     * without calling the AI model. Otherwise a new report is generated, persisted,
     * and returned.</p>
     *
     * @param externalId the public UUID of the subject person.
     * @return the AI-generated intelligence analysis text.
     * @throws EntityNotFoundException if no person matches the given UUID.
     */
    @Transactional
    public String getAiIntelligenceBrief(UUID externalId) {
        Person person = personRepository.findFullProfileByExternalId(externalId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "PERSON_NOT_FOUND: " + externalId));

        // Return cached report if generated within the last 7 days
        Optional<IntelligenceReport> existing =
                reportRepository.findFirstByPersonExternalIdOrderByGeneratedAtDesc(externalId);

        if (existing.isPresent() &&
                existing.get().getGeneratedAt()
                        .isAfter(OffsetDateTime.now().minusDays(7))) {
            log.info("FAUST_AI: Returning cached report for person {}.", externalId);
            return existing.get().getAnalysisResult();
        }

        // Build AI prompt and call the model
        String prompt = buildPrompt(person);
        String rawAnalysis = aiService.askGpt(prompt);

        // Flag high-value intelligence terms in the analysis output
        String finalAnalysis = applyIntelligenceFlags(rawAnalysis);

        // FIXED: model version sourced from AiService rather than hardcoded —
        // was "gemini-3-flash" which didn't match the actual model called.
        IntelligenceReport report = IntelligenceReport.builder()
                .person(person)
                .analysisResult(finalAnalysis)
                .generatedAt(OffsetDateTime.now())
                .modelVersion(aiService.getActiveModelName())
                .riskScore(null) // resolved by the AI layer post-processing
                .build();

        reportRepository.save(report);
        log.info("FAUST_AI: New intelligence report generated for person {} using model {}.",
                externalId, report.getModelVersion());

        return finalAnalysis;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds the AI prompt from the person's profile data.
     *
     * <p>Uses only data present in the system — the prompt explicitly instructs
     * the model not to invent information not present in the provided context.</p>
     */
    private String buildPrompt(Person person) {
        return String.format("""
                You are the analytical module of Project FAUST — an institutional \
                intelligence platform. Produce a structured security profile \
                strictly based on the provided data. Do not invent or infer \
                information not explicitly present in this prompt.

                SUBJECT:
                Name: %s
                Date of birth: %s
                Biography: %s
                Clearance level: %s

                INSTRUCTIONS:
                - Use ONLY the data provided in this prompt.
                - If data is missing, state "DATA_UNAVAILABLE" for that field.
                - If you detect a career anomaly (e.g. rapid escalation from \
                minor role to senior security position), flag it as [ANOMALY_DETECTED].
                - If you detect financial irregularities or conflicts of interest, \
                flag as [CRITICAL_CONFLICT_OF_INTEREST_DETECTED].

                REPORT STRUCTURE:
                1. IDENT_DATA: Known verified facts about the subject.
                2. RISK_ASSESSMENT: Connection and risk analysis based on known data.
                3. INFLUENCE_TRACE: Scope and reach of influence.
                4. ANOMALY_LOG: Any flagged anomalies from the above analysis.
                """,
                person.getFullName(),
                person.getBirthDate() != null ? person.getBirthDate().toString()
                        : "DATA_UNAVAILABLE",
                person.getBiography() != null ? person.getBiography()
                        : "DATA_UNAVAILABLE",
                person.getClearanceLevel()
        );
    }

    /**
     * Applies intelligence flag substitutions to the raw AI output.
     *
     * <p>Marks high-value terms for downstream processing by the SPA
     * risk visualisation layer. Case-insensitive matching.</p>
     */
    private String applyIntelligenceFlags(String rawAnalysis) {
        if (rawAnalysis == null) return "ERROR: Empty AI response.";
        return rawAnalysis
                .replaceAll("(?i)conflict of interest",
                        "[CRITICAL_CONFLICT_OF_INTEREST_DETECTED]")
                .replaceAll("(?i)nepotism",
                        "[NEPOTISM_FLAG_ALPHA]")
                // FIXED: negative lookahead prevents double-wrapping when the AI
                // already outputs "[ANOMALY_DETECTED]" — without this "anomaly"
                // inside the tag gets matched again → "[[ANOMALY_DETECTED]_DETECTED]"
                .replaceAll("(?i)\\banomaly\\b(?![_A-Z\\]])",
                        "[ANOMALY_DETECTED]")
                .replaceAll("(?i)corruption",
                        "[CORRUPTION_SIGNAL]")
                .replaceAll("(?i)bribery",
                        "[BRIBERY_FLAG]");
    }
}