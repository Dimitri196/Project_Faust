package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.OccupationCategory;

import java.util.List;
import java.util.UUID;

/**
 * Recursive Record for the reporting hierarchy (Chain of Command).
 */
public record OccupationTreeResponse(
        UUID publicId,
        String title,
        String code,
        OccupationCategory category,
        boolean isVacant,
        String rank,
        String currentOccupantName,
        List<OccupationTreeResponse> subordinates
) {}
