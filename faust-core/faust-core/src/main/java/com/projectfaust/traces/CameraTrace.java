package com.projectfaust.traces;

import com.projectfaust.vehicle.VehicleRecord;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.Audited;

/**
 * Camera observation trace — a sighting recorded by a fixed or mobile camera
 * with optional biometric or plate recognition.
 *
 * <p>Covers: CCTV facial recognition hits, ANPR (automatic number plate
 * recognition) camera passages, drone footage analysis, and manually
 * reviewed footage where an analyst confirmed identity.</p>
 *
 * <p>Both {@link #person} (from {@link BaseTrace}) and {@link #vehicleRecord}
 * may be set when a driver's identity was confirmed alongside the plate —
 * common for ANPR cameras with integrated facial recognition.</p>
 *
 * <p>Intelligence use-cases:
 * <ul>
 *   <li>Reconstruct a subject's physical route through a city.</li>
 *   <li>Detect co-presence of two subjects at the same camera within a time window.</li>
 *   <li>Track a vehicle across ANPR networks even without driver identification.</li>
 *   <li>Identify pattern-of-life: regular routes, times, locations.</li>
 * </ul>
 * </p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(
        name = "camera_traces",
        indexes = {
                @Index(name = "idx_ctrace_external_id",  columnList = "external_id"),
                @Index(name = "idx_ctrace_person",       columnList = "person_id"),
                @Index(name = "idx_ctrace_vehicle",      columnList = "vehicle_record_id"),
                @Index(name = "idx_ctrace_observed_at",  columnList = "observed_at"),
                @Index(name = "idx_ctrace_camera_id",    columnList = "camera_id"),
                @Index(name = "idx_ctrace_location",     columnList = "location_name")
        }
)
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CameraTrace extends BaseTrace {

    // ── Vehicle attribution ───────────────────────────────────────────────────

    /**
     * Vehicle spotted at this camera — set for ANPR hits.
     * May be set independently of {@link BaseTrace#person} when driver identity
     * is not yet confirmed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_record_id")
    private VehicleRecord vehicleRecord;

    // ── Camera identification ─────────────────────────────────────────────────

    /**
     * Unique identifier of the camera in the operator's CCTV management system.
     * Format is operator-specific (e.g. "CAM-PRG-027", "ANPR-D1-013").
     */
    @Column(name = "camera_id", length = 100)
    private String cameraId;

    /** Operator name (municipality, police, transport authority, private owner). */
    @Column(name = "camera_operator", length = 200)
    private String cameraOperator;

    // ── Location ──────────────────────────────────────────────────────────────

    @Column(name = "location_name", length = 300)
    private String locationName;

    @Column(name = "street_address", length = 500)
    private String streetAddress;

    @Column(name = "city", length = 100)
    private String city;

    /** ISO 3166-1 alpha-2. */
    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    // ── Recognition data ──────────────────────────────────────────────────────

    /**
     * Facial recognition match score (0.0–1.0).
     * Null when recognition was manual or not applicable (ANPR-only event).
     */
    @Column(name = "facial_match_score")
    private Double facialMatchScore;

    /**
     * Raw plate string as read by ANPR OCR — may differ from the normalised
     * {@link VehicleRecord#getLicensePlate()} due to OCR errors.
     */
    @Column(name = "anpr_plate_raw", length = 20)
    private String anprPlateRaw;

    /** ISO 3166-1 alpha-2 country of the read plate (from plate format heuristic). */
    @Column(name = "anpr_plate_country", length = 2)
    private String anprPlateCountry;

    /**
     * Direction of travel through the camera's field of view:
     * INBOUND, OUTBOUND, PASSING, STATIONARY — free-text, operator-defined.
     */
    @Column(name = "direction_of_travel", length = 30)
    private String directionOfTravel;

    /** Frame or timestamp reference within the source footage file. */
    @Column(name = "footage_reference", length = 200)
    private String footageReference;

    /** True if the original footage has been archived and is retrievable. */
    @Builder.Default
    @Column(name = "footage_archived")
    private boolean footageArchived = false;
}
