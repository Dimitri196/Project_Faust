package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.OccupationCategory;

import java.util.UUID;

public record OccupationResponse(
        UUID publicId,
        String title,
        String code,
        OccupationCategory category,
        ClearanceLevel requiredClearanceLevel,
        String institutionName,
        UUID institutionPublicId,
        String supervisorTitle,
        UUID reportsToPublicId,
        boolean isVacant,
        boolean active,
        String rank,
        String description
) {}
