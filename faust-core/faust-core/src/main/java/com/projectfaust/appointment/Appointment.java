package com.projectfaust.appointment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.projectfaust.occupation.Occupation;
import com.projectfaust.person.Person;
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
 * Entity representing the formal assignment of a {@link Person} to an {@link Occupation}.
 *
 * <p>An {@code Appointment} is the temporal bridge between a person and a positional slot.
 * It tracks not only who holds a position and when, but also the financial terms,
 * acting status, and associated privileges — making it a critical HUMINT record.</p>
 *
 * <p><b>Key intelligence fields:</b></p>
 * <ul>
 *   <li>{@link #acting} — flags temporary office holders, signalling power transitions
 *       and potential vulnerabilities in the command structure.</li>
 *   <li>{@link #exOffoAccess} — tracks which persons gain automatic system or data
 *       access by virtue of holding the position.</li>
 *   <li>{@link #benefitDetails} — JSONB-stored perks including diplomatic passport,
 *       security detail, and official residence. High-value FININT and HUMINT signals.</li>
 * </ul>
 *
 * <p>{@link #isActive()} derives current status from dates rather than storing a
 * boolean — this ensures the status is never stale regardless of when it is queried.</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appt_external_id",  columnList = "externalId"),
        @Index(name = "idx_appt_person_id",    columnList = "person_id"),
        @Index(name = "idx_appt_occupation_id", columnList = "occupation_id"),
        @Index(name = "idx_appt_start_date",   columnList = "startDate")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "appointments_seq")
    @SequenceGenerator(
            name = "appointments_seq",
            sequenceName = "appointments_id_seq",
            allocationSize = 1
    )
    private Long id;

    /**
     * Public-facing UUID for API and frontend components.
     * Generated on creation, immutable thereafter.
     */
    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    /**
     * The person assigned to this position.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /**
     * The positional slot this assignment refers to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "occupation_id", nullable = false)
    private Occupation occupation;

    /**
     * The date on which the assignment began (inclusive).
     */
    @Column(nullable = false)
    private LocalDate startDate;

    /**
     * The date on which the assignment ended (inclusive).
     * {@code null} indicates the assignment is still ongoing.
     */
    private LocalDate endDate;

    /**
     * Base monthly compensation before taxes and bonuses.
     * Stored as {@link BigDecimal} to avoid floating-point rounding errors.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal monthlySalary;

    /**
     * Fixed monthly allowances (e.g. representation, per diem, meal allowances).
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal monthlyLumpSumAllowance;

    /**
     * ISO 4217 currency code for salary and allowance values.
     */
    @Builder.Default
    @Column(length = 3)
    private String currency = "CZK";

    /**
     * Indicates whether the person is temporarily holding the office in an acting capacity
     * (Czech: Pověřen řízením). Acting appointments signal power transitions and
     * may indicate structural instability in the command chain.
     */
    @Builder.Default
    @Column(name = "is_acting", nullable = false)
    private boolean acting = false;

    /**
     * Indicates whether the position grants automatic access to data or resources
     * by virtue of the office held (Ex Officio access).
     * Critical for access control intelligence tracking.
     */
    @Builder.Default
    @Column(name = "is_ex_offo_access", nullable = false)
    private boolean exOffoAccess = false;

    /**
     * Evidentiary provenance and verification state of this record.
     * Appointments ingested via Kafka or scrapers default to {@code PENDING_REVIEW}
     * until promoted by a cleared analyst.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING_REVIEW;

    /**
     * Free-text analyst note providing intelligence context for this appointment.
     */
    @Column(length = 2000)
    private String appointmentNote;

    /**
     * Advanced logistical benefit data stored as JSONB for high-granularity tracking.
     * Provides high-value FININT and HUMINT signals — diplomatic passport,
     * security detail, and official residence are key indicators of power and exposure.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "benefit_details", columnDefinition = "jsonb")
    private BenefitDetails benefitDetails;

    // --- AUDIT METADATA ---

    @Column(updatable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // --- DOMAIN METHODS ---

    /**
     * Determines whether this appointment is currently active based on its date range.
     *
     * <p>An appointment is active when:</p>
     * <ul>
     *   <li>The {@code startDate} is today or in the past ({@code !startDate.isAfter(today)}).</li>
     *   <li>The {@code endDate} is null (ongoing) or today or in the future
     *       ({@code endDate == null || !endDate.isBefore(today)}).</li>
     * </ul>
     *
     * <p>Uses {@code !isAfter} and {@code !isBefore} rather than {@code isBefore} and
     * {@code isAfter} to correctly include same-day appointments — a position starting
     * today is active today.</p>
     *
     * @return {@code true} if the appointment is currently active.
     */
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return !startDate.isAfter(today)
                && (endDate == null || !endDate.isBefore(today));
    }

    // --- EMBEDDED BENEFIT STRUCTURE ---

    /**
     * Embedded structure for perks and benefits associated with this appointment.
     *
     * <p>Stored as JSONB in PostgreSQL, allowing schema-flexible extension without
     * additional migrations. Key intelligence signals:</p>
     * <ul>
     *   <li>{@link #diplomaticPassport} — indicates travel privilege and potential
     *       immunity from standard border controls.</li>
     *   <li>{@link #securityDetail} — indicates the holder is considered
     *       a high-value or high-risk target.</li>
     *   <li>{@link #officialCarWithDriver} — logistics signal for surveillance planning.</li>
     *   <li>{@link #housingType} — state-provided housing links the person to
     *       a specific physical location.</li>
     * </ul>
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BenefitDetails {

        @JsonProperty("housingType")
        private HousingType housingType;

        @JsonProperty("officialCarWithDriver")
        private boolean officialCarWithDriver;

        @JsonProperty("securityDetail")
        private boolean securityDetail;

        @JsonProperty("travelBudget")
        private BigDecimal travelBudget;

        @JsonProperty("diplomaticPassport")
        private boolean diplomaticPassport;

        /**
         * Classifies the nature of state-provided or subsidised housing
         * associated with this appointment.
         */
        public enum HousingType {
            /** No state housing provision. */
            NONE,
            /** Official state residence (e.g. ministerial villa). */
            STATE_RESIDENCE,
            /** Monthly housing allowance paid to the holder. */
            ALLOWANCE,
            /** Social support housing for lower-tier positions. */
            SOCIAL_SUPPORT
        }
    }
}