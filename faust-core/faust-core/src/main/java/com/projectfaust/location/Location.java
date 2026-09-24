package com.projectfaust.location;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.LocationSourceType;
import com.projectfaust.shared.enums.LocationType;
import com.projectfaust.shared.enums.VerificationStatus;
import com.projectfaust.shared.validator.Hierarchical;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "locations", indexes = {
        @Index(name = "idx_location_external_id", columnList = "externalId"),
        @Index(name = "idx_location_type", columnList = "type"),
        @Index(name = "idx_location_coords", columnList = "latitude, longitude")
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

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    /**
     * Local language name of the location.
     *
     * <p>Stores the native-language name alongside the English {@link #name}
     * for analyst reference — e.g. name="Prague", localName="Praha";
     * name="Munich", localName="München".</p>
     *
     * <p>Shown as a subtitle in the UI. Not used for primary search or
     * parent resolution — English name is the platform standard.</p>
     *
     * <p>Null for locations where the English and local name are identical
     * (e.g. "Berlin", "Vienna") or where the local name is unknown.</p>
     */
    @Column(name = "local_name", length = 255)
    private String localName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType type;

    private String isoCode;
    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClearanceLevel clearanceLevel = ClearanceLevel.LEVEL_1_PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "source_type")
    @Builder.Default
    private LocationSourceType sourceType = LocationSourceType.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "verification_status")
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Location parent;

    // --- FIXED: ALL → PERSIST + MERGE (never auto-delete children) ---
    @Builder.Default
    @OneToMany(mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Location> children = new ArrayList<>();

    @Column(updatable = false)
    private OffsetDateTime createdAt;

    // --- NEW ---
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    // --- NEW ---
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @Override
    public UUID getExternalId() { return this.externalId; }

    @Override
    public Location getParent() { return this.parent; }

    @Override
    public String getName() { return this.name; }
}