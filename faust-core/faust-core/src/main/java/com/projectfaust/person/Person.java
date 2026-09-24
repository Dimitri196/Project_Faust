package com.projectfaust.person;

import com.projectfaust.appointment.Appointment;
import com.projectfaust.criminal.CriminalRecord;
import com.projectfaust.financial.PersonAccountRelation;
import com.projectfaust.ingest.realestate.core.RealEstateOwnership;
import com.projectfaust.insurance.InsuranceRecord;
import com.projectfaust.mobility.MobilityEvent;
import com.projectfaust.person.contact.PersonContact;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.ContactType;
import com.projectfaust.shared.enums.EducationLevel;
import com.projectfaust.shared.enums.Gender;
import com.projectfaust.shared.enums.VerificationStatus;
import com.projectfaust.vehicle.VehicleRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Past;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.*;

/**
 * Core HUMINT entity representing a physical person within Project Faust.
 *
 * <p>Stores biographical data, education, political affiliation, and clearance
 * level. The person's identity is anchored by {@link #externalId}, while names
 * are managed as a historical collection via {@link PersonName} — allowing
 * aliases, cover names, and name changes to be tracked over time.</p>
 *
 * <p><b>Name management:</b> {@link #firstName} and {@link #lastName} are
 * deprecated legacy fields retained only as a migration fallback. All name
 * operations should use the {@link #names} collection and {@link #getFullName()}.</p>
 *
 * <p><b>Key domain methods:</b></p>
 * <ul>
 *   <li>{@link #getFullName()} — derives the formatted display name from the
 *       primary {@link PersonName}, with Czech title formatting support.</li>
 *   <li>{@link #getAge()} — calculates age correctly for deceased subjects
 *       by using {@link #deathDate} as the reference point.</li>
 *   <li>{@link #getPrimaryEmail()} / {@link #getPrimaryPhone()} — surface the
 *       active primary contact without loading the full contact collection.</li>
 * </ul>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(name = "persons", indexes = {
        @Index(name = "idx_person_external_id", columnList = "externalId"),
        @Index(name = "idx_person_nationality", columnList = "nationality")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public-facing UUID for API and frontend components.
     * Generated on creation, immutable thereafter.
     */
    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // Deprecated legacy name fields — migration fallback only
    // -------------------------------------------------------------------------

    /**
     * @deprecated Names are now managed via {@link PersonName}.
     * Retained as a fallback for rows not yet migrated to the PersonName table.
     */
    @Deprecated
    @Column(name = "first_name")
    private String firstName;

    /**
     * @deprecated Names are now managed via {@link PersonName}.
     * Retained as a fallback for rows not yet migrated to the PersonName table.
     */
    @Deprecated
    @Column(name = "last_name")
    private String lastName;

    // -------------------------------------------------------------------------
    // Biographical fields
    // -------------------------------------------------------------------------

    private String titleBefore;
    private String titleAfter;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private EducationLevel educationLevel;

    private String fieldOfStudy;

    /**
     * Political party or movement affiliation. Key HUMINT analytical dimension.
     */
    @Column(name = "political_affiliation")
    private String politicalAffiliation;

    @Column(columnDefinition = "TEXT")
    private String biography;

    @Column(name = "photo_url", length = 500)
    @Schema(description = "Secure link to the subject's biometric profile image",
            example = "https://cdn.faust.gov/intel/photos/p-342.jpg")
    @URL(message = "Photo URL must be a valid URL.")
    private String photoUrl;

    private String profileUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ClearanceLevel clearanceLevel = ClearanceLevel.LEVEL_1_PUBLIC;

    /**
     * Evidentiary provenance and verification state of this record.
     * Defaults to {@code PENDING_REVIEW} for all newly ingested persons.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING_REVIEW;

    /**
     * Database-generated normalised full-name column for diacritic-insensitive
     * full-text search. Managed entirely by the DB trigger — never written by JPA.
     */
    @Column(name = "full_name_search_normalized", insertable = false, updatable = false)
    private String fullNameSearchNormalized;

    @Past(message = "Birth date must be in the past.")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(length = 100)
    private String nationality;

    @Column(name = "place_of_birth")
    private String placeOfBirth;

    private LocalDate deathDate;

    // -------------------------------------------------------------------------
    // Audit metadata
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Collections — cascade limited to PERSIST + MERGE
    // -------------------------------------------------------------------------

    /**
     * All names for this person — primary name, aliases, cover names, maiden names.
     * Cascade limited to PERSIST and MERGE — name history is never auto-deleted.
     */
    @Builder.Default
    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<PersonName> names = new LinkedHashSet<>();

    /**
     * All appointments this person has held across their career.
     * Cascade limited to PERSIST and MERGE — appointment history is never auto-deleted.
     */
    @Builder.Default
    @OneToMany(mappedBy = "person",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY)
    private List<Appointment> appointments = new ArrayList<>();

    /**
     * All outgoing person-to-person connections where this person is the source.
     * Cascade limited to PERSIST and MERGE.
     */
    @Builder.Default
    @OneToMany(mappedBy = "sourcePerson",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<PersonConnection> sourceConnections = new ArrayList<>();

    /**
     * All contact records for this person (phone, email, social, encrypted channels).
     * Cascade limited to PERSIST and MERGE — contact history is never auto-deleted.
     */
    @Builder.Default
    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<PersonContact> contacts = new LinkedHashSet<>();

    /**
     * All financial account relationships for this person.
     * Cascade limited to PERSIST and MERGE — financial relations are never auto-deleted.
     */
    @Builder.Default
    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<PersonAccountRelation> financialAccounts = new LinkedHashSet<>();

    /**
     * All real estate ownership records linked to this person via cadaster ingest.
     * Cascade limited to PERSIST + MERGE — ownership history is never auto-deleted.
     */
    @Builder.Default
    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<RealEstateOwnership> properties = new LinkedHashSet<>();

    /**
     * All insurance policy records for this person.
     * Cascade limited to PERSIST and MERGE — insurance history is never auto-deleted.
     */
    @Builder.Default
    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<InsuranceRecord> insuranceRecords = new ArrayList<>();

    /**
     * Vehicles for which this person is the registered owner.
     * Inverse side of {@link VehicleRecord#ownerPerson}.
     */
    @Builder.Default
    @OneToMany(mappedBy = "ownerPerson", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<VehicleRecord> vehicleRecordsOwned = new ArrayList<>();

    /**
     * Vehicles for which this person is the registered operator
     * (may differ from the legal owner — common in fleet / leasing contexts).
     * Inverse side of {@link VehicleRecord#operatorPerson}.
     */
    @Builder.Default
    @OneToMany(mappedBy = "operatorPerson", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<VehicleRecord> vehicleRecordsOperated = new ArrayList<>();

       /**
          * All criminal records in which this person is the subject.
          * Includes convictions, acquittals, expunged entries, and open investigations.
         * Defaults to {@code LEVEL_3_CONFIDENTIAL} at the record level.
         */
              @OneToMany(
                       mappedBy = "subjectPerson",
                      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
                      fetch = FetchType.LAZY
   )
         @Builder.Default
   private List<CriminalRecord> criminalRecords = new ArrayList<>();

                  /**
                    * All mobility events in which this person is a subject.
                   * Covers border crossings, violations, ANPR sightings, checkpoint stops.
                    *
                    * <p>Note: a {@link MobilityEvent} may simultaneously reference both this
                   * person and a {@link com.projectfaust.vehicle.VehicleRecord} — the link
                    * is not exclusive.</p>
                   */
                          @OneToMany(
                        mappedBy = "subjectPerson",
                        cascade = {CascadeType.PERSIST, CascadeType.MERGE},
                        fetch = FetchType.LAZY
    )
            @Builder.Default
    private List<MobilityEvent> mobilityEvents = new ArrayList<>();


    // -------------------------------------------------------------------------
    // Domain methods
    // -------------------------------------------------------------------------



    /**
     * Returns the primary {@link PersonName} record for this person.
     * Used as the source of truth for display name resolution.
     *
     * @return the primary name record, or {@code null} if none is designated.
     */
    @Transient
    public PersonName getPrimaryNameRecord() {
        return names.stream()
                .filter(PersonName::isPrimary)
                .findFirst()
                .orElse(null);
    }

    /**
     * Calculates the person's age in years.
     *
     * <p>For deceased subjects, age is calculated at the time of death rather
     * than the current date, preserving historical accuracy.</p>
     *
     * @return age in years, or {@code null} if birth date is unknown.
     */
    @Transient
    public Integer getAge() {
        if (birthDate == null) return null;
        LocalDate endPoint = (deathDate != null) ? deathDate : LocalDate.now();
        return Period.between(birthDate, endPoint).getYears();
    }

    /**
     * Returns the formatted full display name for this person.
     *
     * <p>Prioritises the primary {@link PersonName} record. Falls back to the
     * deprecated {@link #firstName} / {@link #lastName} fields for records not
     * yet migrated to the name table.</p>
     *
     * <p>Formats titles according to Czech typographic conventions:
     * {@code titleBefore firstName lastName, titleAfter}
     * (e.g. {@code Ing. Jan Novák, Ph.D.}).</p>
     *
     * @return the formatted full name, or {@code "UNKNOWN_IDENTITY"} if no name data exists.
     */
    @Transient
    public String getFullName() {
        PersonName primary = getPrimaryNameRecord();
        String fName = (primary != null) ? primary.getFirstName() : this.firstName;
        String lName = (primary != null) ? primary.getLastName() : this.lastName;

        if (fName == null || lName == null) return "UNKNOWN_IDENTITY";

        StringBuilder sb = new StringBuilder();
        if (titleBefore != null && !titleBefore.isBlank()) {
            sb.append(titleBefore.trim()).append(" ");
        }
        sb.append(fName.trim()).append(" ").append(lName.trim());
        if (titleAfter != null && !titleAfter.isBlank()) {
            sb.append(", ").append(titleAfter.trim());
        }
        return sb.toString();
    }

    /**
     * Returns the active primary email address for this person.
     * Used by the SPA dashboard to surface contact without loading the full collection.
     *
     * @return the primary active email address, or {@code null} if none exists.
     */
    @Transient
    public String getPrimaryEmail() {
        return contacts.stream()
                .filter(c -> c.isActive() && c.getContactType() == ContactType.EMAIL)
                .findFirst()
                .map(PersonContact::getContactValueRaw)
                .orElse(null);
    }

    /**
     * Returns the active primary mobile phone number for this person.
     * Used by the SPA dashboard to surface contact without loading the full collection.
     *
     * @return the primary active mobile number, or {@code null} if none exists.
     */
    @Transient
    public String getPrimaryPhone() {
        return contacts.stream()
                .filter(c -> c.isActive() && c.getContactType() == ContactType.CELLULAR_GSM)
                .findFirst()
                .map(PersonContact::getContactValueRaw)
                .orElse(null);
    }
}