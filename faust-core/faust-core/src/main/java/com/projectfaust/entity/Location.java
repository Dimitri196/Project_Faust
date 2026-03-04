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
 * Entita reprezentující prostorový uzel v rámci projektu Faust.
 * Umožňuje mapování od geopolitických celků (státy) až po specifické místnosti v utajených objektech.
 * Podporuje hierarchické uspořádání pro sledování vnořených struktur (např. Budova -> Patro -> Kancl).
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
     * Unikátní veřejný identifikátor pro integraci s externími systémy a frontendem.
     * Generován při vytvoření, neměnný.
     */
    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    /**
     * Oficiální název lokace (např. 'Strakova akademie' nebo 'Objekt K-12').
     */
    @Column(nullable = false)
    private String name;

    /**
     * Kategorizace lokace (COUNTRY, CITY, BUILDING, ROOM, SECRET_FACILITY).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType type;

    /**
     * Kód ISO 3166-1 alpha-2 pro státy, případně interní kódování pro utajené oblasti.
     */
    private String isoCode;

    /**
     * Geografická šířka v desítkové soustavě. Klíčové pro mapové vizualizace ve Faust UI.
     */
    private Double latitude;

    /**
     * Geografická délka v desítkové soustavě.
     */
    private Double longitude;

    /**
     * Bezpečnostní úroveň přístupu do lokace (např. 0 = Veřejná, 4 = Přísně tajná).
     * Určuje viditelnost v Dossieru pro různé úrovně uživatelů.
     */
    @Enumerated(EnumType.STRING) // Ukládá text (PUBLIC, SECRET), což je čitelnější pro DB adminy
    @Column(nullable = false)
    private ClearanceLevel clearanceLevel = ClearanceLevel.LEVEL_1_PUBLIC;

    /**
     * Indikátor, zda je lokace aktuálně aktivní/používaná.
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Nadřazená lokace v hierarchii.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Location parent;

    /**
     * Seznam podřízených lokací (místnosti v budově, budovy v areálu).
     */
    @Builder.Default
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Location> children = new ArrayList<>();

    /**
     * Čas vytvoření záznamu pro účely auditní stopy.
     */
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    // --- Implementace Hierarchical interface ---

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
