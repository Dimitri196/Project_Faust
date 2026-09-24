package com.projectfaust.criminal;

import com.projectfaust.institution.Institution;
import com.projectfaust.person.Person;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.VerificationStatus;
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
 * Represents a single criminal record entry for a natural person or legal entity.
 *
 * <p><b>Subject model:</b> the {@link CriminalSubjectType} discriminator determines
 * whether {@code subjectPerson} or {@code subjectInstitution} is populated.
 * Corporate criminal liability (zákon č. 418/2011 Sb., Slovak Act 91/2016,
 * Polish Act 197/2002, etc.) makes institution-level records a real requirement
 * across the operating jurisdictions.</p>
 *
 * <p><b>Deduplication:</b> {@code (sourceSystem, sourceReferenceId)} is unique —
 * re-ingesting the same registry entry performs an upsert rather than inserting
 * a duplicate.</p>
 *
 * <p><b>Expiry:</b> criminal records in CEE registries are automatically expunged
 * after a rehabilitation period tied to the sentence length. The {@code expiryDate}
 * field tracks this; the {@code status} field is set to {@link CriminalRecordStatus#EXPUNGED}
 * once the period has passed.</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(
        name = "criminal_records",
        indexes = {
                @Index(name = "idx_cr_external_id",       columnList = "external_id"),
                @Index(name = "idx_cr_subject_person",    columnList = "subject_person_id"),
                @Index(name = "idx_cr_subject_inst",      columnList = "subject_institution_id"),
                @Index(name = "idx_cr_status",            columnList = "status"),
                @Index(name = "idx_cr_offense_category",  columnList = "offense_category"),
                @Index(name = "idx_cr_source_system",     columnList = "source_system"),
                @Index(name = "idx_cr_country_code",      columnList = "country_code")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_cr_source_reference",
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
public class CriminalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "external_id", nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // Subject — Person XOR Institution
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 15)
    private CriminalSubjectType subjectType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_person_id")
    private Person subjectPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_institution_id")
    private Institution subjectInstitution;

    /**
     * Name of the subject as recorded in the source registry.
     * Preserved verbatim even after FK resolution.
     */
    @Column(name = "subject_name_raw", length = 300)
    private String subjectNameRaw;

    /** National ID / IČO as recorded in the source registry. */
    @Column(name = "subject_national_id", length = 50)
    private String subjectNationalId;

    // -------------------------------------------------------------------------
    // Offense description
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "offense_category", nullable = false, length = 30)
    private OffenseCategory offenseCategory;

    /**
     * Free-text description of the offense.
     * May include national penal code reference and short narrative.
     */
    @Column(name = "offense_description", columnDefinition = "TEXT")
    private String offenseDescription;

    /**
     * Legal statute / penal code article (e.g. "§ 140 TrZ", "Art. 148 KK").
     * Stored as free text to accommodate cross-jurisdictional variation.
     */
    @Column(name = "statute", length = 200)
    private String statute;

    /** Date the offense was committed (or first date if a range). */
    @Column(name = "offense_date")
    private LocalDate offenseDate;

    // -------------------------------------------------------------------------
    // Proceedings
    // -------------------------------------------------------------------------

    /** Court case or registry reference number. */
    @Column(name = "case_number", length = 100)
    private String caseNumber;

    @Column(name = "court_name", length = 300)
    private String courtName;

    /**
     * ISO 3166-1 alpha-2 jurisdiction in which the proceeding took place.
     */
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    // -------------------------------------------------------------------------
    // Outcome
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CriminalRecordStatus status;

    /** Date of conviction, acquittal, or case closure. */
    @Column(name = "conviction_date")
    private LocalDate convictionDate;

    // -------------------------------------------------------------------------
    // Sentence
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "sentence_type", length = 30)
    private SentenceType sentenceType;

    /**
     * Length of custodial sentence in months.
     * Null for non-custodial sentences.
     */
    @Column(name = "sentence_length_months")
    private Integer sentenceLengthMonths;

    /** Monetary fine imposed (if applicable). */
    @Column(name = "fine_amount", precision = 18, scale = 2)
    private BigDecimal fineAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    /** End date of probation or parole supervision period. */
    @Column(name = "probation_until")
    private LocalDate probationUntil;

    /** Earliest date on which parole can be granted. */
    @Column(name = "parole_eligible_from")
    private LocalDate paroleEligibleFrom;

    /**
     * Date on which this record is legally expunged / rehabilitated under
     * the applicable national registry rules.
     */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // -------------------------------------------------------------------------
    // Source provenance
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "source_system", nullable = false, length = 50)
    private CriminalSourceSystem sourceSystem;

    @Column(name = "source_reference_id", length = 200)
    private String sourceReferenceId;

    @Column(name = "registry_url", length = 500)
    private String registryUrl;

    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData;

    // -------------------------------------------------------------------------
    // Access control & quality
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "verification_status", nullable = false, length = 50)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @Builder.Default
    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore = 1.0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "clearance_level", nullable = false, length = 50)
    private ClearanceLevel clearanceLevel = ClearanceLevel.LEVEL_3_CONFIDENTIAL;

    @Column(name = "analytical_note", columnDefinition = "TEXT")
    private String analyticalNote;

    // -------------------------------------------------------------------------
    // Audit
    // -------------------------------------------------------------------------

    @Column(name = "ingested_at", updatable = false)
    private OffsetDateTime ingestedAt;

    @PrePersist
    protected void onCreate() {
        ingestedAt = OffsetDateTime.now();
    }
}
