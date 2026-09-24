package com.projectfaust.financial.dto;

import com.projectfaust.shared.enums.InstitutionAccountRole;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for linking an {@code Institution} to a {@code BankAccount}.
 *
 * @param institutionPublicId External UUID of the institution
 * @param accountPublicId     External UUID of the target bank account
 * @param roleType            Institution's role on the account
 *                            (OPERATIONAL, DISCRETIONARY_FUND, BUDGETARY)
 * @param validFrom           Start of the relationship
 * @param validTo             End of the relationship (null = still active)
 * @param active              Whether the relationship is currently active
 */
public record InstitutionFinancialRelationRequest(

        @NotNull
        UUID institutionPublicId,

        @NotNull
        UUID accountPublicId,

        @NotNull
        InstitutionAccountRole roleType,

        LocalDate validFrom,
        LocalDate validTo,
        Boolean   active
) {}
