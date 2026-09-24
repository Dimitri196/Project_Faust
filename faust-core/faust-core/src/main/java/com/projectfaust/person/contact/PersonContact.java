package com.projectfaust.person.contact;

import com.projectfaust.person.Person;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.ContactType;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a specific communication or digital contact vector
 * associated with a HUMINT subject.
 *
 * <p>Within counter-intelligence operations, communication vectors are highly
 * transient — frequently reassigned or shared between operational cells.
 * This entity models each identifier as a separate, time-bounded intelligence
 * item rather than a static personal attribute.</p>
 *
 * <p>By tracking temporal bounds, signal metadata (IMEI, operator), and
 * confidence scores, the analytics layer can detect hardware multiplexing
 * (multiple targets alternating SIM cards in one device), operator jurisdiction
 * for intercept requests, and physical proximity anomalies across actors.</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(name = "person_contacts", indexes = {
        @Index(name = "idx_contact_person_id",        columnList = "person_id"),
        @Index(name = "idx_contact_value_normalized",  columnList = "contact_value_normalized"),
        @Index(name = "idx_contact_type",              columnList = "contact_type"),
        @Index(name = "idx_contact_imei",              columnList = "imei")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public-facing UUID for API exposure.
     * Prevents sequential enumeration of internal keys.
     */
    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    /**
     * The subject who uses or is suspected of using this contact vector.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /**
     * Communication channel type (EMAIL, CELLULAR_GSM, SECURE_IM, SAT_PHONE, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 50)
    private ContactType contactType;

    /**
     * Raw address or number exactly as ingested from telemetry or external registry.
     */
    @Column(name = "contact_value_raw", nullable = false, length = 500)
    private String contactValueRaw;

    /**
     * Normalised form used for deterministic exact-match lookups.
     * E.164 for phone numbers (+420777666555), lowercase for emails.
     */
    @Column(name = "contact_value_normalized", nullable = false, length = 500)
    private String contactValueNormalized;

    /**
     * Telecom operator or service provider (T-Mobile CZ, Vodafone, Kyivstar, Thuraya).
     * Used to map corporate jurisdiction or cross-reference intercept requests.
     */
    @Column(name = "operator_name", length = 150)
    private String operatorName;

    /**
     * IMEI or equivalent hardware fingerprint.
     * Critical for detecting hardware multiplexing across operational cells.
     */
    @Column(name = "imei", length = 50)
    private String imei;

    /**
     * Evidentiary provenance of this contact record.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "verification_status", nullable = false, length = 50)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    /**
     * Confidence metric from 0.0 (speculative) to 1.0 (confirmed).
     * Used by the graph visualisation to weight edge opacity.
     */
    @Builder.Default
    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore = 1.0;

    /**
     * Minimum clearance required to view this contact record in the SPA.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "clearance_level", nullable = false, length = 50)
    private ClearanceLevel clearanceLevel = ClearanceLevel.LEVEL_1_PUBLIC;

    /**
     * Whether the subject is currently active on this channel.
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Date from which this contact was valid; null if unknown. */
    @Column(name = "valid_from")
    private LocalDate validFrom;

    /** Date until which this contact was valid; null if still active. */
    @Column(name = "valid_to")
    private LocalDate validTo;

    /** Analyst notes — interception caveats, source details, secondary leads. */
    @Column(columnDefinition = "TEXT")
    private String analyticalNote;

    // -------------------------------------------------------------------------
    // Audit metadata
    // -------------------------------------------------------------------------

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}