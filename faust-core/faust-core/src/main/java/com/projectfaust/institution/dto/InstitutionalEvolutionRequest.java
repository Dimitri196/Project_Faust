package com.projectfaust.institution.dto;

import com.projectfaust.shared.enums.InstitutionalEvolutionType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record InstitutionalEvolutionRequest(
        @NotNull UUID predecessorExternalId,
        UUID successorExternalId, // Může být null u TOTAL_DISSOLUTION
        @NotNull InstitutionalEvolutionType evolutionType,
        @NotNull LocalDate effectiveDate,
        String legalBasis,
        String description
) {}