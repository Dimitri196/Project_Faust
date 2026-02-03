package com.projectfaust.entity;

import com.projectfaust.entity.enums.EducationLevel;
import jakarta.persistence.GeneratedValue;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "persons")
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
    private EducationLevel educationLevel;

    private String fieldOfStudy;

    @Column(unique = true)
    private String email;

    private String phone;

    @Column(length = 2000)
    private String biography;

    /**
     * Speciální sloupec spravovaný databází pro bleskové vyhledávání.
     * V Javě je pouze pro čtení (insertable/updatable = false).
     */
    @Column(name = "full_name_search_normalized", insertable = false, updatable = false)
    private String fullNameSearchNormalized;

    /**
     * Helper pro zobrazení jména včetně titulů.
     */
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (titleBefore != null && !titleBefore.isBlank()) {
            sb.append(titleBefore).append(" ");
        }
        sb.append(firstName).append(" ").append(lastName);
        if (titleAfter != null && !titleAfter.isBlank()) {
            sb.append(", ").append(titleAfter);
        }
        return sb.toString();
    }
}
