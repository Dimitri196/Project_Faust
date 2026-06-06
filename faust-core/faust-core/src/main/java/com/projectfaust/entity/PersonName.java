package com.projectfaust.entity;

import com.projectfaust.entity.enums.NameType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.time.LocalDate;

@Entity
@Table(name = "person_names", indexes = {
        @Index(name = "idx_name_person_id", columnList = "person_id"),
        @Index(name = "idx_name_last_name", columnList = "lastName")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 30)
    private NameType type = NameType.LEGAL;

    @Builder.Default
    private boolean isPrimary = false;

    private LocalDate validFrom;
    private LocalDate validTo;

    @Column(length = 500)
    private String note;
}
