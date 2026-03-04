package com.projectfaust.dto.request;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.HierarchicalLevel;
import com.projectfaust.entity.enums.InstitutionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InstitutionRequest(
        @NotBlank String name,
        UUID locationId,
        @NotNull HierarchicalLevel level,
        @NotNull InstitutionType type,
        ClearanceLevel clearanceLevel,
        UUID parentExternalId,
        boolean isStateOwned,
        boolean active,
        String description,
        String logoUrl,
        String websiteUrl
) {}
