package com.projectfaust.court;

import com.projectfaust.criminal.CriminalRecord;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single court proceeding within Project Faust.
 *
 * <p><b>Parties:</b> each proceeding has one or more {@link CourtRecordParty}
 * child rows, each linking a {@link com.projectfaust.person.Person} or
 * {@link com.projectfaust.institution.Institution} to the proceeding via a
 * {@link PartyRole}. This enables querying "all proceedings in which person X
 * appeared as defendant" or "all cases where institutions A and B were co-parties".</p>
 *
 * <p><b>Criminal cross-reference:</b> when a court proceeding results in a
 * criminal conviction, {@code linkedCriminalRecord} links directly to the
 * corresponding {@link CriminalRecord} entry, avoiding data duplication while
 * preserving the court-level detail.</p>
 *
 * <p><b>Deduplication:</b> {@code (sourceSystem, sourceReferenceId)} is unique.</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(
        name = "court_records",
        indexes = {
                @Index(name = "idx_court_external_id",    columnList = "external_id"),
                @Index(name = "idx_court_case_number",    columnList = "case_number"),
                @Index(name = "idx_court_proceeding_type",columnList = "proceeding_type"),
                @Index(name = "idx_court_outcome",        columnList = "outcome"),
                @Index(name = "idx_court_country_code",   columnList = "country_code"),
                @Index(name = "idx_court_source_system",  columnList = "source_system"),
                @Index(name = "idx_court_judgment_date",  columnList = "judgment_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_court_source_reference",
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
public class CourtRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "external_id", nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // Proceeding metadata
    // -------------------------------------------------------------------------

    /**
     * Official case / docket number as assigned by the court.
     * Format varies by jurisdiction (e.g. "1 T 45/2023", "IV. ÚS 1234/22").
     */
    @Column(name = "case_number", length = 100)
    private String caseNumber;

    @Column(name = "court_name", nullable = false, length = 300)
    private String courtName;

    @Enumerated(EnumType.STRING)
    @Column(name = "court_level", length = 30)
    private CourtLevel courtLevel;

    /** ISO 3166-1 alpha-2 jurisdiction of the court. */
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "proceeding_type", nullable = false, length = 30)
    private ProceedingType proceedingType;

    // -------------------------------------------------------------------------
    // Subject matter
    // -------------------------------------------------------------------------

    /**
     * Short description of what the proceeding is about.
     * E.g. "Tax fraud — EUR 4.2M evasion scheme".
     */
    @Column(name = "subject_matter", columnDefinition = "TEXT")
    private String subjectMatter;

    /**
     * Applicable legal statute(s) / directive as free text.
     */
    @Column(name = "statute", length = 500)
    private String statute;

    // -------------------------------------------------------------------------
    // Timeline
    // -------------------------------------------------------------------------

    /** Date the case was filed / registered with the court. */
    @Column(name = "filed_date")
    private LocalDate filedDate;

    /** Date of the first substantive hearing. */
    @Column(name = "first_hearing_date")
    private LocalDate firstHearingDate;

    /** Date the judgment / decision was handed down. */
    @Column(name = "judgment_date")
    private LocalDate judgmentDate;

    /** Deadline by which an appeal must be filed. */
    @Column(name = "appeal_deadline")
    private LocalDate appealDeadline;

    /** Date the proceeding was formally closed / archived. */
    @Column(name = "closed_date")
    private LocalDate closedDate;

    // -------------------------------------------------------------------------
    // Outcome
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "outcome", nullable = false, length = 40)
    private CourtOutcome outcome = CourtOutcome.ONGOING;

    /** True if an appeal was filed against the judgment. */
    @Builder.Default
    @Column(name = "appealed")
    private boolean appealed = false;

    /** Free-text summary of the judgment (e.g. sentence, damages awarded). */
    @Column(name = "judgment_summary", columnDefinition = "TEXT")
    private String judgmentSummary;

    // -------------------------------------------------------------------------
    // Parties (child collection — cross-references to Person / Institution)
    // -------------------------------------------------------------------------

    /**
     * All parties involved in this proceeding.
     * Ordered by role, then by name for display purposes.
     */
    @Builder.Default
    @OneToMany(mappedBy = "courtRecord", fetch = FetchType.LAZY,
               cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
               orphanRemoval = true)
    @OrderBy("partyRole ASC")
    private List<CourtRecordParty> parties = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Criminal cross-reference
    // -------------------------------------------------------------------------

    /**
     * Optional link to a {@link CriminalRecord} produced by this proceeding.
     * Populated after a conviction is registered; null for acquittals, civil
     * matters, or pending proceedings.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_criminal_record_id")
    private CriminalRecord linkedCriminalRecord;

    // -------------------------------------------------------------------------
    // Source provenance
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "source_system", nullable = false, length = 50)
    private CourtSourceSystem sourceSystem;

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
    private ClearanceLevel clearanceLevel = ClearanceLevel.LEVEL_2_INTERNAL;

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
