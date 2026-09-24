package com.projectfaust.institution;

import com.projectfaust.criminal.CriminalRecord;
import com.projectfaust.financial.InstitutionAccountRelation;
import com.projectfaust.location.Location;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.HierarchicalLevel;
import com.projectfaust.shared.enums.InstitutionType;
import com.projectfaust.shared.enums.VerificationStatus;
import com.projectfaust.shared.validator.Hierarchical;
import com.projectfaust.vehicle.VehicleRecord;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "institutions", indexes = {
        @Index(name = "idx_inst_external_id", columnList = "externalId"),
        @Index(name = "idx_inst_type",        columnList = "type"),
        @Index(name = "idx_inst_name",        columnList = "name")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Institution implements Hierarchical<Institution> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HierarchicalLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private InstitutionType type;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ClearanceLevel clearanceLevel = ClearanceLevel.LEVEL_1_PUBLIC;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING_REVIEW;

    @Builder.Default
    private boolean stateOwned = false;

    @Builder.Default
    private boolean active = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "website_url", length = 512)
    private String websiteUrl;

    // --- HIERARCHY ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Institution parent;

    @Builder.Default
    @OneToMany(mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @NotAudited
    private List<Institution> children = new ArrayList<>();

    // --- FINANCIAL ---

    @Builder.Default
    @OneToMany(mappedBy = "institution", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<InstitutionAccountRelation> financialAccounts = new LinkedHashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "institution", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<InstitutionIdentifier> identifiers = new LinkedHashSet<>();

    /**
     * Vehicles for which this institution is the registered owner (fleet).
     * Inverse side of {@link VehicleRecord#ownerInstitution}.
     */
    @Builder.Default
    @OneToMany(mappedBy = "ownerInstitution", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<VehicleRecord> vehicleFleetOwned = new ArrayList<>();

    /**
     * Vehicles operated (managed / assigned) by this institution but not
     * necessarily legally owned by it. Inverse side of
     * {@link VehicleRecord#operatorInstitution}.
     */
    @Builder.Default
    @OneToMany(mappedBy = "operatorInstitution", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<VehicleRecord> vehicleFleetOperated = new ArrayList<>();

        /**
          * All criminal records in which this institution is the subject.
          * Covers corporate criminal liability, money-laundering cases, etc.
          * Defaults to {@code LEVEL_3_CONFIDENTIAL} at the record level.
          */
                @OneToMany(
                        mappedBy = "subjectInstitution",
                        cascade = {CascadeType.PERSIST, CascadeType.MERGE},
                        fetch = FetchType.LAZY
    )
            @Builder.Default
    private List<CriminalRecord> criminalRecords = new ArrayList<>();

    // --- AUDIT ---

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

    // --- HELPER METHODS ---

    public void addChild(Institution child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(Institution child) {
        children.remove(child);
        child.setParent(null);
    }

    // NEW: convenience helper — adds an identifier and maintains the
    // bidirectional relationship, same pattern as addChild.
    public void addIdentifier(InstitutionIdentifier identifier) {
        identifiers.add(identifier);
        identifier.setInstitution(this);
    }

    @Override
    public UUID getExternalId() { return this.externalId; }

    @Override
    public String getName()     { return this.name; }

    @Override
    public Institution getParent() { return this.parent; }
}