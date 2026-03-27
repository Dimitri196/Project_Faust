package com.projectfaust.entity;

import com.projectfaust.entity.enums.IncomeSourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Agregovaná data o příjmech subjektu z mimoresortních zdrojů (ČSSZ, Finanční úřady).
 */
@Entity
@Table(name = "external_income_reports")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalIncomeReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false)
    private Integer reportYear;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalNetIncome;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 30, nullable = false)
    private IncomeSourceType sourceType;

    private String employerTin; // IČO zaměstnavatele (pokud je relevantní)

    /**
     * Automatický příznak, pokud příjem vybočuje z historického průměru osoby.
     */
    private boolean anomalyFlag;

    private OffsetDateTime verifiedAt;
}
