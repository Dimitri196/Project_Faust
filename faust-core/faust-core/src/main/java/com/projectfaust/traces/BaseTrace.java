package com.projectfaust.traces;

import com.projectfaust.person.Person;
import com.projectfaust.shared.enums.VerificationStatus;
import com.projectfaust.traces.enums.TraceConfidence;
import com.projectfaust.traces.enums.TraceSourceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Shared base for all trace subtypes in the Project Faust SIGINT/ELINT/FININT layer.
 *
 * <p>All five trace entities ({@link DigitalTrace}, {@link FinancialTrace},
 * {@link CameraTrace}, {@link TelcoTrace}, {@link SurveillanceEvent}) extend this
 * class. Each maps to its own table — there is no joined or single-table
 * inheritance hierarchy in the DB; {@code @MappedSuperclass} keeps the common
 * columns DRY without the overhead of a discriminator column or a join.</p>
 *
 * <p><b>Attribution model:</b> {@code person} is nullable — a trace may be
 * ingested before the subject is identified. {@code vehicleRecord} is set only
 * when the primary observable was a vehicle (ANPR, vehicle spotting).
 * Both may be set simultaneously for events where a person + vehicle are both
 * confirmed (e.g. driver identified at ANPR camera).</p>
 *
 * @author Dimitri / Project Faust
 */
@MappedSuperclass
@Audited
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class BaseTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public-facing UUID for API exposure — never expose internal PK.
     */
    @Column(name = "external_id", nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    // ── Subject attribution ───────────────────────────────────────────────────

    /**
     * The identified person this trace is attributed to.
     * Nullable — may be null when ingested before identity is resolved.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    // ── When ─────────────────────────────────────────────────────────────────

    /**
     * The moment the observed event occurred (or the earliest known bound).
     * Stored in UTC — the UI layer applies timezone conversion.
     */
    @Column(name = "observed_at", nullable = false)
    private LocalDateTime observedAt;

    // ── Source provenance ─────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private TraceSourceType sourceType;

    /**
     * External reference: camera ID, BTS cell ID, terminal ID, operation code,
     * report number — whatever uniquely identifies the source system's record.
     */
    @Column(name = "source_reference", length = 200)
    private String sourceReference;

    // ── Quality ───────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence", nullable = false, length = 20)
    private TraceConfidence confidence = TraceConfidence.UNCONFIRMED;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING_REVIEW;

    /**
     * Quick analyst flag — set to true to surface this trace in alert views
     * without changing verification status.
     */
    @Column(name = "flagged", nullable = false)
    private boolean flagged = false;

    @Column(name = "analytical_note", columnDefinition = "TEXT")
    private String analyticalNote;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (externalId == null) externalId = UUID.randomUUID();
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
