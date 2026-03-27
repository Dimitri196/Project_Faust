package com.projectfaust.entity;

import com.projectfaust.entity.enums.ThesisType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Reprezentuje vysokoškolskou závěrečnou práci subjektu.
 * Slouží k mapování odborné specializace a vazeb na akademické mentory (vedoucí práce).
 */
@Entity
@Table(name = "academic_theses", indexes = {
        @Index(name = "idx_thesis_external_id", columnList = "externalId"),
        @Index(name = "idx_thesis_university", columnList = "universityName")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicThesis {

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

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThesisType thesisType; // BACHELOR, MASTER, DOCTORAL, RIGOROUS

    @Column(nullable = false)
    private String universityName; // např. "Policejní akademie ČR", "Univerzita obrany"

    private String facultyName;

    private String supervisorName; // Jméno vedoucího práce - klíčové pro graf vztahů!

    private Integer defenseYear;

    /**
     * URL na repozitář (Theses.cz, repozitář UK, atd.).
     */
    private String repositoryUrl;

    /**
     * Příznak, pokud je práce v režimu utajení (časté u VZ a PA ČR).
     */
    @Builder.Default
    private boolean isClassified = false;

    @Column(columnDefinition = "TEXT")
    private String abstractText;

    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = OffsetDateTime.now(); }

    private String opponentName; // Druhý klíčový kontakt v akademické síti

    @Column(columnDefinition = "TEXT")
    private String keywords; // Pro fulltext a automatickou profilaci subjektu

    /**
     * ID práce v externím registru (např. "theses:12345").
     * Slouží pro idempotenci při zpracování z Kafky.
     */
    @Column(unique = true)
    private String sourceSystemId;

    /**
     * Příznak ručního ověření analytikem (shoda jména s reálnou osobou).
     */
    @Builder.Default
    private boolean isVerifiedByAgent = false;

    /**
     * Jazyk práce (často se u VZ objevují práce v AJ z NATO škol).
     */
    private String language;
}
