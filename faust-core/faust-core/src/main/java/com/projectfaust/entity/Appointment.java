package com.projectfaust.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "appointments")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "occupation_id", nullable = false)
    private Occupation occupation;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate; // Null if the person is currently in the role

    @Builder.Default
    @Column(name = "is_acting", nullable = false) // Přidáno nullable = false
    private boolean acting = false;

    private String appointmentNote; // e.g., "Appointed by the Government resolution No. X"
}
