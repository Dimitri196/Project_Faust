package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.OccupationCategory;

import java.util.UUID;

public record OccupationResponse(
        UUID publicId,           // Use UUID for the frontend
        String title,
        String code,
        OccupationCategory category,
        String institutionName,
        UUID institutionPublicId,
        String supervisorTitle,
        UUID reportsToPublicId,  // Link to supervisor via UUID
        boolean isVacant,
        String rank
) {}
