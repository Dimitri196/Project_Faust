package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.HierarchicalLevel;
import com.projectfaust.entity.enums.InstitutionType;
import java.util.UUID;

/**
 * Immutable view of an Institution for the Frontend.
 * Uses Java 25 Record for high-performance JSON serialization.
 */
public record InstitutionResponse(
        UUID publicId,
        String name,
        String countryCode,
        HierarchicalLevel level,
        InstitutionType type,
        UUID parentId,
        boolean hasChildren,
        boolean isStateOwned,
        String description

) {}
