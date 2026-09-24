package com.projectfaust.person.document;

import com.projectfaust.person.Person;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.DocumentSourceSystem;
import com.projectfaust.shared.enums.DocumentType;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Intelligence record of an identity document held by a person.
 *
 * <p>Supports three ingestion paths:
 * <ol>
 *   <li><b>Manual</b> — analyst enters data directly via UI/API.</li>
 *   <li><b>Scan/OCR</b> — document image uploaded, OCR extracts fields,
 *       MRZ parser extracts machine-readable zone.</li>
 *   <li><b>Registry pull</b> — automated fetch from ARES, Katastr,
 *       GLEIF, sanctions lists etc. Raw response stored in
 *       {@link #rawSourceData} for re-processing.</li>
 * </ol>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(name = "person_documents", indexes = {
        @Index(name = "idx_doc_person_id",         columnList = "person_id"),
        @Index(name = "idx_doc_type",              columnList = "document_type"),
        @Index(name = "idx_doc_number",            columnList = "document_number_normalized"),
        @Index(name = "idx_doc_issuing_state",     columnList = "issuing_state"),
        @Index(name = "idx_doc_source_system",     columnList = "source_system")
})
@Audited
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PersonDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    // ── Document classification ───────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    // ── Document numbers ──────────────────────────────────────────────────────

    @Column(name = "document_number_raw", nullable = false, length = 100)
    private String documentNumberRaw;

    @Column(name = "document_number_normalized", nullable = false, length = 100)
    private String documentNumberNormalized;

    // ── Issuing authority ─────────────────────────────────────────────────────

    /** ISO 3166-1 alpha-2 code of the issuing state. */
    @Column(name = "issuing_state", length = 10)
    private String issuingState;

    @Column(name = "issuing_authority", length = 300)
    private String issuingAuthority;

    // ── Validity ──────────────────────────────────────────────────────────────

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** False = suspected forgery or alias document. */
    @Builder.Default
    @Column(name = "is_authentic", nullable = false)
    private boolean authentic = true;

    // ── MRZ — Machine Readable Zone ───────────────────────────────────────────

    /**
     * MRZ string parsed from the bottom of travel documents.
     * Format: ICAO 9303 (two or three lines, 30 or 44 chars each).
     * High value for border crossing cross-reference.
     */
    @Column(name = "mrz_line", length = 200)
    private String mrzLine;

    // ── Source ingestion ──────────────────────────────────────────────────────

    /**
     * Which system provided this document record.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "source_system", length = 50)
    private DocumentSourceSystem sourceSystem = DocumentSourceSystem.MANUAL;

    /**
     * URL to the original document scan stored in FAUST file storage.
     * Supports PDF, JPEG, PNG. Never exposed publicly.
     */
    @Column(name = "source_image_url", length = 500)
    private String sourceImageUrl;

    /**
     * Raw JSON/XML response from the source registry (ARES, GLEIF, etc.).
     * Preserved for re-processing when normalization logic improves.
     */
    @Column(name = "raw_source_data", columnDefinition = "TEXT")
    private String rawSourceData;

    /**
     * External reference ID within the source system
     * (e.g. ARES entity ID, Katastr parcel ID).
     */
    @Column(name = "source_reference_id", length = 200)
    private String sourceReferenceId;

    // ── Intelligence metadata ─────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "verification_status", nullable = false, length = 50)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @Builder.Default
    @Column(name = "confidence_score")
    private Double confidenceScore = 1.0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "clearance_level", nullable = false, length = 50)
    private ClearanceLevel clearanceLevel = ClearanceLevel.LEVEL_1_PUBLIC;

    @Column(columnDefinition = "TEXT")
    private String analyticalNote;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        normalizeDocumentNumber();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
        normalizeDocumentNumber();
    }

    private void normalizeDocumentNumber() {
        if (documentNumberRaw != null) {
            documentNumberNormalized = documentNumberRaw
                    .toUpperCase()
                    .replaceAll("[\\s\\-]", "");
        }
    }
}