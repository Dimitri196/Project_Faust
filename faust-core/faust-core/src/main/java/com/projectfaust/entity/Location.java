package com.projectfaust.entity;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.LocationType;
import com.projectfaust.validator.Hierarchical;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a spatial node within the Faust project.
 * Enables mapping from geo-political entities (countries) down to
 * specific rooms within classified facilities.
 * * Supports hierarchical organization for tracking nested structures
 * (e.g., Building -> Floor -> Office).
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(name = "locations", indexes = {
        @Index(name = "idx_location_external_id", columnList = "externalId"),
        @Index(name = "idx_location_type", columnList = "type")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location implements Hierarchical<Location> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique public identifier for integration with external systems and the frontend.
     * Generated upon creation, immutable.
     */
    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    /**
     * Official name of the location (e.g., 'Strakova Academy' or 'Facility K-12').
     */
    @Column(nullable = false)
    private String name;

    /**
     * Categorization of the location (COUNTRY, CITY, BUILDING, ROOM, SECRET_FACILITY).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType type;

    /**
     * ISO 3166-1 alpha-2 code for countries, or internal coding for classified areas.
     */
    private String isoCode;

    /**
     * Latitude in decimal degrees. Crucial for map visualizations in the Faust UI.
     */
    private Double latitude;

    /**
     * Longitude in decimal degrees.
     */
    private Double longitude;

    /**
     * Security clearance level for the location (e.g., 0 = Public, 4 = Top Secret).
     * Determines visibility in the Dossier for different user clearance levels.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClearanceLevel clearanceLevel = ClearanceLevel.LEVEL_1_PUBLIC;

    /**
     * Indicator whether the location is currently active or in use.
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Parent location in the spatial hierarchy.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Location parent;

    /**
     * List of child locations (e.g., rooms within a building, buildings within a facility).
     */
    @Builder.Default
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Location> children = new ArrayList<>();

    /**
     * Timestamp of record creation for audit trail purposes.
     */
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    // --- Hierarchical interface implementation ---

    @Override
    public UUID getExternalId() {
        return this.externalId;
    }

    @Override
    public Location getParent() {
        return this.parent;
    }

    @Override
    public String getName() {
        return this.name;
    }
}

// pridat nekolik novych entit pro zpracovani dat z CSSZ, ZP, verejnych registru (Katastr, Auto, Busines, apod), social network.