package com.projectfaust.ingest.realestate.core;

import com.projectfaust.institution.Institution;
import com.projectfaust.person.Person;
import com.projectfaust.shared.enums.CadasterSourceSystem;
import com.projectfaust.shared.enums.PropertyType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a real estate ownership record ingested from a country-specific
 * cadaster registry.
 *
 * <p>Either {@code person} or {@code institution} will be non-null depending
 * on {@code ownerType} — natural persons link to {@link Person},
 * legal entities link to {@link Institution}. Co-ownership is expressed
 * via {@code ownershipShare} (e.g. "1/2") — one row per owner per property.</p>
 *
 * <p><b>GDPR note:</b> personal data (owner name, national ID) comes from
 * official public cadaster registries and is therefore lawfully processed
 * under GDPR Article 6(1)(e) — public interest. Access is gated by
 * clearance level via the standard FAUST role hierarchy.</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(
        name = "real_estate_ownerships",
        indexes = {
                @Index(name = "idx_reo_external_id",   columnList = "external_id"),
                @Index(name = "idx_reo_source_system", columnList = "source_system"),
                @Index(name = "idx_reo_person",        columnList = "person_id"),
                @Index(name = "idx_reo_institution",   columnList = "institution_id"),
                @Index(name = "idx_reo_country",       columnList = "country_code")
        },
        uniqueConstraints = {
                // Deduplication — same (sourceSystem, externalId) pair cannot appear twice
                @UniqueConstraint(name = "uq_reo_source_external",
                        columnNames = {"source_system", "external_id"})
        }
)
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealEstateOwnership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "external_id", nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(name = "source_system", nullable = false)
    private CadasterSourceSystem sourceSystem;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    // -------------------------------------------------------------------------
    // Owner — person XOR institution (one must be non-null)
    // -------------------------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    /**
     * Owner name as registered in the cadaster — preserved even after
     * FK resolution, since the cadaster spelling may differ from the
     * FAUST Person/Institution name.
     */
    @Column(name = "owner_name")
    private String ownerName;

    /**
     * Owner's national registration number from the cadaster
     * (IČO for CZ legal entities). Used for FK resolution.
     */
    @Column(name = "owner_national_id")
    private String ownerNationalId;

    // -------------------------------------------------------------------------
    // Property attributes
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false)
    private PropertyType propertyType;

    @Column(name = "property_address")
    private String propertyAddress;

    @Column(name = "cadastral_unit")
    private String cadastralUnit;

    @Column(name = "parcel_number")
    private String parcelNumber;

    /**
     * Fractional ownership share — null if sole owner, e.g. "1/2", "1/4"
     * for co-ownership. Stored as string to preserve exact fractions.
     */
    @Column(name = "ownership_share")
    private String ownershipShare;

    @Column(name = "estimated_value", precision = 20, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "currency", length = 3)
    private String currency;

    /**
     * True if the property has registered encumbrances (mortgage, easement,
     * lien, etc.) — key signal for financial analysis.
     */
    @Builder.Default
    @Column(name = "encumbered")
    private boolean encumbered = false;

    @Column(name = "registry_url")
    private String registryUrl;

    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData;

    // -------------------------------------------------------------------------
    // Source registry ID for deduplication
    // -------------------------------------------------------------------------

    /**
     * The property's unique ID in the source cadaster registry.
     * Used with {@code sourceSystem} for idempotent ingest deduplication.
     */
    @Column(name = "source_registry_id")
    private String sourceRegistryId;

    @Column(name = "ingested_at", updatable = false)
    private OffsetDateTime ingestedAt;

    @PrePersist
    protected void onCreate() {
        ingestedAt = OffsetDateTime.now();
    }
}