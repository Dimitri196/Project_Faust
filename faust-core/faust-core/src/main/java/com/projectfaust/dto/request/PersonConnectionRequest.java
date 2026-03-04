package com.projectfaust.dto.request;

import com.projectfaust.entity.enums.ConnectionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record PersonConnectionRequest(
        @NotNull UUID sourcePersonId,
        @NotNull UUID targetPersonId,
        @NotNull ConnectionType type,
        @Min(0) @Max(1) Double influenceScore,
        String description,
        LocalDate startDate,
        LocalDate endDate
) {}
