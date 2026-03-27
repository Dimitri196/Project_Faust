package com.projectfaust.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "intelligence_access_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntelligenceAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String agentId; // ID uživatele v systému

    @Column(nullable = false)
    private String accessedResourceId; // UUID osoby nebo majetku

    @Column(nullable = false)
    private String action; // READ_DOSSIER, EXPORT_DATA, UPDATE_RISK_LEVEL

    @Column(nullable = false)
    private OffsetDateTime timestamp;

    private String ipAddress;
}
