package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.HierarchicalLevel;
import com.projectfaust.entity.enums.InstitutionType;

import java.util.List;
import java.util.UUID;

public record InstitutionTreeResponse(
        UUID publicId,
        String name,
        HierarchicalLevel level,
        InstitutionType type,
        ClearanceLevel clearanceLevel,
        String description,
        boolean isStateOwned,
        boolean active,
        boolean hasChildren,
        InstitutionTreeResponse parent,
        List<InstitutionTreeResponse> children,
        String logoUrl
) {}
