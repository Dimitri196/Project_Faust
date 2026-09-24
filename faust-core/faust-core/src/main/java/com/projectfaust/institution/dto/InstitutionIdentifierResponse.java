package com.projectfaust.institution.dto;

import com.projectfaust.shared.enums.IdentifierType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for a single {@link com.projectfaust.institution.InstitutionIdentifier}.
 *
 * @param id          UUID of this identifier record.
 * @param type        the registration scheme (NATIONAL_REGISTRATION, LEI, DUNS etc.)
 * @param value       the actual identifier value.
 * @param countryCode ISO 3166-1 alpha-2 country code; null for global schemes.
 * @param note        optional free-text note (required for CUSTOM type).
 * @param active      whether this identifier is currently valid.
 * @param createdAt   when this record was created — useful for provenance.
 * @author Dimitri / Project Faust
 */
public record InstitutionIdentifierResponse(
        UUID id,
        IdentifierType type,
        String value,
        String countryCode,
        String note,
        boolean active,
        OffsetDateTime createdAt
) {}
