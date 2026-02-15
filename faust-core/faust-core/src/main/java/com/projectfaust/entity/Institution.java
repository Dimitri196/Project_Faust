package com.projectfaust.entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.projectfaust.entity.enums.HierarchicalLevel;
import com.projectfaust.entity.enums.InstitutionType;
import com.projectfaust.validator.Hierarchical;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "institutions")
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

    @Column(nullable = false, unique = true)
    private String name;

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID) // Ensures Postgres uses the native 'uuid' type
    private UUID externalId = UUID.randomUUID();

    private String countryCode;

    /** * Konec countryCode. Nyní odkazujeme na uzel v mapě světa.
     * Může to být COUNTRY (Česko) nebo až SUBLOCATION (Místnost 404).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HierarchicalLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private InstitutionType type;

    @JsonProperty("isStateOwned")
    private boolean isStateOwned = false;

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

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    @Builder.Default // Required so Builder doesn't make this null
    private List<Institution> children = new ArrayList<>();

    // --- HELPER METHODS ---

    public void addChild(Institution child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(Institution child) {
        children.remove(child);
        child.setParent(null);
    }

    @Override
    public String getName() { return this.name; }
}
