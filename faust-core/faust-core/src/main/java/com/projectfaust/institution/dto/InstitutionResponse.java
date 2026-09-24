package com.projectfaust.institution.dto;

import com.projectfaust.financial.dto.BankAccountResponse;
import com.projectfaust.location.dto.LocationResponse;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.HierarchicalLevel;
import com.projectfaust.shared.enums.InstitutionAccountRole;
import com.projectfaust.shared.enums.InstitutionType;
import com.projectfaust.shared.enums.VerificationStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing an institutional node in the Project Faust organisation graph.
 *
 * <p>Design principles consistent with the rest of the platform:</p>
 * <ul>
 *   <li><b>No internal IDs</b> — only {@code publicId} (UUID) crosses the API boundary.</li>
 *   <li><b>Flat location reference</b> — {@code locationId} and {@code locationName} for
 *       lightweight display, plus {@code fullLocationPath} for breadcrumb rendering.</li>
 *   <li><b>Intelligence metadata</b> — {@code verificationStatus} enables the frontend to
 *       render provenance badges without a second API call.</li>
 *   <li><b>Timestamps</b> — {@code createdAt} and {@code updatedAt} for data freshness
 *       assessment in the Dossier view.</li>
 * </ul>
 *
 * @param publicId           the public UUID of this institution.
 * @param name               official name of the institution.
 * @param level              administrative tier (NATIONAL, REGIONAL, LOCAL).
 * @param type               functional category (EXECUTIVE, INTELLIGENCE, etc.).
 * @param clearanceLevel     minimum clearance required to view this institution.
 * @param parentId           public UUID of the parent institution, or null for roots.
 * @param hasChildren        whether this institution has at least one active child node.
 * @param stateOwned         whether the institution is state-controlled or owned.
 * @param active             whether the institution is currently operational.
 * @param description        free-text description of the institution's mandate.
 * @param locationId         public UUID of the physical location node.
 * @param locationName       display name of the physical location.
 * @param fullLocationPath   full breadcrumb path from root to the location node.
 * @param logoUrl            URL of the institution's logo.
 * @param websiteUrl         URL of the institution's official website.
 * @param financialAccounts  list of financial account relationships for FININT display.
 * @param verificationStatus evidentiary provenance and trust state of this record.
 * @param createdAt          timestamp when this record was first persisted.
 * @param updatedAt          timestamp of the most recent modification.
 * @author Dimitri / Project Faust
 */
public record InstitutionResponse(
        UUID publicId,
        String name,
        HierarchicalLevel level,
        InstitutionType type,
        ClearanceLevel clearanceLevel,
        UUID parentId,
        boolean hasChildren,
        boolean stateOwned,
        boolean active,
        String description,
        UUID locationId,
        String locationName,
        List<LocationResponse> fullLocationPath,
        String logoUrl,
        String websiteUrl,
        List<InstitutionFinancialDto> financialAccounts,
        VerificationStatus verificationStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * Nested response DTO for a single financial account relationship.
     *
     * <p>Uses {@code relationId} (UUID) rather than an internal Long —
     * the internal primary key must never cross the API boundary.</p>
     *
     * @param relationId  public UUID of the account relationship record.
     * @param bankAccount the bank account details.
     * @param roleType    the institution's role with respect to this account.
     * @param validFrom   the date from which this relationship is valid.
     * @param validTo     the date until which this relationship is valid.
     * @param active      whether this financial relationship is currently active.
     */
    public record InstitutionFinancialDto(
            UUID relationId,
            BankAccountResponse bankAccount,
            InstitutionAccountRole roleType,
            LocalDate validFrom,
            LocalDate validTo,
            boolean active
    ) {}
}