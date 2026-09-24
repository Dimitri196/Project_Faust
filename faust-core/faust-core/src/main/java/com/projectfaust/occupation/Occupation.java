package com.projectfaust.occupation;

import com.projectfaust.appointment.Appointment;
import com.projectfaust.institution.Institution;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.OccupationCategory;
import com.projectfaust.shared.enums.VerificationStatus;
import com.projectfaust.shared.validator.Hierarchical;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Entity representing a specific job position or functional slot within an institution.
 *
 * <p>In Project Faust, an {@code Occupation} defines the <em>static structure of power</em>
 * — a named role that exists independently of whoever holds it at any given time.
 * Persons are linked to positions via {@link Appointment} records, enabling full
 * temporal tracking of who held what position and when.</p>
 *
 * <p>Occupations implement their own reporting hierarchy (via {@link #reportsTo})
 * that is <em>independent</em> of the institutional hierarchy. A position at the
 * Ministry of Interior reports to the Deputy Minister slot — this org-chart relationship
 * is captured here, separate from the institution's administrative parent chain.</p>
 *
 * <p>The {@link #code} field provides a unique identifier for cross-system mapping
 * with official government registries (e.g. {@code GOV-UV-001}).</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(name = "occupations", indexes = {
        @Index(name = "idx_occ_external_id", columnList = "externalId"),
        @Index(name = "idx_occ_code",        columnList = "code")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Occupation implements Hierarchical<Occupation> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
     * Official title of the position
     * (e.g. "Deputy Minister for Cyber Security").
     */
    @Column(nullable = false)
    private String title;

    /**
     * Unique position code for cross-system mapping with official registries
     * (e.g. {@code GOV-UV-001}).
     */
    @Column(unique = true, nullable = false)
    private String code;

    /**
     * Functional category of this position
     * (e.g. EXECUTIVE, MILITARY, POLITICAL, ADVISORY).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OccupationCategory category;

    /**
     * Minimum security clearance required to hold this position.
     * Distinct from the holder's personal clearance — the position itself
     * carries a clearance requirement.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ClearanceLevel requiredClearanceLevel = ClearanceLevel.LEVEL_1_PUBLIC;

    /**
     * Evidentiary provenance and verification state of this record.
     * Defaults to {@code PENDING_REVIEW} for all newly ingested positions.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING_REVIEW;

    /**
     * The institution this position administratively belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    /**
     * The direct superior slot in the organisational chart.
     * This is the reporting line hierarchy — independent of the
     * institution's administrative parent chain.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reports_to_id")
    private Occupation reportsTo;

    /**
     * Whether the position is currently unfilled.
     * Named {@code vacant} to avoid the Lombok double-prefix bug
     * ({@code isVacant} would generate {@code isIsVacant()} in some configurations).
     * Lombok generates {@code isVacant()} correctly from {@code vacant}.
     */
    @Column(name = "is_vacant")
    @Builder.Default
    private boolean vacant = true;

    /**
     * Whether this position still exists in the current organisational structure.
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Military or police rank associated with this position.
     */
    private String rank;

    @Column(length = 1000)
    private String description;

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

    // --- RELATIONS ---

    /**
     * Direct subordinate positions in the reporting hierarchy.
     * Cascade limited to PERSIST and MERGE — subordinate positions must be
     * explicitly deactivated, never silently deleted with their superior.
     */
    @Builder.Default
    @OneToMany(mappedBy = "reportsTo", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @NotAudited
    private List<Occupation> subordinates = new ArrayList<>();

    /**
     * History of all appointments to this position, ordered most-recent first.
     * Cascade limited to PERSIST and MERGE — appointment history is never
     * auto-deleted when a position is removed.
     */
    @Builder.Default
    @OneToMany(mappedBy = "occupation",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY)
    @OrderBy("startDate DESC")
    @NotAudited
    private List<Appointment> appointments = new ArrayList<>();

    // --- HELPER METHODS ---

    /**
     * Returns the direct superior slot in the org-chart hierarchy.
     * Used by {@link com.projectfaust.shared.validator.HierarchyValidator}
     * for circular reference detection.
     *
     * @return the {@link Occupation} this position reports to, or {@code null} for top-level slots.
     */
    @Override
    public Occupation getParent() { return this.reportsTo; }

    /**
     * Returns the title of this position, used for diagnostic messages
     * in {@link com.projectfaust.shared.validator.HierarchyValidator}.
     *
     * @return the position title.
     */
    @Override
    public String getName() { return this.title; }

    /**
     * Identifies the currently active appointment — the person who holds
     * this position at the present moment.
     *
     * <p>An appointment is considered active when its {@code endDate} is null
     * (i.e. no termination date has been recorded).</p>
     *
     * @return an {@link Optional} containing the active {@link Appointment},
     *         or empty if the position is currently vacant.
     */
    public Optional<Appointment> findCurrentAppointment() {
        return appointments.stream()
                .filter(a -> a.getEndDate() == null)
                .findFirst();
    }
}