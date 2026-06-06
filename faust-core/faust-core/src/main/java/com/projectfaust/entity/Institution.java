package com.projectfaust.entity;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.HierarchicalLevel;
import com.projectfaust.entity.enums.InstitutionType;
import com.projectfaust.validator.Hierarchical;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Entita reprezentující organizační uzel v projektu Faust.
 * Mapuje státní úřady, ministerstva, tajné služby i soukromé subjekty zapojené do vládní agendy.
 * Umožňuje rekurzivní vnořování (např. Úřad vlády -> Sekce -> Odbor -> Oddělení).
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(name = "institutions", indexes = {
        @Index(name = "idx_inst_external_id", columnList = "externalId"),
        @Index(name = "idx_inst_type", columnList = "type")
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

    /**
     * Unikátní veřejný identifikátor pro API a frontendové komponenty (např. Dossier).
     */
    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    /**
     * Oficiální název instituce.
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Vazba na geografickou páteř.
     * Určuje fyzické sídlo instituce (např. Strakova akademie).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    /**
     * Rozlišuje úroveň v rámci státní/organizační správy (NATIONAL, REGIONAL, LOCAL).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HierarchicalLevel level;

    /**
     * Kategorizace instituce (EXECUTIVE, LEGISLATIVE, INTELLIGENCE, PRIVATE_SECTOR).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private InstitutionType type;

    /**
     * Úroveň utajení instituce.
     * Určuje, který uživatel systému Faust má právo vidět detaily této organizace.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ClearanceLevel clearanceLevel = ClearanceLevel.LEVEL_1_PUBLIC;

    /**
     * Příznak, zda jde o subjekt přímo ovládaný nebo vlastněný státem.
     */
    @Builder.Default
    private boolean isStateOwned = false;

    /**
     * Indikátor, zda instituce aktuálně existuje a plní svou funkci.
     */
    @Builder.Default
    private boolean active = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "website_url", length = 512)
    private String websiteUrl;

    // --- HIERARCHIE ---

    /**
     * Nadřazená instituce (např. Ministerstvo jako rodič pro podřízený úřad).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Institution parent;

    /**
     * Podřízené organizační složky.
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited // Historii vazeb řeší strana "ManyToOne", seznam dětí v auditu nepotřebujeme
    @Builder.Default
    private List<Institution> children = new ArrayList<>();

    @OneToMany(mappedBy = "institution", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<InstitutionAccountRelation> financialAccounts = new LinkedHashSet<>();

    // --- AUDIT METADATA ---

    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
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

    @Override
    public UUID getExternalId() { return this.externalId; }

    @Override
    public String getName() { return this.name; }

    @Override
    public Institution getParent() { return this.parent; }
}
