package com.projectfaust.entity;

import com.projectfaust.shared.enums.AssetType;
import com.projectfaust.person.Person;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Reprezentuje majetkový uzel získaný z externích registrů (Katastr nemovitostí, Registr vozidel).
 * Slouží k detekci nesrovnalostí mezi oficiálními příjmy a reálným majetkem.
 */
@Entity
@Table(name = "external_assets", indexes = {
        @Index(name = "idx_asset_external_id", columnList = "externalId"),
        @Index(name = "idx_asset_type", columnList = "assetType")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetType assetType; // REAL_ESTATE, VEHICLE, SHARE, LUXURY_GOODS

    @Column(nullable = false)
    private String registrySource; // např. "ISKN", "CRV"

    /**
     * Identifikátor v rámci registru (např. číslo listu vlastnictví LV nebo VIN).
     */
    private String registryIdentifier;

    @Column(precision = 19, scale = 2)
    private BigDecimal estimatedValue;

    private LocalDate acquisitionDate;

    /**
     * Procentuální podíl (1.0 = 100%, 0.5 = spoluvlastnictví).
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal ownershipShare;

    @Column(columnDefinition = "TEXT")
    private String locationDescription; // např. "Katastrální území Modřany, parc. č. 123"

    @Builder.Default
    private boolean isVerifiedByAgent = false;

    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = OffsetDateTime.now(); }
}