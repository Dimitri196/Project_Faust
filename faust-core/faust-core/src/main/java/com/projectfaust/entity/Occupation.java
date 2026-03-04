package com.projectfaust.entity;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.OccupationCategory;
import com.projectfaust.validator.Hierarchical;
import jakarta.persistence.GeneratedValue;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Entita reprezentující konkrétní pracovní pozici nebo funkční slot v rámci instituce.
 * V projektu Faust definuje statickou strukturu moci, do které jsou následně jmenovány osoby.
 * Implementuje hierarchii (ReportsTo / Subordinates) nezávisle na institucionální hierarchii.
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(name = "occupations", indexes = {
        @Index(name = "idx_occ_external_id", columnList = "externalId"),
        @Index(name = "idx_occ_code", columnList = "code")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Occupation implements Hierarchical<Occupation> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unikátní identifikátor pro integraci s externími systémy (např. personální rejstříky).
     */
    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    /**
     * Oficiální název pozice (např. "Náměstek pro kybernetickou bezpečnost").
     */
    @Column(nullable = false)
    private String title;

    /**
     * Unikátní kód pozice (např. "GOV-UV-001") pro rychlé mapování v rámci státní správy.
     */
    @Column(unique = true, nullable = false)
    private String code;

    /**
     * Kategorizace role (např. EXECUTIVE, MILITARY, POLITICAL, ADVISORY).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OccupationCategory category;

    /**
     * Minimální úroveň utajení vyžadovaná pro obsazení této pozice.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ClearanceLevel  requiredClearanceLevel = ClearanceLevel.LEVEL_1_PUBLIC;

    /**
     * Instituce, pod kterou pozice administrativně spadá.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    /**
     * Přímý nadřízený slot v organizačním schématu.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reports_to_id")
    private Occupation reportsTo;

    /**
     * Příznak, zda je pozice aktuálně neobsazená.
     */
    @Column(name = "is_vacant")
    @Builder.Default
    private boolean isVacant = true;

    /**
     * Indikátor, zda pozice v aktuálním organizačním řádu stále existuje.
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Hodnost spojená s pozicí (převážně pro vojenské a policejní složky).
     */
    private String rank;

    @Column(length = 1000)
    private String description;

    // --- RELATIONS ---

    @Builder.Default
    @OneToMany(mappedBy = "reportsTo", cascade = CascadeType.ALL)
    @NotAudited
    private List<Occupation> subordinates = new ArrayList<>();

    /**
     * Historie jmenování na tuto pozici. Seřazeno od nejnovějších.
     */
    @Builder.Default
    @OneToMany(mappedBy = "occupation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("startDate DESC")
    @NotAudited
    private List<Appointment> appointments = new ArrayList<>();

    // --- HELPER METHODS ---

    @Override
    public Occupation getParent() { return this.reportsTo; }

    @Override
    public String getName() { return this.title; }

    /**
     * Identifikuje aktuálně aktivní jmenování (osobu, která v daný moment pozici zastává).
     * @return Optional obsahující aktivní Appointment, pokud existuje.
     */
    public Optional<Appointment> findCurrentAppointment() {
        return appointments.stream()
                .filter(a -> a.getEndDate() == null)
                .findFirst();
    }
}
