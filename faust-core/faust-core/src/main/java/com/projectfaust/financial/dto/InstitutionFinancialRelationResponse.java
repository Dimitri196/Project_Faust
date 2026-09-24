package com.projectfaust.financial.dto;

import com.projectfaust.shared.enums.InstitutionAccountRole;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for an {@code InstitutionAccountRelation}.
 *
 * <p>Includes denormalised account identifiers (IBAN, bankName) and
 * institution name for display without a secondary fetch.</p>
 *
 * @param institutionPublicId External UUID of the institution
 * @param institutionName     Display name of the institution (denormalised)
 * @param accountPublicId     External UUID of the bank account
 * @param iban                IBAN (denormalised)
 * @param bankName            Bank name (denormalised)
 * @param roleType            Institution's account role
 * @param validFrom           Start of the relationship
 * @param validTo             End of the relationship (null = still active)
 * @param active              Whether currently active
 */
public record InstitutionFinancialRelationResponse(
        UUID                   institutionPublicId,
        String                 institutionName,
        UUID                   accountPublicId,
        String                 iban,
        String                 bankName,
        InstitutionAccountRole roleType,
        LocalDate              validFrom,
        LocalDate              validTo,
        boolean                active
) {}
