package com.projectfaust.entity;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.ContactType;
import com.projectfaust.entity.enums.VerificationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a specific communication or digital point of presence associated with a subject.
 * <p>
 * Within anti-corruption and counter-intelligence operations, communication vectors are highly transient,
 * frequently reassigned, or shared between operational cells. This entity models identifiers as separate,
 * time-bounded intelligence items rather than static personal attributes.
 * </p>
 * <p>
 * By tracking temporal boundaries (validity ranges), signal metadata (IMEI, operators), and intelligence confidence scores,
 * the analytics layer can deduce physical proximity anomalies, hardware swaps, or operational security overrides
 * across seemingly disparate actors.
 * </p>
 *
 * @author Dimitri
 */
@Entity
@Table(name = "person_contacts", indexes = {
        @Index(name = "idx_contact_person_id", columnList = "person_id"),
        @Index(name = "idx_contact_value_normalized", columnList = "contact_value_normalized"),
        @Index(name = "idx_contact_type", columnList = "contact_type"),
        @Index(name = "idx_contact_imei", columnList = "imei")
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
     * Unique structural identifier exposed to the presentation layer (SPA) and downstream API consumers.
     * Prevents sequential enumeration attacks and leaking system internal keys.
     */
    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    /**
     * The target subject who utilizes or is suspected of using this communication channel.
     */
    @NotNull(message = "Contact anchor must be bound to a target entity")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /**
     * Categorizes the communication domain (e.g., MOBILE_GSM, SECURE_IM, EMAIL, THREEMA_ID, ELEMENT_MATRIX, SAT_PHONE).
     */
    @NotNull(message = "Contact channel type classification is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 50)
    private ContactType contactType;

    /**
     * The raw address, handle, or number exactly as ingested from telemetry or external registry.
     */
    @NotBlank(message = "Raw contact identifier cannot be empty")
    @Column(name = "contact_value_raw", nullable = false, length = 500)
    private String contactValueRaw;

    /**
     * Programmatically sanitized and normalized representation used for deterministic exact-match lookups.
     * E.g., international E.164 phone formats without spaces (+420777666555) or lower-case trimmed emails.
     */
    @NotBlank(message = "Normalized lookup value must be computed upon ingestion")
    @Column(name = "contact_value_normalized", nullable = false, length = 500)
    private String contactValueNormalized;

    /**
     * The network or telecom provider managing the infrastructure (e.g., T-Mobile CZ, Vodafone, Kyivstar, Thuraya).
     * Used to map corporate jurisdiction or cross-reference signal intercept requests.
     */
    @Column(name = "operator_name", length = 150)
    private String operatorName;

    /**
     * International Mobile Equipment Identity (IMEI) or equivalent hardware fingerprint.
     * Critical for establishing hardware multiplexing (when multiple targets alternate SIM cards in a single device).
     */
    @Column(name = "imei", length = 50)
    private String imei;

    /**
     * Evaluates the provenance and validity of this specific link (e.g., VERIFIED_REGISTRY, COVERT_INTERCEPT, UNVERIFIED_HUMINT).
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "verification_status", nullable = false, length = 50)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    /**
     * Algorithmic confidence metric ranging from 0.00 (speculative trace) to 1.00 (absolute certainty).
     * Directly consumed by the SPA's link analysis visualization to adjust the opacity/weight of graph edges.
     */
    @Min(0)
    @Max(1)
    @Builder.Default
    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore = 1.0;

    /**
     * Restricts metadata rendering on the client view based on the current user's operating clearance level.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "clearance_level", nullable = false, length = 50)
    private ClearanceLevel clearanceLevel = ClearanceLevel.LEVEL_1_PUBLIC;

    /**
     * Denotes whether the target actively answers or broadcasts from this endpoint.
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /**
     * Operational window start: the date when the subject first acquired, activated, or was seen using this vector.
     */
    @Column(name = "valid_from")
    private LocalDate validFrom;

    /**
     * Operational window end: the date when the line was deactivated, the terminal destroyed, or the account closed.
     */
    @Column(name = "valid_to")
    private LocalDate validTo;

    /**
     * Field notes regarding interception caveats, metadata source details, or secondary user leads.
     */
    @Column(columnDefinition = "TEXT")
    private String analyticalNote;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}