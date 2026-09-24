package com.projectfaust.vehicle;

import com.projectfaust.institution.Institution;
import com.projectfaust.person.Person;
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
 * Records a single ownership transfer event in a vehicle's history.
 *
 * <p>One row is created for each change of registered owner — enabling
 * temporal reconstruction of the full ownership chain for any vehicle.
 * The previous owner (before the transfer) and the new owner (after the
 * transfer) are both captured, together with the transfer date and any
 * known financial consideration.</p>
 *
 * <p>The child of a {@link VehicleRecord}; accessed via
 * {@code VehicleRecord#ownershipHistory}.</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(
        name = "vehicle_ownership_history",
        indexes = {
                @Index(name = "idx_voh_vehicle",          columnList = "vehicle_record_id"),
                @Index(name = "idx_voh_from_person",      columnList = "from_person_id"),
                @Index(name = "idx_voh_from_institution", columnList = "from_institution_id"),
                @Index(name = "idx_voh_to_person",        columnList = "to_person_id"),
                @Index(name = "idx_voh_to_institution",   columnList = "to_institution_id"),
                @Index(name = "idx_voh_transfer_date",    columnList = "transfer_date")
        }
)
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleOwnershipHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "external_id", nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // Parent vehicle
    // -------------------------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_record_id", nullable = false)
    private VehicleRecord vehicleRecord;

    // -------------------------------------------------------------------------
    // Transfer metadata
    // -------------------------------------------------------------------------

    /**
     * Date on which the ownership was legally transferred.
     */
    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    /**
     * Previous licence plate at the time of transfer (plates may change
     * when a vehicle crosses borders or is re-registered).
     */
    @Column(name = "license_plate_at_transfer", length = 20)
    private String licensePlateAtTransfer;

    /**
     * Financial consideration paid in the transfer.
     * Null for inheritance, donation, or unknown transactions.
     */
    @Column(name = "transfer_price", precision = 18, scale = 2)
    private BigDecimal transferPrice;

    @Column(name = "currency", length = 3)
    private String currency;

    // -------------------------------------------------------------------------
    // Previous owner (from) — Person XOR Institution
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "from_owner_type", length = 15)
    private VehicleOwnerType fromOwnerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_person_id")
    private Person fromPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_institution_id")
    private Institution fromInstitution;

    /** Registry name of the previous owner — preserved verbatim. */
    @Column(name = "from_name_raw", length = 300)
    private String fromNameRaw;

    @Column(name = "from_national_id", length = 50)
    private String fromNationalId;

    // -------------------------------------------------------------------------
    // New owner (to) — Person XOR Institution
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "to_owner_type", length = 15)
    private VehicleOwnerType toOwnerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_person_id")
    private Person toPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_institution_id")
    private Institution toInstitution;

    /** Registry name of the new owner — preserved verbatim. */
    @Column(name = "to_name_raw", length = 300)
    private String toNameRaw;

    @Column(name = "to_national_id", length = 50)
    private String toNationalId;

    // -------------------------------------------------------------------------
    // Analyst context
    // -------------------------------------------------------------------------

    @Column(name = "analytical_note", columnDefinition = "TEXT")
    private String analyticalNote;

    @Column(name = "ingested_at", updatable = false)
    private OffsetDateTime ingestedAt;

    @PrePersist
    protected void onCreate() {
        ingestedAt = OffsetDateTime.now();
    }
}
