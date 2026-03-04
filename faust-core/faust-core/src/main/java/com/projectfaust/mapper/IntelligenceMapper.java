package com.projectfaust.mapper;

import com.projectfaust.dto.response.IntelligenceReportResponseDto;
import com.projectfaust.entity.IntelligenceReport;
import org.springframework.stereotype.Component;

/**
 * Component responsible for transforming raw Intelligence Reports into
 * structured Response DTOs for the analytical dashboard.
 */
@Component
public class IntelligenceMapper {

    /**
     * Maps a persistent IntelligenceReport entity to its DTO representation.
     * Extracts subject metadata and flattens AI-generated analysis results.
     *
     * @param report The raw intelligence report from the database.
     * @return A structured DTO containing the analysis and risk scores.
     */
    public IntelligenceReportResponseDto toDTO(IntelligenceReport report) {
        if (report == null) return null;

        return new IntelligenceReportResponseDto(
                report.getExternalId(),
                report.getPerson().getExternalId(),
                report.getPerson().getFullName(),
                report.getAnalysisResult(),
                report.getGeneratedAt(),
                report.getModelVersion(),
                5
        );
    }
}
