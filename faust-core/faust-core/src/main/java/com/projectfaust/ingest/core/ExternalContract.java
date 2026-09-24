package com.projectfaust.ingest.core;

import com.projectfaust.shared.enums.SourceSystem;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a normalised public contract record ingested from
 * an external data source.
 *
 * <p>Designed to be source-agnostic — field names are generic (not tied to
 * any single country's terminology). Source-specific field mapping happens
 * in the {@link ContractSourceMapper} implementation for each
 * {@link SourceSystem}.</p>
 *
 * <p>The {@link #rawJsonData} column preserves the original API response —
 * useful for re-processing if the normalisation logic improves, and for
 * audit/provenance purposes.</p>
 *
 * <p><b>Linking to the intelligence graph:</b> {@link #buyerIco} and
 * {@link #supplierIco} are national company identifiers (IČO for CZ,
 * equivalent registration numbers for other countries). Linking these to
 * {@link com.projectfaust.institution.Institution} records is a separate
 * downstream step — not performed at ingestion time.</p>
 *
 * <p><b>ADDED:</b> {@link #buyerDic} and {@link #supplierDic} — VAT tax IDs
 * (DIČ for CZ, format CZ + IČO). Enables cross-source matching with TED EU
 * contracts which identify parties by VAT ID rather than national registration
 * number, and supports supplier search by tax ID from the intelligence UI.</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(name = "external_contracts", indexes = {
        @Index(name = "idx_ext_contract_external_id",   columnList = "external_id, source_system", unique = true),
        @Index(name = "idx_ext_contract_buyer_ico",     columnList = "buyer_ico"),
        @Index(name = "idx_ext_contract_supplier_ico",  columnList = "supplier_ico"),
        @Index(name = "idx_ext_contract_supplier_dic",  columnList = "supplier_dic"),
        @Index(name = "idx_ext_contract_date",          columnList = "contract_date")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalContract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /**
     * The source system this record was ingested from.
     * Combined with {@link #externalId}, forms a unique constraint.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_system", nullable = false, length = 50)
    private SourceSystem sourceSystem;

    /**
     * The unique identifier of this record within its source system.
     */
    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    // -------------------------------------------------------------------------
    // Buyer (contracting authority)
    // -------------------------------------------------------------------------

    /**
     * National company registration number of the buyer (IČO for CZ).
     */
    @Column(name = "buyer_ico", length = 50)
    private String buyerIco;

    /**
     * VAT tax ID of the buyer (DIČ for CZ, format CZ + IČO).
     * Enables cross-source matching with TED EU contracts which use VAT ID
     * as the primary party identifier.
     */
    @Column(name = "buyer_dic", length = 20)
    private String buyerDic;

    /**
     * Display name of the buyer as provided by the source.
     */
    @Column(name = "buyer_name", length = 500)
    private String buyerName;

    // -------------------------------------------------------------------------
    // Supplier
    // -------------------------------------------------------------------------

    /**
     * National company registration number of the supplier (IČO for CZ).
     * If a contract has multiple suppliers, the primary one is stored here —
     * additional suppliers are preserved in {@link #rawJsonData}.
     */
    @Column(name = "supplier_ico", length = 50)
    private String supplierIco;

    /**
     * VAT tax ID of the supplier (DIČ for CZ, format CZ + IČO).
     * Key for cross-border supplier identification — a foreign supplier
     * won't have a Czech IČO but will have a DIČ if VAT-registered in CZ.
     * Also enables supplier search by tax ID from the intelligence UI.
     */
    @Column(name = "supplier_dic", length = 20)
    private String supplierDic;

    /**
     * Display name of the supplier as provided by the source.
     */
    @Column(name = "supplier_name", length = 500)
    private String supplierName;

    // -------------------------------------------------------------------------
    // Contract details
    // -------------------------------------------------------------------------

    /**
     * Total contract value, normalised to the source's stated currency.
     */
    @Column(name = "amount_total", precision = 18, scale = 2)
    private BigDecimal amountTotal;

    /**
     * Date the contract was confirmed/signed/published.
     */
    @Column(name = "contract_date")
    private LocalDate contractDate;

    /**
     * Free-text description of the contract subject matter.
     */
    @Column(name = "subject_text", columnDefinition = "TEXT")
    private String subjectText;

    /**
     * Full raw JSON response from the source API.
     * Preserved for re-processing and provenance — not exposed via API responses.
     */
    @Column(name = "raw_json_data", columnDefinition = "TEXT")
    private String rawJsonData;

    /**
     * Evidentiary provenance — external ingests default to UNVERIFIED until
     * an analyst confirms the record and any entity links.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "verification_status", nullable = false, length = 50)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    // -------------------------------------------------------------------------
    // Audit metadata
    // -------------------------------------------------------------------------

    @Column(name = "ingested_at", updatable = false)
    private OffsetDateTime ingestedAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        ingestedAt = OffsetDateTime.now();
        updatedAt  = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}