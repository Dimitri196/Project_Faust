package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.OccupationCategory;
import java.util.UUID;

public record OccupationResponse(
        UUID publicId,
        String title,
        String code,
        OccupationCategory category,
        String institutionName,
        UUID institutionPublicId,
        String supervisorTitle,
        UUID reportsToPublicId,
        boolean isVacant,
        String rank,
        String description
) {}
