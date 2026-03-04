package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.OccupationCategory;

import java.util.List;
import java.util.UUID;

public record OccupationTreeResponse(
        UUID publicId,
        String title,
        String code,
        OccupationCategory category,
        ClearanceLevel requiredClearanceLevel,
        boolean isVacant,
        boolean active,
        String rank,
        String currentOccupantName,
        List<OccupationTreeResponse> subordinates
) {}
