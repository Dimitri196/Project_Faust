package com.projectfaust.institution.dto;

import com.projectfaust.shared.enums.InstitutionalEvolutionType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InstitutionalEvolutionResponse(
        UUID externalId,
        UUID predecessorExternalId,
        String predecessorName,
        UUID successorExternalId,
        String successorName,
        InstitutionalEvolutionType evolutionType,
        LocalDate effectiveDate,
        String legalBasis,
        String description,
        OffsetDateTime createdAt
) {}