package com.projectfaust.ingest.dto;

import com.projectfaust.shared.enums.SourceSystem;
import com.projectfaust.shared.enums.VerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for a normalised public contract record.
 *
 * <p>Exposes the analytical fields needed for the institution dossier —
 * date, subject, amount, buyer/supplier names, which role the queried
 * institution played (BUYER/SUPPLIER/BOTH), and ingestion provenance.
 * The raw JSON payload is intentionally excluded.</p>
 *
 * <p>Used by {@code GET /api/v1/contracts/institution/{publicId}} and
 * any future contract search endpoints.</p>
 *
 * @param id                 internal UUID of this contract record.
 * @param sourceSystem       which external system this was ingested from.
 * @param externalId         the record's ID within the source system.
 * @param buyerIco           registration number of the contracting authority.
 * @param buyerName          display name of the contracting authority.
 * @param supplierIco        registration number of the supplier.
 * @param supplierName       display name of the supplier.
 * @param amountTotal        total contract value in the source currency.
 * @param contractDate       date the contract was confirmed/published.
 * @param subjectText        free-text description of the contract subject.
 * @param verificationStatus evidentiary provenance state of this record.
 * @param ingestedAt         when this record was pulled into Faust — useful
 *                           for analysts assessing data freshness.
 * @param role               whether the queried institution is the BUYER,
 *                           SUPPLIER, or BOTH in this contract — computed
 *                           by the service layer so the frontend doesn't
 *                           need to re-derive it by comparing ICO values.
 * @author Dimitri / Project Faust
 */
public record ExternalContractResponse(
        UUID id,
        SourceSystem sourceSystem,
        String externalId,
        String buyerIco,
        String buyerName,
        String supplierIco,
        String supplierName,
        BigDecimal amountTotal,
        LocalDate contractDate,
        String subjectText,
        VerificationStatus verificationStatus,
        OffsetDateTime ingestedAt,
        ContractRole role
) {
    /**
     * The role the queried institution played in this contract.
     * Computed server-side by matching the institution's registration
     * number against buyerIco and supplierIco.
     */
    public enum ContractRole {
        BUYER, SUPPLIER, BOTH
    }
}