package com.projectfaust.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents the formal assignment of a Person to a specific Occupation.
 * Tracks the temporal, financial, and logistical parameters of the position.
 */
@Entity
@Table(name = "appointments")
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
     * Permanent unique identifier for external systems and data synchronization.
     */
    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "occupation_id", nullable = false)
    private Occupation occupation;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    /**
     * Base monthly compensation before taxes and bonuses.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal monthlySalary;

    /**
     * Fixed monthly allowances (e.g., representation, per diem, meal allowances).
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal monthlyLumpSumAllowance;

    @Builder.Default
    @Column(length = 3)
    private String currency = "CZK";

    /**
     * Indicates if the person is temporarily holding the office (Acting/Pověřen řízením).
     */
    @Builder.Default
    @Column(name = "is_acting", nullable = false)
    private boolean acting = false;

    /**
     * Advanced logistical data stored as JSONB for high-granularity tracking.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "benefit_details", columnDefinition = "jsonb")
    private BenefitDetails benefitDetails;

    /**
     * Indicates if the position provides access to data or resources "Ex Offo" (by virtue of office).
     */
    @Column(name = "is_ex_offo_access", nullable = false)
    @Builder.Default
    private boolean exOffoAccess = false;

    @Column(length = 2000)
    private String appointmentNote;

    /**
     * Embedded structure for perks and benefits linked to the appointment.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BenefitDetails {
        private HousingType housingType;
        private boolean officialCarWithDriver;
        private boolean securityDetail;
        private BigDecimal travelBudget;
        private boolean diplomaticPassport;

        /**
         * Defines the nature of state-provided or subsidized housing.
         */
        public enum HousingType {
            NONE,
            STATE_RESIDENCE,
            ALLOWANCE,
            SOCIAL_SUPPORT
        }
    }

    /**
     * Checks if the appointment is currently active.
     */
    public boolean isActive() {
        LocalDate now = LocalDate.now();
        return (endDate == null || endDate.isAfter(now)) && startDate.isBefore(now);
    }
}
