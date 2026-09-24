package com.projectfaust.traces;

import com.projectfaust.person.Person;
import com.projectfaust.traces.enums.SurveillanceEventType;
import com.projectfaust.vehicle.VehicleRecord;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.Audited;

/**
 * Operative surveillance event — a physical observation of a subject by a
 * human asset, drone, satellite, or partner agency.
 *
 * <p>Unlike the sensor-driven traces ({@link CameraTrace}, {@link TelcoTrace}),
 * this entity models human-reported or analyst-curated observations where context,
 * meeting participants, and narrative description are as important as the
 * raw positional data.</p>
 *
 * <p>Multiple {@link #meetingParticipants} can be linked when a subject was
 * observed in the company of other known persons — supporting meeting graph
 * reconstruction.</p>
 *
 * <p>Intelligence use-cases:
 * <ul>
 *   <li>Record field reports from operative assets.</li>
 *   <li>Document clandestine meeting participants for network analysis.</li>
 *   <li>Log border crossing observations not captured by official records.</li>
 *   <li>Flag counter-surveillance behaviour for threat assessment.</li>
 * </ul>
 * </p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(
        name = "surveillance_events",
        indexes = {
                @Index(name = "idx_sevt_external_id",  columnList = "external_id"),
                @Index(name = "idx_sevt_person",       columnList = "person_id"),
                @Index(name = "idx_sevt_vehicle",      columnList = "vehicle_record_id"),
                @Index(name = "idx_sevt_observed_at",  columnList = "observed_at"),
                @Index(name = "idx_sevt_event_type",   columnList = "event_type"),
                @Index(name = "idx_sevt_country",      columnList = "country")
        }
)
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SurveillanceEvent extends BaseTrace {

    // ── Vehicle attribution ───────────────────────────────────────────────────

    /**
     * Vehicle observed during this event — set when the operative recorded
     * a vehicle alongside or instead of a person (e.g. vehicle spotting,
     * driver not identified).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_record_id")
    private VehicleRecord vehicleRecord;

    // ── Event classification ──────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private SurveillanceEventType eventType;

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

    // ── Narrative ─────────────────────────────────────────────────────────────

    /**
     * Operative field report — full narrative description of the observed event.
     * May include subject appearance, behaviour, vehicle details, context.
     * Stored as TEXT; handled as classified content.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Codename or identifier of the operative or asset who filed this report.
     * Never store real name here — use a codename or internal asset ID.
     */
    @Column(name = "asset_codename", length = 100)
    private String assetCodename;

    /** Name or code of the operation under which this observation was made. */
    @Column(name = "operation_name", length = 200)
    private String operationName;

    // ── Meeting participants ──────────────────────────────────────────────────

    /**
     * Other known persons observed at this event (e.g. a meeting).
     * Many-to-many via join table — supports meeting graph reconstruction
     * in the network analytics layer.
     *
     * <p>This is a read/write association: adding a person here creates a
     * surveillance_event_participants row. Cascade is limited to PERSIST/MERGE —
     * participants are never deleted when the event is deleted.</p>
     */
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY,
                cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "surveillance_event_participants",
            joinColumns        = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    private java.util.Set<Person> meetingParticipants = new java.util.LinkedHashSet<>();

    // ── Physical evidence ─────────────────────────────────────────────────────

    /** True if photographic evidence was collected during this event. */
    @Builder.Default
    @Column(name = "photo_evidence")
    private boolean photoEvidence = false;

    /** True if audio/video recording was made (covert). */
    @Builder.Default
    @Column(name = "av_recording")
    private boolean avRecording = false;

    /**
     * Reference to the evidence storage system where photo/AV material is held.
     * Never store raw files in the DB — store a reference only.
     */
    @Column(name = "evidence_reference", length = 300)
    private String evidenceReference;
}
