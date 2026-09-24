package com.projectfaust.medical;

import com.projectfaust.institution.Institution;
import com.projectfaust.person.Person;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Medical intelligence record — covers both HUMINT clinical profiles and
 * forensic / field-incident reports in a single unified entity.
 *
 * <p><b>Subject model:</b> XOR discriminator identical to {@code CriminalRecord}:
 * exactly one of {@code subjectPerson} / {@code subjectInstitution} is non-null.
 * {@code subjectNameRaw} serves as a fallback when no FK has been resolved.</p>
 *
 * <p><b>Clearance:</b> defaults to {@code LEVEL_4_SECRET} — the highest
 * module-level default in Project Faust. Individual records may be set lower
 * (e.g. fitness-for-duty summaries shared with HR) but never lower than
 * {@code LEVEL_3_CONFIDENTIAL} by policy.</p>
 *
 * <p><b>Unified type model:</b> {@link MedicalRecordType} drives which field
 * clusters are relevant. Clinical types populate the {@code conditionCategory /
 * diagnosisRaw / diagnosisCodeIcd10 / treatment} cluster; forensic types
 * populate the {@code forensicFindings / toxicologyResults / causeOfDeath}
 * cluster. Both clusters may be populated for complex records.</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(
        name = "medical_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_medical_source",
                        columnNames = {"source_system", "source_reference_id"}
                )
        },
        indexes = {
                @Index(name = "idx_mr_external_id",   columnList = "external_id"),
                @Index(name = "idx_mr_subject_person", columnList = "subject_person_id"),
                @Index(name = "idx_mr_subject_inst",   columnList = "subject_institution_id"),
                @Index(name = "idx_mr_record_type",    columnList = "record_type"),
                @Index(name = "idx_mr_condition_cat",  columnList = "condition_category"),
                @Index(name = "idx_mr_assessment_date",columnList = "assessment_date"),
                @Index(name = "idx_mr_source",         columnList = "source_system, source_reference_id")
        }
)
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
                    generator = "medical_record_seq")
    @SequenceGenerator(name = "medical_record_seq",
                       sequenceName = "medical_record_seq",
                       allocationSize = 50)
    private Long id;

    @Column(name = "external_id", nullable = false, updatable = false, unique = true)
    @Builder.Default
    private UUID externalId = UUID.randomUUID();

    // ── Subject discriminator (XOR: person XOR institution) ───────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    private MedicalSubjectType subjectType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_person_id")
    private Person subjectPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_institution_id")
    private Institution subjectInstitution;

    /** Raw subject name — populated when no FK has been resolved. */
    @Column(name = "subject_name_raw", length = 300)
    private String subjectNameRaw;

    // ── Record type ───────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false, length = 40)
    private MedicalRecordType recordType;

    // ── Dates ─────────────────────────────────────────────────────────────────

    /** Date of examination, assessment, or clinical episode. */
    @Column(name = "assessment_date")
    private LocalDate assessmentDate;

    /** Date the report was issued / signed. */
    @Column(name = "report_date")
    private LocalDate reportDate;

    /** For FITNESS_FOR_DUTY / DISABILITY_ASSESSMENT: valid-until date. */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // ── Clinical fields (CLINICAL_PROFILE, PSYCHIATRIC_ASSESSMENT, etc.) ──────

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_category", length = 30)
    private MedicalConditionCategory conditionCategory;

    /** Free-text diagnosis as supplied by the source. */
    @Column(name = "diagnosis_raw", columnDefinition = "text")
    private String diagnosisRaw;

    /** ICD-10 / ICD-11 code, e.g. {@code F20.0}, {@code T40.1}. */
    @Column(name = "diagnosis_code_icd", length = 20)
    private String diagnosisCodeIcd;

    @Column(name = "treatment_summary", columnDefinition = "text")
    private String treatmentSummary;

    /** Medication or prescription list — free text (may contain JSON). */
    @Column(name = "medications", columnDefinition = "text")
    private String medications;

    /** {@code true} = subject declared fit, {@code false} = unfit, {@code null} = N/A. */
    @Column(name = "fitness_for_duty")
    private Boolean fitnessForDuty;

    /** Statutory disability percentage 0–100; null if not assessed. */
    @Column(name = "disability_percentage")
    private Integer disabilityPercentage;

    // ── Forensic fields (FORENSIC_EXAMINATION, TOXICOLOGY_REPORT, etc.) ───────

    @Column(name = "examining_physician", length = 200)
    private String examiningPhysician;

    @Column(name = "forensic_findings", columnDefinition = "text")
    private String forensicFindings;

    @Column(name = "toxicology_results", columnDefinition = "text")
    private String toxicologyResults;

    /** Official cause of death; populated for CAUSE_OF_DEATH records. */
    @Column(name = "cause_of_death", columnDefinition = "text")
    private String causeOfDeath;

    @Column(name = "injury_description", columnDefinition = "text")
    private String injuryDescription;

    // ── Issuing authority ─────────────────────────────────────────────────────

    @Column(name = "issuing_authority", length = 300)
    private String issuingAuthority;

    @Column(name = "facility_name", length = 300)
    private String facilityName;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    // ── Source / provenance ───────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "source_system", nullable = false, length = 40)
    private MedicalSourceSystem sourceSystem;

    @Column(name = "source_reference_id", length = 200)
    private String sourceReferenceId;

    @Column(name = "document_url", length = 2000)
    private String documentUrl;

    // ── Intelligence metadata ─────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 30)
    private VerificationStatus verificationStatus;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    /**
     * Clearance level for this record.
     * Defaults to {@code LEVEL_4_SECRET} — the highest module-level default
     * in Project Faust.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "clearance_level", nullable = false, length = 30)
    @Builder.Default
    private ClearanceLevel clearanceLevel = ClearanceLevel.LEVEL_4_SECRET;

    @Column(name = "analytical_note", columnDefinition = "text")
    private String analyticalNote;

    @Column(name = "ingested_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ingestedAt = LocalDateTime.now();
}
