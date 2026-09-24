package com.projectfaust.financial;

import com.projectfaust.person.Person;
import com.projectfaust.shared.enums.PersonAccountRole;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing the financial account relationship between a
 * {@link Person} and a {@link BankAccount}.
 *
 * <p>Captures the person's role with respect to the account (OWNER, SIGNATORY,
 * BENEFICIARY, etc.) and the temporal validity of the relationship.
 * Used for FININT cross-link analysis — identifying all persons connected
 * to a given account and all accounts connected to a given person.</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(name = "person_account_relations", indexes = {
        @Index(name = "idx_person_account_lookup",   columnList = "person_id, bank_account_id"),
        @Index(name = "idx_person_account_ext_id",   columnList = "external_id")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonAccountRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public-facing UUID for API exposure.
     * The internal Long id must never cross the API boundary.
     */
    @Builder.Default
    @Column(name = "external_id", nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 50)
    private PersonAccountRole roleType;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    /**
     * Whether this financial relationship is currently active.
     * Named {@code active} to avoid the Lombok double-prefix bug.
     */
    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    /**
     * Evidentiary provenance of this financial relationship record.
     * Defaults to PENDING_REVIEW for all newly ingested relations.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING_REVIEW;

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
}