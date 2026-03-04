package com.projectfaust.dto.request;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.OccupationCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OccupationRequest(
        @NotBlank String title,
        @NotBlank String code,
        @NotNull OccupationCategory category,
        @NotNull ClearanceLevel requiredSecurityLevel,
        @NotNull UUID institutionPublicId,
        UUID reportsToPublicId,
        boolean isVacant,
        boolean active,
        String rank,
        String description
) {}
