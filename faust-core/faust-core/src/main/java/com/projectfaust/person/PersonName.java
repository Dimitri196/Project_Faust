package com.projectfaust.person;

import com.projectfaust.shared.enums.NameType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a single name record in a person's identity history.
 *
 * <p>Every person can have multiple name records covering their full identity
 * lifecycle — legal name, maiden name, aliases, cover names, and historical names.
 * Names are never deleted; they are reclassified and given a {@code validTo} date
 * when superseded, preserving the complete identity trail.</p>
 *
 * <p>At most one name record per person should have {@code primary = true} at any
 * given time. Use {@link PersonNameService#addName} to safely append a new
 * additional name without disturbing the rest of the person's profile.</p>
 *
 * <p><b>ADDED:</b> {@code externalId} — previously this entity had only the
 * internal {@code Long id}, meaning individual name records could not be
 * addressed via REST (only the entire {@code Person.names} collection could
 * be replaced wholesale through {@code PUT /persons/{id}}). Adding this
 * follows the same public-UUID convention used by every other entity in
 * Project Faust, and enables the new dedicated names endpoints
 * (add/list without touching the rest of the person's profile).</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(name = "person_names", indexes = {
        @Index(name = "idx_name_person_id",   columnList = "person_id"),
        @Index(name = "idx_name_last_name",   columnList = "lastName"),
        @Index(name = "idx_name_external_id", columnList = "external_id")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public-facing UUID for this name record — used by the dedicated
     * names REST endpoints. Never exposed: the internal {@code Long id}.
     */
    @Builder.Default
    @Column(name = "external_id", nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    /**
     * The person this name record belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    /**
     * Classification of this name record
     * (LEGAL, ALIAS, COVER, MAIDEN, HISTORICAL, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 30)
    private NameType type = NameType.LEGAL;

    /**
     * Whether this is the current primary display name for the person.
     *
     * <p>Named {@code primary} to avoid the Lombok double-prefix bug —
     * a field named {@code isPrimary} causes Lombok to generate
     * {@code isIsPrimary()} in some configurations. Lombok generates the
     * correct {@code isPrimary()} accessor from the field name {@code primary}.</p>
     */
    @Builder.Default
    @Column(name = "is_primary")
    private boolean primary = false;

    /**
     * The date from which this name was in use; {@code null} if unknown.
     */
    private LocalDate validFrom;

    /**
     * The date until which this name was in use.
     * {@code null} indicates the name is still current/active.
     */
    private LocalDate validTo;

    @Column(length = 500)
    private String note;

    /**
     * Timestamp when this name record was first persisted.
     * Tracks when the identity information entered the system.
     */
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}