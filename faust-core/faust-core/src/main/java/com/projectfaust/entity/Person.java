package com.projectfaust.entity;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.ContactType;
import com.projectfaust.entity.enums.EducationLevel;
import com.projectfaust.entity.enums.Gender;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.*;
import jakarta.validation.constraints.Past;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import org.hibernate.validator.constraints.URL;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;


/**
 * Representuje fyzickou osobu v systému Projekt Faust.
 * Uchovává biografické údaje, vzdělání a politickou příslušnost.
 * Identita osoby je fixována pomocí externalId, zatímco jména jsou vedena v historii.
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

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    /**
     * @deprecated Jména jsou nyní spravována v entitě PersonName.
     * Ponecháno pro účely databázové migrace.
     */
    @Deprecated
    @Column(name = "first_name", nullable = true)
    private String firstName;

    /**
     * @deprecated Jména jsou nyní spravována v entitě PersonName.
     * Ponecháno pro účely databázové migrace.
     */
    @Deprecated
    @Column(name = "last_name", nullable = true)
    private String lastName;

    private String titleBefore;
    private String titleAfter;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private EducationLevel educationLevel;

    private String fieldOfStudy;

    @Column(name = "political_affiliation")
    private String politicalAffiliation;

    @Column(columnDefinition = "TEXT")
    private String biography;

    @Column(name = "photo_url", length = 500)
    @Schema(description = "Secure link to the subject's biometric profile image", example = "https://cdn.faust.gov/intel/photos/p-342.jpg")
    @URL(message = "Provided photo storage path must be a valid URL")
    private String photoUrl;

    private String profileUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ClearanceLevel clearanceLevel = ClearanceLevel.LEVEL_1_PUBLIC;

    @Column(name = "full_name_search_normalized", insertable = false, updatable = false)
    private String fullNameSearchNormalized;

    @Past(message = "Datum narození musí být v minulosti")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(length = 100)
    private String nationality;

    @Column(name = "place_of_birth")
    private String placeOfBirth;

    private LocalDate deathDate;

    /**
     * Seznam všech jmen (historická, rodná, aliasy).
     */
    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<PersonName> names = new LinkedHashSet<>();

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    @OneToMany(mappedBy = "sourcePerson", cascade = CascadeType.ALL)
    @Builder.Default
    private List<PersonConnection> sourceConnections = new ArrayList<>();

    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<PersonContact> contacts = new LinkedHashSet<>();

    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private Set<PersonAccountRelation> financialAccounts = new LinkedHashSet<>();

    /**
     * Vrátí aktuální primární jméno osoby.
     */
    @Transient
    public PersonName getPrimaryNameRecord() {
        return names.stream()
                .filter(PersonName::isPrimary)
                .findFirst()
                .orElse(null);
    }

    @Transient
    public Integer getAge() {
        if (birthDate == null) return null;
        LocalDate endPoint = (deathDate != null) ? deathDate : LocalDate.now();
        return Period.between(birthDate, endPoint).getYears();
    }

    /**
     * Vrátí plné jméno osoby formátované podle českých typografických pravidel.
     * Prioritně bere data z tabulky person_names, sekundárně z fallback polí.
     */
    @Transient
    public String getFullName() {
        PersonName primary = getPrimaryNameRecord();
        String fName = (primary != null) ? primary.getFirstName() : this.firstName;
        String lName = (primary != null) ? primary.getLastName() : this.lastName;

        if (fName == null || lName == null) return "Neznámá identita";

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
     * Helper to retrieve the current primary active email address for the SPA dashboard.
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
     * Helper to retrieve the current primary active mobile connection.
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
