package com.projectfaust.institution;

import com.projectfaust.shared.enums.InstitutionalEvolutionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "institutional_evolutions", indexes = {
        @Index(name = "idx_inst_evo_external_id", columnList = "externalId")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstitutionalEvolution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predecessor_id", nullable = false)
    private Institution predecessor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "successor_id")
    private Institution successor;

    @Enumerated(EnumType.STRING)
    @Column(name = "evolution_type", nullable = false)
    private InstitutionalEvolutionType type;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "legal_basis", columnDefinition = "TEXT")
    private String legalBasis;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}