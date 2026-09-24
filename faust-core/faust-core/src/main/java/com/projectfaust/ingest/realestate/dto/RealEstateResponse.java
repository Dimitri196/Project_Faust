package com.projectfaust.ingest.realestate.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO returned by the real estate REST endpoints.
 * Represents a persisted ownership record linked to a FAUST Person or Institution.
 *
 * @author Dimitri / Project Faust
 */
public record RealEstateResponse(
        UUID publicId,
        String sourceSystem,
        String countryCode,
        String propertyType,
        String propertyAddress,
        String cadastralUnit,
        String parcelNumber,
        String ownershipShare,
        BigDecimal estimatedValue,
        String currency,
        boolean encumbered,
        String registryUrl,
        UUID personPublicId,
        String personName,
        UUID institutionPublicId,
        String institutionName,
        OffsetDateTime ingestedAt
) {}