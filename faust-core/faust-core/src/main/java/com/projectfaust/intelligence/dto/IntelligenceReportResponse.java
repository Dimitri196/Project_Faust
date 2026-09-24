package com.projectfaust.intelligence.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for an AI-generated intelligence report.
 *
 * @param reportId        public UUID of the report.
 * @param personId        public UUID of the subject person.
 * @param personFullName  display name of the subject.
 * @param analysisResult  full AI-generated analysis text.
 * @param generatedAt     timestamp when the report was generated.
 * @param modelVersion    version of the AI model that produced this report.
 * @param riskScore       risk score assigned by the model (0–10).
 * @author Dimitri / Project Faust
 */
public record IntelligenceReportResponse(
        UUID reportId,
        UUID personId,
        String personFullName,
        String analysisResult,
        OffsetDateTime generatedAt,
        String modelVersion,
        Integer riskScore
) {}