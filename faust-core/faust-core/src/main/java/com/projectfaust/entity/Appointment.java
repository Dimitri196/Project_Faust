package com.projectfaust.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(precision = 15, scale = 2)
    private BigDecimal monthlySalary; // Základní hrubý plat

    @Column(precision = 15, scale = 2)
    private BigDecimal monthlyLumpSumAllowance; // Paušální náhrady (reprezentace, stravné)

    @Builder.Default
    private String currency = "CZK";

    @Builder.Default
    @Column(name = "is_acting", nullable = false)
    private boolean acting = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "benefit_details", columnDefinition = "jsonb")
    private BenefitDetails benefitDetails;

    private String appointmentNote;

    /**
     * Struktura pro benefity navázané na pozici a jmenování
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

        public enum HousingType {
            NONE,
            STATE_RESIDENCE,
            ALLOWANCE,
            SOCIAL_SUPPORT
        }
    }
}
