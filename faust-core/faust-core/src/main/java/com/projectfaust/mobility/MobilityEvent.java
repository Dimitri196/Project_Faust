package com.projectfaust.mobility;

import com.projectfaust.person.Person;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.VerificationStatus;
import com.projectfaust.vehicle.VehicleRecord;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Unified mobility intelligence event — covers border crossings, parking
 * and traffic violations, toll passages, ANPR sightings, and checkpoint stops.
 *
 * <p><b>Subject model:</b> unlike other Faust modules this entity does NOT use
 * an XOR discriminator. Both {@code subjectPerson} and {@code subjectVehicle}
 * are independently optional and may both be populated on the same record
 * (e.g. a border crossing links the traveller <em>and</em> their car).</p>
 *
 * <p><b>Field clusters:</b>
 * <ul>
 *   <li><em>Border / port cluster</em> — {@code direction}, {@code originCountryCode},
 *       {@code destinationCountryCode}, {@code crossingPointName},
 *       {@code travelDocumentNumber}, {@code travelDocumentType}</li>
 *   <li><em>Violation cluster</em> — {@code violationCode}, {@code violationDescription},
 *       {@code violationSeverity}, {@code fineAmount}, {@code fineCurrency},
 *       {@code finePaid}, {@code fineDueDate}, {@code pointsDeducted}</li>
 *   <li><em>Surveillance / ANPR cluster</em> — {@code cameraId},
 *       {@code licensePlateRaw}, {@code ocrConfidence}</li>
 * </ul>
 * All three clusters are null-safe; populate only the ones relevant to
 * {@link #eventType}.</p>
 *
 * <p><b>Clearance:</b> defaults to {@code LEVEL_2_INTERNAL}.
 * Border-crossing and SIS records should be upgraded to at least
 * {@code LEVEL_3_CONFIDENTIAL} at ingest time.</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(
        name = "mobility_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_mobility_source",
                        columnNames = {"source_system", "source_reference_id"}
                )
        },
        indexes = {
                @Index(name = "idx_me_external_id",       columnList = "external_id"),
                @Index(name = "idx_me_subject_person",    columnList = "subject_person_id"),
                @Index(name = "idx_me_subject_vehicle",   columnList = "subject_vehicle_id"),
                @Index(name = "idx_me_event_type",        columnList = "event_type"),
                @Index(name = "idx_me_event_timestamp",   columnList = "event_timestamp"),
                @Index(name = "idx_me_location_country",  columnList = "location_country_code"),
                @Index(name = "idx_me_source",            columnList = "source_system, source_reference_id"),
                @Index(name = "idx_me_license_plate_raw", columnList = "license_plate_raw")
        }
)
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MobilityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
                    generator = "mobility_event_seq")
    @SequenceGenerator(name = "mobility_event_seq",
                       sequenceName = "mobility_event_seq",
                       allocationSize = 100)
    private Long id;

    @Column(name = "external_id", nullable = false, updatable = false, unique = true)
    @Builder.Default
    private UUID externalId = UUID.randomUUID();

    // ── Subject links (independently optional — NOT XOR) ─────────────────────

    /** The person involved in this event — may be null if only vehicle is known. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_person_id")
    private Person subjectPerson;

    /** The vehicle involved — may be null if only person is known. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_vehicle_id")
    private VehicleRecord subjectVehicle;

    // ── Core event ────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private MobilityEventType eventType;

    /** When the event was recorded by the source system (UTC preferred). */
    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    // ── Location ──────────────────────────────────────────────────────────────

    @Column(name = "location_name", length = 300)
    private String locationName;

    @Column(name = "location_country_code", length = 2)
    private String locationCountryCode;

    @Column(name = "gps_latitude")
    private Double gpsLatitude;

    @Column(name = "gps_longitude")
    private Double gpsLongitude;

    // ── Border / port cluster ─────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 10)
    @Builder.Default
    private MobilityDirection direction = MobilityDirection.N_A;

    /** ISO-3166-1 alpha-2 — country of origin/departure. */
    @Column(name = "origin_country_code", length = 2)
    private String originCountryCode;

    /** ISO-3166-1 alpha-2 — country of destination/entry. */
    @Column(name = "destination_country_code", length = 2)
    private String destinationCountryCode;

    /** Name of the specific border crossing point or port terminal. */
    @Column(name = "crossing_point_name", length = 200)
    private String crossingPointName;

    @Column(name = "travel_document_number", length = 50)
    private String travelDocumentNumber;

    @Column(name = "travel_document_type", length = 30)
    private String travelDocumentType;

    // ── Violation cluster ─────────────────────────────────────────────────────

    /** Official offence / violation code (national legal code). */
    @Column(name = "violation_code", length = 50)
    private String violationCode;

    @Column(name = "violation_description", columnDefinition = "text")
    private String violationDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_severity", length = 20)
    private MobilityViolationSeverity violationSeverity;

    @Column(name = "fine_amount", precision = 12, scale = 2)
    private BigDecimal fineAmount;

    @Column(name = "fine_currency", length = 3)
    private String fineCurrency;

    @Column(name = "fine_paid")
    private Boolean finePaid;

    @Column(name = "fine_due_date")
    private LocalDate fineDueDate;

    /** Driver's licence points deducted; 0 if no points scheme or not applicable. */
    @Column(name = "points_deducted")
    private Integer pointsDeducted;

    /** Badge number of the issuing officer or enforcer. */
    @Column(name = "officer_badge_number", length = 50)
    private String officerBadgeNumber;

    // ── Surveillance / ANPR cluster ───────────────────────────────────────────

    /** Camera or sensor identifier (ANPR camera ID, CCTV node ID). */
    @Column(name = "camera_id", length = 100)
    private String cameraId;

    /**
     * Raw licence plate read — stored even when vehicle FK not resolved,
     * to allow retroactive linking if VehicleRecord is ingested later.
     */
    @Column(name = "license_plate_raw", length = 20)
    private String licensePlateRaw;

    /** OCR or recognition confidence for ANPR reads (0.0 – 1.0). */
    @Column(name = "ocr_confidence", precision = 5, scale = 4)
    private BigDecimal ocrConfidence;

    // ── Source / provenance ───────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "source_system", nullable = false, length = 40)
    private MobilitySourceSystem sourceSystem;

    @Column(name = "source_reference_id", length = 200)
    private String sourceReferenceId;

    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    // ── Intelligence metadata ─────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 30)
    private VerificationStatus verificationStatus;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "clearance_level", nullable = false, length = 30)
    @Builder.Default
    private ClearanceLevel clearanceLevel = ClearanceLevel.LEVEL_2_INTERNAL;

    @Column(name = "analytical_note", columnDefinition = "text")
    private String analyticalNote;

    @Column(name = "ingested_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ingestedAt = LocalDateTime.now();
}
