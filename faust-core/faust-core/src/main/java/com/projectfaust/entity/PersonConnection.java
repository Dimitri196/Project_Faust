package com.projectfaust.entity;

import com.projectfaust.entity.enums.ConnectionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "person_connections", indexes = {
        @Index(name = "idx_conn_source", columnList = "source_person_id"),
        @Index(name = "idx_conn_target", columnList = "target_person_id")
})
@Audited
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PersonConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    private UUID externalId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_person_id", nullable = false)
    private Person sourcePerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_person_id", nullable = false)
    private Person targetPerson;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", nullable = false, length = 50)
    private ConnectionType connectionType;

    private Double influenceScore; // 0.0 - 1.0 (váha vztahu)

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate startDate;
    private LocalDate endDate;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
