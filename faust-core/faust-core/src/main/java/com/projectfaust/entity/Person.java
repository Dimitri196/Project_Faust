package com.projectfaust.entity;

import com.projectfaust.entity.enums.EducationLevel;
import com.projectfaust.entity.enums.Gender;
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
import java.util.UUID;

/**
 * Representuje fyzickou osobu v systému Projekt Faust.
 * Uchovává biografické údaje, vzdělání a politickou příslušnost.
 */
@Entity
@Table(name = "persons", indexes = {
        @Index(name = "idx_person_external_id", columnList = "externalId"),
        @Index(name = "idx_person_last_name", columnList = "lastName"),
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

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String titleBefore;
    private String titleAfter;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private EducationLevel educationLevel;

    private String fieldOfStudy;

    @Column(unique = true)
    private String email;

    private String phone;

    @Column(name = "political_affiliation")
    private String politicalAffiliation;

    @Column(length = 2000)
    private String biography;

    @Column(name = "photo_url", length = 500)
    @Schema(description = "Secure link to the subject's biometric profile image", example = "https://cdn.faust.gov/intel/photos/p-342.jpg")
    @URL(message = "Provided photo storage path must be a valid URL")
    private String photoUrl;

    /**
     * Speciální sloupec spravovaný databází pro fulltextové vyhledávání.
     */
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

    /**
     * Datum úmrtí pro historické záznamy.
     */
    private LocalDate deathDate;

    /**
     * Vypočítá aktuální věk osoby k dnešnímu dni nebo ke dni úmrtí.
     * @return věk v letech nebo null, pokud není známo datum narození.
     */
    @Transient
    public Integer getAge() {
        if (birthDate == null) {
            return null;
        }
        LocalDate endPoint = (deathDate != null) ? deathDate : LocalDate.now();
        return Period.between(birthDate, endPoint).getYears();
    }

    /**
     * Vrátí plné jméno osoby formátované podle českých typografických pravidel.
     * Příklad: "Ing. Petr Pavel, M.A."
     */
    @Transient
    public String getFullName() {
        StringBuilder sb = new StringBuilder();

        if (titleBefore != null && !titleBefore.isBlank()) {
            sb.append(titleBefore.trim()).append(" ");
        }

        sb.append(firstName.trim()).append(" ").append(lastName.trim());

        if (titleAfter != null && !titleAfter.isBlank()) {
            sb.append(", ").append(titleAfter.trim());
        }

        return sb.toString();
    }
}
