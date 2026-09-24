package com.projectfaust.financial.dto;

import com.projectfaust.shared.enums.PersonAccountRole;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for linking a {@code Person} to a {@code BankAccount}.
 *
 * @param personPublicId   External UUID of the person subject
 * @param accountPublicId  External UUID of the target bank account
 * @param roleType         Person's role with respect to the account
 * @param validFrom        Start of the relationship (nullable = unknown)
 * @param validTo          End of the relationship (nullable = still active)
 * @param active           Whether the relationship is currently active
 * @param verificationStatus Evidentiary status of this record
 */
public record PersonFinancialRelationRequest(

        @NotNull
        UUID personPublicId,

        @NotNull
        UUID accountPublicId,

        @NotNull
        PersonAccountRole roleType,

        LocalDate validFrom,
        LocalDate validTo,
        Boolean   active,
        VerificationStatus verificationStatus
) {}
