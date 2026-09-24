package com.projectfaust.vehicle;

import com.projectfaust.institution.Institution;
import com.projectfaust.insurance.InsuranceRecord;
import com.projectfaust.mobility.MobilityEvent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a registered vehicle (or watercraft / aircraft) within Project Faust.
 *
 * <p><b>Owner vs operator:</b> a vehicle may be legally owned by one entity and
 * operated (i.e. registered driver / fleet manager) by another — common in
 * corporate fleets, leasing arrangements, and intelligence-relevant shell
 * structures. Both roles are modelled independently, each as a {@code Person}
 * XOR {@code Institution} pair guarded by a {@link VehicleOwnerType}
 * discriminator.</p>
 *
 * <p><b>Ownership history:</b> previous owners are tracked via the
 * {@link VehicleOwnershipHistory} child collection — one row per transfer,
 * allowing temporal reconstruction of the full ownership chain.</p>
 *
 * <p><b>Deduplication:</b> the {@code (sourceSystem, sourceReferenceId)} pair
 * is unique, enabling idempotent re-ingestion without creating duplicates.</p>
 *
 * <p><b>GDPR note:</b> vehicle registration data comes from official public
 * registries and is lawfully processed under GDPR Article 6(1)(e) — public
 * interest. Access is gated by clearance level via the FAUST role hierarchy.</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(
        name = "vehicle_records",
        indexes = {
                @Index(name = "idx_veh_external_id",      columnList = "external_id"),
                @Index(name = "idx_veh_vin",              columnList = "vin"),
                @Index(name = "idx_veh_plate",            columnList = "license_plate"),
                @Index(name = "idx_veh_owner_person",     columnList = "owner_person_id"),
                @Index(name = "idx_veh_owner_inst",       columnList = "owner_institution_id"),
                @Index(name = "idx_veh_operator_person",  columnList = "operator_person_id"),
                @Index(name = "idx_veh_operator_inst",    columnList = "operator_institution_id"),
                @Index(name = "idx_veh_source_system",    columnList = "source_system"),
                @Index(name = "idx_veh_category",         columnList = "category")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_veh_source_reference",
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
public class VehicleRecord {

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
    // Registration identifiers
    // -------------------------------------------------------------------------

    /**
     * Vehicle Identification Number (VIN) — globally unique 17-character code.
     * Null for pre-VIN vehicles and some watercraft / aircraft.
     */
    @Column(name = "vin", length = 17)
    private String vin;

    /**
     * Current licence plate / registration mark.
     * Stored upper-case, spaces removed (e.g. "1AB2345").
     */
    @Column(name = "license_plate", length = 20)
    private String licensePlate;

    /**
     * ISO 3166-1 alpha-2 country code of the issuing registry.
     */
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    /**
     * Registration number in the source cadaster / vehicle registry.
     * Used together with {@code sourceSystem} for deduplication.
     */
    @Column(name = "source_reference_id", length = 200)
    private String sourceReferenceId;

    // -------------------------------------------------------------------------
    // Vehicle description
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private VehicleCategory category;

    @Column(name = "make", length = 100)
    private String make;

    @Column(name = "model", length = 100)
    private String model;

    /** Commercial variant / trim level (e.g. "AMG Line", "4Motion"). */
    @Column(name = "variant", length = 100)
    private String variant;

    @Column(name = "model_year")
    private Integer modelYear;

    @Column(name = "color", length = 50)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", length = 30)
    private FuelType fuelType;

    /** Engine displacement in cm³. */
    @Column(name = "engine_displacement_cc")
    private Integer engineDisplacementCc;

    /** Maximum power output in kW. */
    @Column(name = "power_kw")
    private Integer powerKw;

    /** Number of seats (excluding driver). */
    @Column(name = "seat_count")
    private Integer seatCount;

    /** Gross vehicle weight in kg. */
    @Column(name = "gross_weight_kg")
    private Integer grossWeightKg;

    // -------------------------------------------------------------------------
    // Owner — Person XOR Institution
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", length = 15)
    private VehicleOwnerType ownerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_person_id")
    private Person ownerPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_institution_id")
    private Institution ownerInstitution;

    /**
     * Owner name as it appears in the registry — preserved even after FK
     * resolution, since registry spelling may differ from FAUST canonical name.
     */
    @Column(name = "owner_name_raw", length = 300)
    private String ownerNameRaw;

    /** Owner's national ID / IČO from the source registry. */
    @Column(name = "owner_national_id", length = 50)
    private String ownerNationalId;

    // -------------------------------------------------------------------------
    // Operator — Person XOR Institution (may differ from owner)
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "operator_type", length = 15)
    private VehicleOwnerType operatorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_person_id")
    private Person operatorPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_institution_id")
    private Institution operatorInstitution;

    @Column(name = "operator_name_raw", length = 300)
    private String operatorNameRaw;

    @Column(name = "operator_national_id", length = 50)
    private String operatorNationalId;

    // -------------------------------------------------------------------------
    // Registration validity
    // -------------------------------------------------------------------------

    /** Date the vehicle was first registered (anywhere). */
    @Column(name = "first_registered_on")
    private LocalDate firstRegisteredOn;

    /** Date the current registration became valid. */
    @Column(name = "registered_since")
    private LocalDate registeredSince;

    /** Date the registration was cancelled, exported, or scrapped. Null if active. */
    @Column(name = "deregistered_on")
    private LocalDate deregisteredOn;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // -------------------------------------------------------------------------
    // Technical inspection (STK / TÜV)
    // -------------------------------------------------------------------------

    /** Date of the last passed technical inspection. */
    @Column(name = "last_inspection_date")
    private LocalDate lastInspectionDate;

    /** Expiry date of the current technical inspection certificate. */
    @Column(name = "inspection_valid_until")
    private LocalDate inspectionValidUntil;

    // -------------------------------------------------------------------------
    // Financial profile
    // -------------------------------------------------------------------------

    /** Purchase / acquisition value at time of registration. */
    @Column(name = "acquisition_value", precision = 18, scale = 2)
    private BigDecimal acquisitionValue;

    /** ISO 4217 currency code for {@code acquisitionValue}. */
    @Column(name = "currency", length = 3)
    private String currency;

    /**
     * True if the vehicle was acquired under a leasing or credit arrangement.
     * Key signal for financial structure analysis.
     */
    @Builder.Default
    @Column(name = "is_leased")
    private boolean leased = false;

    /**
     * Name of the leasing company / creditor (if {@code leased} is true).
     */
    @Column(name = "lessor_name", length = 300)
    private String lessorName;

    /**
     * True if a lien, pledge, or encumbrance is registered against the vehicle.
     */
    @Builder.Default
    @Column(name = "encumbered")
    private boolean encumbered = false;

    // -------------------------------------------------------------------------
    // Insurance cross-reference
    // -------------------------------------------------------------------------

    /**
     * Optional direct link to the compulsory third-party liability insurance
     * record ({@link InsuranceRecord}) for this vehicle.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_record_id")
    private InsuranceRecord insuranceRecord;

    // -------------------------------------------------------------------------
    // Ownership history
    // -------------------------------------------------------------------------

    /**
     * Ordered list of previous ownership transfers for this vehicle.
     * Populated by the ingest pipeline; never auto-deleted.
     */
    @Builder.Default
    @OneToMany(mappedBy = "vehicleRecord", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("transferDate DESC")
    private List<VehicleOwnershipHistory> ownershipHistory = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Source provenance
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "source_system", nullable = false, length = 50)
    private VehicleSourceSystem sourceSystem;

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
    private ClearanceLevel clearanceLevel = ClearanceLevel.LEVEL_1_PUBLIC;

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


       /**
     +     * All mobility events in which this vehicle is a subject.
     +     * Covers toll passages, ANPR sightings, parking violations, border crossings.
     +     *
     +     * <p>Note: a {@link MobilityEvent} may simultaneously reference both this
     +     * vehicle and a {@link com.projectfaust.person.Person} — the link
     +     * is not exclusive (XOR is not enforced here).</p>
     +     */
                @OneToMany(
                        mappedBy = "subjectVehicle",
                        cascade = {CascadeType.PERSIST, CascadeType.MERGE},
                        fetch = FetchType.LAZY
    )
            @Builder.Default
    private List<MobilityEvent> mobilityEvents = new ArrayList<>();

}
