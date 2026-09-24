package com.projectfaust.institution.dto;

import com.projectfaust.shared.enums.IdentifierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for registering a new identifier against an institution.
 *
 * <p>Identifiers are the bridge to external data sources — a
 * {@link IdentifierType#NATIONAL_REGISTRATION} with countryCode "CZ"
 * enables automatic linkage to Hlidač Státu procurement contracts;
 * a {@link IdentifierType#LEI} enables linkage to financial disclosures
 * and sanctions lists.</p>
 *
 * @param type        the registration scheme.
 * @param value       the identifier value within that scheme.
 * @param countryCode ISO 3166-1 alpha-2 country code (2 uppercase letters).
 *                    Required for national schemes (NATIONAL_REGISTRATION, VAT).
 *                    Omit or leave null for global schemes (LEI, DUNS, EUID).
 * @param note        optional note; strongly recommended for CUSTOM type
 *                    to document the exact scheme and source.
 * @author Dimitri / Project Faust
 */
public record InstitutionIdentifierRequest(

        @NotNull(message = "Identifier type is required.")
        IdentifierType type,

        @NotBlank(message = "Identifier value is required.")
        @Size(max = 100, message = "Identifier value must not exceed 100 characters.")
        String value,

        /**
         * ISO 3166-1 alpha-2 country code — 2 uppercase letters.
         * Null is valid for global schemes (LEI, DUNS, EUID, GLEIF_RELATIONSHIP).
         */
        @Pattern(regexp = "^[A-Z]{2}$",
                message = "Country code must be exactly 2 uppercase letters (ISO 3166-1 alpha-2).")
        String countryCode,

        @Size(max = 500, message = "Note must not exceed 500 characters.")
        String note

) {}