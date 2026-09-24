package com.projectfaust.intelligence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity recording every access to sensitive intelligence resources within Project Faust.
 *
 * <p>Provides a tamper-evident audit trail of operator actions — who accessed what,
 * when, and from which IP address. Used for security audits and insider threat detection.</p>
 *
 * <p>This entity is intentionally write-only at the application layer — logs are
 * inserted on access and never modified or deleted via the API.</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(name = "intelligence_access_logs", indexes = {
        @Index(name = "idx_access_log_agent",    columnList = "agentId"),
        @Index(name = "idx_access_log_resource", columnList = "accessedResourceId"),
        @Index(name = "idx_access_log_ts",       columnList = "timestamp")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntelligenceAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public UUID of the operator who performed the action.
     * Stored as UUID column for efficient indexed lookups.
     */
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false)
    private UUID agentId;

    /**
     * Public UUID of the resource that was accessed
     * (person, institution, appointment, etc.).
     */
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false)
    private UUID accessedResourceId;

    /**
     * The type of action performed on the resource.
     * Examples: READ_DOSSIER, EXPORT_DATA, UPDATE_RISK_LEVEL, VIEW_CONNECTIONS.
     */
    @Column(nullable = false, length = 50)
    private String action;

    /**
     * Timestamp of the access event. Always stored with timezone offset.
     */
    @Column(nullable = false)
    private OffsetDateTime timestamp;

    /**
     * IP address of the operator's client at the time of access.
     * Used for geolocation and anomaly detection.
     */
    @Column(length = 45)
    private String ipAddress;
}