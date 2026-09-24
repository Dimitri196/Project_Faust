package com.projectfaust.insurance;

import com.projectfaust.person.Person;
import com.projectfaust.shared.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents an insurance policy record linked to a natural person within
 * Project Faust.
 *
 * <p>Captures all analytically relevant dimensions of a subject's insurance
 * profile: policy type, insurer, premium size, coverage period, and the
 * beneficiary chain. Records may originate from official insurance association
 * registries, declaratory disclosures, or OSINT ingestion pipelines.</p>
 *
 * <p><b>Deduplication:</b> the {@code (sourceSystem, sourceReferenceId)} pair
 * is unique, enabling idempotent re-ingestion without creating duplicates.</p>
 *
 * <p><b>GDPR note:</b> insurance data constitutes personal financial data.
 * Access is gated by clearance level via the standard FAUST role hierarchy.
 * Processing is lawful under GDPR Article 6(1)(e) — public-interest task.</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(
        name = "insurance_records",
        indexes = {
                @Index(name = "idx_ins_external_id",    columnList = "external_id"),
                @Index(name = "idx_ins_person",         columnList = "person_id"),
                @Index(name = "idx_ins_source_system",  columnList = "source_system"),
                @Index(name = "idx_ins_type",           columnList = "insurance_type"),
                @Index(name = "idx_ins_policy_number",  columnList = "policy_number")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_ins_source_reference",
                        columnNames = {"source_system", "source_reference_id"}
                )
        }
)
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public-facing UUID for API exposure.
     * Prevents sequential enumeration of internal keys.
     */
    @Builder.Default
    @Column(name = "external_id", nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // Owner
    // -------------------------------------------------------------------------

    /**
     * The subject who holds or is the beneficiary of this policy.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    // -------------------------------------------------------------------------
    // Policy identification
    // -------------------------------------------------------------------------

    /**
     * Classification of the insurance product.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "insurance_type", nullable = false, length = 50)
    private InsuranceType insuranceType;

    /**
     * Policy number as assigned by the insurer.
     * May be null for records ingested before the policy number was resolved.
     */
    @Column(name = "policy_number", length = 100)
    private String policyNumber;

    /**
     * Commercial name of the insurance product (e.g. "Flexi Life Plus").
     */
    @Column(name = "product_name", length = 200)
    private String productName;

    /**
     * Legal name of the insurer (insurance company).
     */
    @Column(name = "insurer_name", length = 300)
    private String insurerName;

    /**
     * IČO or equivalent registration number of the insurer.
     * Used for cross-referencing with the institution graph.
     */
    @Column(name = "insurer_registration_number", length = 30)
    private String insurerRegistrationNumber;

    // -------------------------------------------------------------------------
    // Coverage period
    // -------------------------------------------------------------------------

    /** Date from which the policy is in force. */
    @Column(name = "valid_from")
    private LocalDate validFrom;

    /** Date on which the policy expires or was terminated. Null if still active. */
    @Column(name = "valid_to")
    private LocalDate validTo;

    /**
     * True when the policy is currently active (premium paid, not lapsed).
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // -------------------------------------------------------------------------
    // Financial terms
    // -------------------------------------------------------------------------

    /** Gross premium amount per billing cycle (before discounts/taxes). */
    @Column(name = "premium_amount", precision = 18, scale = 2)
    private BigDecimal premiumAmount;

    /** ISO 4217 currency code for the premium and sum assured. */
    @Column(name = "currency", length = 3)
    private String currency;

    /**
     * Frequency at which the premium is billed.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "premium_frequency", length = 30)
    private PremiumFrequency premiumFrequency;

    /**
     * Total coverage / sum assured under the policy.
     * Null for policies where coverage is event-based (e.g. liability).
     */
    @Column(name = "sum_assured", precision = 20, scale = 2)
    private BigDecimal sumAssured;

    // -------------------------------------------------------------------------
    // Beneficiary
    // -------------------------------------------------------------------------

    /**
     * Named beneficiary as stated in the policy.
     * May be a person name, institution name, or "estate".
     */
    @Column(name = "beneficiary_name", length = 300)
    private String beneficiaryName;

    /**
     * National ID or registration number of the beneficiary — used for
     * cross-referencing with the Person or Institution graph.
     */
    @Column(name = "beneficiary_national_id", length = 50)
    private String beneficiaryNationalId;

    // -------------------------------------------------------------------------
    // Source provenance
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "source_system", nullable = false, length = 50)
    private InsuranceSourceSystem sourceSystem;

    /**
     * The policy's unique reference ID in the source system.
     * Used together with {@code sourceSystem} for idempotent ingest deduplication.
     */
    @Column(name = "source_reference_id", length = 200)
    private String sourceReferenceId;

    /** Direct URL to the policy record in the source registry (if publicly accessible). */
    @Column(name = "registry_url", length = 500)
    private String registryUrl;

    /** Raw payload as received from the source — preserved for audit and re-parsing. */
    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData;

    // -------------------------------------------------------------------------
    // Access control & provenance quality
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "verification_status", nullable = false, length = 50)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    /**
     * Confidence metric from 0.0 (speculative) to 1.0 (confirmed).
     * Used by the graph visualisation to weight edge opacity.
     */
    @Builder.Default
    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore = 1.0;

    /**
     * Minimum clearance level required to view this record in the SPA.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "clearance_level", nullable = false, length = 50)
    private ClearanceLevel clearanceLevel = ClearanceLevel.LEVEL_1_PUBLIC;

    /** Analyst notes — context, caveats, secondary leads. */
    @Column(name = "analytical_note", columnDefinition = "TEXT")
    private String analyticalNote;

    // -------------------------------------------------------------------------
    // Audit metadata
    // -------------------------------------------------------------------------

    @Column(name = "ingested_at", updatable = false)
    private OffsetDateTime ingestedAt;

    @PrePersist
    protected void onCreate() {
        ingestedAt = OffsetDateTime.now();
    }
}