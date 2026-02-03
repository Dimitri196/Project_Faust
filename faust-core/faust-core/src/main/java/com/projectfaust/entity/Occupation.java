package com.projectfaust.entity;

import com.projectfaust.entity.enums.OccupationCategory;
import com.projectfaust.validator.Hierarchical;
import jakarta.persistence.GeneratedValue;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "occupations")
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

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID externalId = UUID.randomUUID();

    @Column(nullable = false)
    private String title;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OccupationCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reports_to_id")
    private Occupation reportsTo;

    @Column(name = "is_vacant")
    @Builder.Default
    private boolean isVacant = true;

    private String rank;

    @Column(length = 1000)
    private String description;

    @Builder.Default
    @OneToMany(mappedBy = "reportsTo", cascade = CascadeType.ALL)
    private List<Occupation> subordinates = new ArrayList<>();

    // --- NEW: The link to Persons via Appointments ---

    @Builder.Default
    @OneToMany(mappedBy = "occupation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("startDate DESC")
    private List<Appointment> appointments = new ArrayList<>();

    // --- HELPER METHODS ---

    @Override
    public Occupation getParent() { return this.reportsTo; }

    @Override
    public String getName() { return this.title; }

    /**
     * Finds the currently active appointment (where endDate is null).
     * Useful for showing the "Current Occupant" in the UI.
     */
    public Optional<Appointment> findCurrentAppointment() {
        return appointments.stream()
                .filter(a -> a.getEndDate() == null)
                .findFirst();
    }
}