package com.projectfaust.dto.request;

import com.projectfaust.entity.enums.HierarchicalLevel;
import com.projectfaust.entity.enums.InstitutionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record InstitutionRequest(
        @NotBlank(message = "Institution name is required")
        @Size(max = 255)
        String name,
        @NotBlank(message = "Country code is required")
        @Size(min = 2, max = 2, message = "Country code must be ISO 3166-1 alpha-2 (e.g., CZ)")
        String countryCode,
        @NotNull(message = "Hierarchical level is required")
        HierarchicalLevel level,
        @NotNull(message = "Institution type is required")
        InstitutionType type,
        UUID parentExternalId,
        boolean isStateOwned,
        String description,
        String logoUrl,
        String websiteUrl
) {}
