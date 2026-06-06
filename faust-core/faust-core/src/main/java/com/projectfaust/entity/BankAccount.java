package com.projectfaust.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "bank_accounts", indexes = {
        @Index(name = "idx_account_iban", columnList = "iban"),
        @Index(name = "idx_account_external_id", columnList = "externalId")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    @Column(nullable = false, length = 34) // Max IBAN délka
    private String iban;

    @Column(length = 11)
    private String bic;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency; // CZK, EUR, USD...

    @Column(name = "is_monitored")
    @Builder.Default
    private boolean isMonitored = false; // Příznak pro sledování finanční inteligencí

    @Column(columnDefinition = "TEXT")
    private String analyticalNote;

    @OneToMany(mappedBy = "bankAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PersonAccountRelation> accountHolders = new LinkedHashSet<>();

    @OneToMany(mappedBy = "bankAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<InstitutionAccountRelation> institutionalOwners = new LinkedHashSet<>();
}