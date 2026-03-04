package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.ClearanceLevel;

import java.util.UUID;

public record OccupationAscendedResponse(
        UUID publicId,
        String title,
        String category,
        String rank,
        boolean isVacant,
        ClearanceLevel requiredClearanceLevel,
        String currentOccupantName
) {}
