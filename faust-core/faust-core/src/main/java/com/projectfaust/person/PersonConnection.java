package com.projectfaust.person;

import com.projectfaust.shared.enums.ConnectionType;
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
 * Entity representing a directed relationship between two persons in the
 * Project Faust intelligence network.
 *
 * <p>Connections are the edges of the HUMINT graph. Each connection has:</p>
 * <ul>
 *   <li>A {@link #connectionType} classifying the nature of the relationship
 *       (family, colleague, handler, associate, etc.).</li>
 *   <li>An {@link #influenceScore} (0.0–1.0) representing the weight of the edge
 *       for graph analytics and influence mapping algorithms.</li>
 *   <li>Temporal bounds ({@link #startDate} / {@link #endDate}) for historical
 *       relationship tracking.</li>
 * </ul>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(name = "person_connections", indexes = {
        @Index(name = "idx_conn_source",  columnList = "source_person_id"),
        @Index(name = "idx_conn_target",  columnList = "target_person_id"),
        @Index(name = "idx_conn_ext_id",  columnList = "externalId")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public-facing UUID. The internal Long id must never cross the API boundary.
     */
    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    /**
     * The person initiating or holding this relationship (source node).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_person_id", nullable = false)
    private Person sourcePerson;

    /**
     * The person this relationship points to (target node).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_person_id", nullable = false)
    private Person targetPerson;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", nullable = false, length = 50)
    private ConnectionType connectionType;

    /**
     * Edge weight for graph analytics (0.0 = weak link, 1.0 = strong/confirmed link).
     * Defaults to the {@link ConnectionType#getDefaultWeight()} for the connection type.
     */
    private Double influenceScore;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * Evidentiary provenance of this connection record.
     * Connections inferred from OSINT or HUMINT default to PENDING_REVIEW.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING_REVIEW;

    // -------------------------------------------------------------------------
    // Audit metadata
    // -------------------------------------------------------------------------

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