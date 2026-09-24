package com.projectfaust.institution.dto;

import com.projectfaust.financial.dto.BankAccountRequest;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.HierarchicalLevel;
import com.projectfaust.shared.enums.InstitutionAccountRole;
import com.projectfaust.shared.enums.InstitutionType;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating or updating an {@link com.projectfaust.institution.Institution} node.
 *
 * <p>All mandatory fields are annotated with Bean Validation constraints.
 * The controller must annotate the parameter with {@code @Valid} for
 * constraints to be enforced, including cascaded validation into
 * {@link InstitutionFinancialRequest} via the {@code @Valid} on the list.</p>
 *
 * <p><b>Financial accounts:</b> each entry in {@code financialAccounts} supports
 * either attaching an existing global bank account (by {@code bankAccountExternalId})
 * or creating a new one (via {@code newBankAccountData}). The service layer enforces
 * IBAN deduplication — providing both fields results in the existing account being used.</p>
 *
 * @param name               official name of the institution (required, max 255 chars).
 * @param locationId         public UUID of the GEOINT location node (optional).
 * @param level              administrative tier: NATIONAL, REGIONAL, LOCAL (required).
 * @param type               functional category: EXECUTIVE, INTELLIGENCE, etc. (required).
 * @param clearanceLevel     minimum clearance to view this institution; defaults to PUBLIC.
 * @param parentExternalId   public UUID of the parent institution; null for root nodes.
 * @param stateOwned         whether the institution is state-controlled or owned.
 * @param active             whether the institution is currently operational.
 * @param description        free-text description of the institution's mandate.
 * @param logoUrl            URL of the institution's logo (max 512 chars).
 * @param websiteUrl         URL of the institution's official website (max 512 chars).
 * @param verificationStatus evidentiary provenance state; defaults to PENDING_REVIEW in service.
 * @param financialAccounts  list of financial account relationships to synchronise.
 * @author Dimitri / Project Faust
 */
public record InstitutionRequest(

        @NotBlank(message = "Institution name is required.")
        @Size(max = 255, message = "Institution name must not exceed 255 characters.")
        String name,

        UUID locationId,

        @NotNull(message = "Hierarchical level is required.")
        HierarchicalLevel level,

        @NotNull(message = "Institution type is required.")
        InstitutionType type,

        ClearanceLevel clearanceLevel,
        UUID parentExternalId,

        boolean stateOwned,
        boolean active,

        @Size(max = 2000, message = "Description must not exceed 2000 characters.")
        String description,

        @Size(max = 512, message = "Logo URL must not exceed 512 characters.")
        String logoUrl,

        @Size(max = 512, message = "Website URL must not exceed 512 characters.")
        String websiteUrl,

        VerificationStatus verificationStatus,

        @Valid List<InstitutionFinancialRequest> financialAccounts

) {

    /**
     * Nested request DTO for a single financial account relationship entry.
     *
     * <p>Exactly one of {@code bankAccountExternalId} or {@code newBankAccountData}
     * should be provided per entry. If both are provided, the existing account
     * referenced by {@code bankAccountExternalId} takes precedence. If neither
     * is provided, the entry is silently skipped by the service.</p>
     *
     * @param bankAccountExternalId public UUID of an existing global bank account to attach.
     * @param newBankAccountData    data for creating a new bank account if no existing one.
     * @param roleType              the institution's role with respect to this account (required).
     * @param validFrom             the date from which this relationship is valid.
     * @param validTo               the date until which this relationship is valid (null = open).
     * @param active                whether this financial relationship is currently active.
     */
    public record InstitutionFinancialRequest(
            UUID bankAccountExternalId,
            @Valid BankAccountRequest newBankAccountData,
            @NotNull(message = "Account role type is required.") InstitutionAccountRole roleType,
            LocalDate validFrom,
            LocalDate validTo,
            boolean active
    ) {}
}