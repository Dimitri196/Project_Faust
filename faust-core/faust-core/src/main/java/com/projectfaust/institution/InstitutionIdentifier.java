package com.projectfaust.institution;

import com.projectfaust.ingest.core.ExternalContract;
import com.projectfaust.person.contact.PersonContact;
import com.projectfaust.shared.enums.IdentifierType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a single national or international registration identifier
 * for an {@link Institution}.
 *
 * <p>An institution may hold multiple identifiers across different schemes
 * simultaneously — e.g. a Czech state agency might have an IČO (Czech
 * national register), a LEI (international financial), and a DUNS
 * (NATO/US procurement). This entity captures all of them without
 * restricting an institution to a single registration scheme.</p>
 *
 * <p>This is the join point for linking institutions to external data
 * sources. {@link ExternalContract} records are
 * linked to institutions at query time by matching
 * {@code ExternalContract.buyerIco} / {@code ExternalContract.supplierIco}
 * against identifiers of type {@link IdentifierType#NATIONAL_REGISTRATION}. Future
 * sources (EU procurement via EUID, financial sanctions via LEI, NATO
 * procurement via DUNS) slot in by adding new identifier types without
 * any schema change.</p>
 *
 * <p>Mirrors the pattern of {@link PersonContact}
 * for persons — one entity, multiple typed contact vectors across
 * different channels.</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(
        name = "institution_identifiers",
        indexes = {
                @Index(name = "idx_inst_ident_institution", columnList = "institution_id"),
                @Index(name = "idx_inst_ident_value",       columnList = "value"),
                @Index(name = "idx_inst_ident_type_value",  columnList = "type, value")
        },
        uniqueConstraints = {
                // One institution cannot have the same identifier value registered
                // twice under the same scheme (e.g. two ICO_CZ entries with the
                // same number for the same institution is a data error).
                @UniqueConstraint(
                        name = "uq_inst_ident_institution_type_value",
                        columnNames = { "institution_id", "type", "value" }
                )
        }
)
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstitutionIdentifier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /**
     * The institution this identifier belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    /**
     * The registration/identification scheme this value belongs to
     * (e.g. ICO_CZ, LEI, DUNS).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IdentifierType type;

    /**
     * The actual identifier value within the given scheme.
     * Format is scheme-specific — see {@link IdentifierType} for details.
     */
    @Column(nullable = false, length = 100)
    private String value;

    /**
     * ISO 3166-1 alpha-2 country code of the register this identifier
     * belongs to (e.g. "CZ", "DE", "GB"). Null for international schemes
     * (LEI, DUNS, EUID) that are not country-specific.
     */
    @Column(name = "country_code", length = 2)
    private String countryCode;

    /**
     * Optional free-text note for CUSTOM identifier types, or to document
     * the specific sub-register when a scheme has regional variants.
     */
    @Column(length = 500)
    private String note;

    /**
     * Whether this identifier is currently valid and active.
     * Identifiers may be superseded (e.g. company re-registration) but
     * should be retained for historical correlation — mark as inactive
     * rather than deleting.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // --- AUDIT ---

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
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