package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.HierarchicalLevel;
import com.projectfaust.entity.enums.InstitutionType;

import java.util.UUID;

public record InstitutionAscendedResponse(
        UUID publicId,
        String name,
        HierarchicalLevel level,
        InstitutionType type,
        String description,
        boolean isStateOwned,
        InstitutionAscendedResponse parent
) {}
