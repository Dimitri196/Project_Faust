package com.projectfaust.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record IntelligenceReportResponseDto(
        UUID reportId,
        UUID personId,
        String personFullName,
        String analysis,
        LocalDateTime generatedAt,
        String aiModel,
        Integer riskLevel
) {}