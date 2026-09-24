package com.projectfaust.ingest.theses;

import com.projectfaust.shared.enums.ThesisType;
import com.projectfaust.person.Person;
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
 *
 * <p><b>Oprava 1 — package:</b> přesunuto z {@code com.projectfaust.entity}
 * do {@code com.projectfaust.ingest.theses}, kde patří spolu s ostatními
 * třídami modulu (ThesisFetchService, ThesisIngestService, ThesisIngestController).
 * Entita {@code com.projectfaust.entity} byl starý plochý balíček před reorganizací.</p>
 *
 * <p><b>Oprava 2 — booleany:</b> {@code isClassified} a {@code isVerifiedByAgent}
 * přejmenovány na {@code classified} a {@code verifiedByAgent}. Lombok generuje
 * pro boolean pole s prefixem {@code is} gettery jako {@code isIsClassified()} —
 * duplicitní prefix způsobuje selhání JPA mapování a problémy s Jackson serializací.
 * Správný vzor: pole {@code classified}, Lombok vygeneruje {@code isClassified()}.
 * Stejná oprava jako u {@code Institution.stateOwned} (tam byl stejný bug).</p>
 *
 * <p><b>Poznámka k supervisorName / opponentName:</b> prozatím ukládáme jako String.
 * Budoucí iterace: přidat volitelný {@code @ManyToOne Person supervisor} vedle
 * {@code supervisorName} — při ingestu zkusit vyhledat shodu v DB podle jména,
 * pokud nalezena, propojit FK; pokud ne, ponechat jako String pro manuální
 * ověření analytikem ({@code verifiedByAgent = false}).</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(name = "academic_theses", indexes = {
        @Index(name = "idx_thesis_external_id",  columnList = "external_id"),
        @Index(name = "idx_thesis_university",    columnList = "university_name"),
        @Index(name = "idx_thesis_person",        columnList = "person_id"),
        @Index(name = "idx_thesis_source_system", columnList = "source_system_id")
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
    @Column(name = "external_id", nullable = false, unique = true, updatable = false)
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

    @Column(name = "university_name", nullable = false)
    private String universityName; // např. "Policejní akademie ČR", "Univerzita obrany"

    @Column(name = "faculty_name")
    private String facultyName;

    /**
     * Jméno vedoucího práce — klíčové pro graf vztahů.
     *
     * <p>Ukládáme jako String; při ingestu se pokusíme vyhledat shodu
     * v tabulce persons. Pokud nalezena → budoucí FK vazba.
     * Pokud ne → zůstane jako String, {@code verifiedByAgent = false}.</p>
     */
    @Column(name = "supervisor_name")
    private String supervisorName;

    /**
     * Jméno oponenta práce — druhý klíčový kontakt v akademické síti.
     * Stejná logika rozlišení jako u {@code supervisorName}.
     */
    @Column(name = "opponent_name")
    private String opponentName;

    @Column(name = "defense_year")
    private Integer defenseYear;

    /**
     * URL na repozitář (Theses.cz, repozitář UK, IS MU atd.).
     */
    @Column(name = "repository_url")
    private String repositoryUrl;

    /**
     * Příznak utajení — časté u prací z VZ, PA ČR a NATO škol.
     *
     * <p><b>FIXED:</b> přejmenováno z {@code isClassified} na {@code classified}.
     * Lombok generuje getter {@code isClassified()} správně z pole {@code classified}.
     * Původní {@code isClassified} způsobovalo getter {@code isIsClassified()} —
     * duplicitní prefix, JPA a Jackson selhávaly při mapování.</p>
     */
    @Builder.Default
    @Column(name = "classified")
    private boolean classified = false;

    @Column(name = "abstract_text", columnDefinition = "TEXT")
    private String abstractText;

    /**
     * Klíčová slova pro fulltext vyhledávání a automatickou profilaci subjektu.
     */
    @Column(columnDefinition = "TEXT")
    private String keywords;

    /**
     * ID práce v externím registru (např. "theses:12345", "semanticscholar:abc").
     * Slouží pro idempotenci při opakovaném ingestu — stejný vzor jako
     * {@code ExternalContract.externalId}.
     */
    @Column(name = "source_system_id", unique = true)
    private String sourceSystemId;

    /**
     * Příznak ručního ověření analytikem — shoda jména autora s reálnou osobou v DB.
     *
     * <p><b>FIXED:</b> přejmenováno z {@code isVerifiedByAgent} na {@code verifiedByAgent}.
     * Stejný důvod jako u {@code classified} výše.</p>
     */
    @Builder.Default
    @Column(name = "verified_by_agent")
    private boolean verifiedByAgent = false;

    /**
     * Jazyk práce — důležité pro práce z NATO škol (angličtina)
     * a mezinárodních programů.
     */
    private String language;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}