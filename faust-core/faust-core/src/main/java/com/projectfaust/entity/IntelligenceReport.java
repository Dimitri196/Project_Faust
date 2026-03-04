package com.projectfaust.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "intelligence_reports")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class IntelligenceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    private UUID externalId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    @Column(columnDefinition = "TEXT")
    private String analysisResult;

    private LocalDateTime generatedAt;
    private String modelVersion;
}