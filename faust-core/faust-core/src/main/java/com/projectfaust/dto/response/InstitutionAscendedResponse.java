package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.HierarchicalLevel;
import com.projectfaust.entity.enums.InstitutionType;

import java.util.UUID;

/**
 * Recursive Record for bottom-up (ascended) representation.
 * From leaf (e.g., Odbor 164) -> Section -> Division -> HQ.
 */
public record InstitutionAscendedResponse(
        UUID publicId,
        String name,
        HierarchicalLevel level,
        InstitutionType type,
        String description,
        boolean isStateOwned,
        InstitutionAscendedResponse parent // The "Ascended" link
) {}