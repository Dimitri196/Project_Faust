package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.HierarchicalLevel;
import com.projectfaust.entity.enums.InstitutionType;

import java.util.List;
import java.util.UUID;

public record InstitutionResponse(
        UUID publicId,
        String name,
        String countryCode,
        HierarchicalLevel level,
        InstitutionType type,
        UUID parentId,
        boolean hasChildren,
        boolean isStateOwned,
        String description,
        UUID locationId,
        String locationName,
        List<LocationResponse> fullLocationPath,
        String logoUrl,
        String websiteUrl
) {}
