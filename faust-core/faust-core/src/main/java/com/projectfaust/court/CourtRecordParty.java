package com.projectfaust.court;

import com.projectfaust.institution.Institution;
import com.projectfaust.person.Person;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a single party's involvement in a {@link CourtRecord}.
 *
 * <p>A court proceeding typically has multiple parties in different roles —
 * defendants, plaintiffs, prosecutors, witnesses, experts. This entity models
 * each party-role pair as a separate row, enabling queries such as:
 * <ul>
 *   <li>"All proceedings where person X appeared as DEFENDANT"</li>
 *   <li>"All proceedings where institution Y appeared as any party"</li>
 *   <li>"Co-defendants in case Z"</li>
 * </ul>
 *
 * <p>Each party is either a {@link Person} or an {@link Institution} —
 * the {@code partySubjectType} discriminator enforces the XOR constraint.</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(
        name = "court_record_parties",
        indexes = {
                @Index(name = "idx_crp_court_record",  columnList = "court_record_id"),
                @Index(name = "idx_crp_person",        columnList = "person_id"),
                @Index(name = "idx_crp_institution",   columnList = "institution_id"),
                @Index(name = "idx_crp_role",          columnList = "party_role")
        }
)
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourtRecordParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "external_id", nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // Parent proceeding
    // -------------------------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_record_id", nullable = false)
    private CourtRecord courtRecord;

    // -------------------------------------------------------------------------
    // Party subject — Person XOR Institution
    // -------------------------------------------------------------------------

    /**
     * Discriminator indicating whether this party is a person or institution.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "party_subject_type", nullable = false, length = 15)
    private CourtPartySubjectType partySubjectType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    /**
     * Name as it appears in the court records — preserved verbatim after
     * FK resolution, since registry spelling may differ from FAUST canonical.
     */
    @Column(name = "party_name_raw", length = 300)
    private String partyNameRaw;

    /** National ID / IČO / company number from court records. */
    @Column(name = "party_national_id", length = 50)
    private String partyNationalId;

    // -------------------------------------------------------------------------
    // Role in this proceeding
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "party_role", nullable = false, length = 30)
    private PartyRole partyRole;

    /**
     * Additional detail about the party's role or standing.
     * E.g. "co-defendant, charges subsequently dropped", "expert in forensic accounting".
     */
    @Column(name = "role_detail", columnDefinition = "TEXT")
    private String roleDetail;

    /**
     * True if this party was represented by legal counsel throughout
     * the proceeding.
     */
    @Column(name = "legally_represented")
    private Boolean legallyRepresented;

    /**
     * Name of the legal representative (attorney / firm) if known.
     */
    @Column(name = "legal_representative_name", length = 300)
    private String legalRepresentativeName;

    // -------------------------------------------------------------------------
    // Analyst context
    // -------------------------------------------------------------------------

    @Column(name = "analytical_note", columnDefinition = "TEXT")
    private String analyticalNote;

    @Column(name = "ingested_at", updatable = false)
    private OffsetDateTime ingestedAt;

    @PrePersist
    protected void onCreate() {
        ingestedAt = OffsetDateTime.now();
    }
}
