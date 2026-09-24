package com.projectfaust.intelligence;

import com.projectfaust.intelligence.dto.IntelligenceReportResponse;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for transforming {@link IntelligenceReport} entities
 * into {@link IntelligenceReportResponse} DTOs.
 *
 * @author Dimitri / Project Faust
 */
@Component
public class IntelligenceMapper {

    /**
     * Maps an {@link IntelligenceReport} to an {@link IntelligenceReportResponse}.
     *
     * <p>The {@code riskScore} is taken from the entity — it must never be
     * hardcoded in the mapper as it varies per report.</p>
     *
     * @param report the intelligence report entity.
     * @return a response DTO, or {@code null} if input is null.
     */
    public IntelligenceReportResponse toResponse(IntelligenceReport report) {
        if (report == null) return null;

        return new IntelligenceReportResponse(
                report.getExternalId(),
                report.getPerson().getExternalId(),
                report.getPerson().getFullName(),
                report.getAnalysisResult(),
                report.getGeneratedAt(),
                report.getModelVersion(),
                report.getRiskScore()
        );
    }
}