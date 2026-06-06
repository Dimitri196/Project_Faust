package com.projectfaust.entity;

import com.projectfaust.entity.enums.InstitutionAccountRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;
import java.time.LocalDate;

@Entity
@Table(name = "institution_account_relations", indexes = {
        @Index(name = "idx_inst_account_lookup", columnList = "institution_id, bank_account_id")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstitutionAccountRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // Tady nullable parametr smazán
    @JoinColumn(name = "institution_id", nullable = false) // Tady je to správně
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY) // Tady nullable parametr smazán
    @JoinColumn(name = "bank_account_id", nullable = false) // Tady je to správně
    private BankAccount bankAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 50)
    private InstitutionAccountRole roleType; // OPERATIONAL, DISCRETIONARY_FUND, BUDGETARY

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;
}