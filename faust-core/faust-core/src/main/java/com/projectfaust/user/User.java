package com.projectfaust.user;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.UserRole;
import com.projectfaust.shared.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing an internal operator or analyst account within Project Faust.
 *
 * <p>Manages authentication credentials, RBAC role assignment, security clearance,
 * and account status. Operator accounts are audited via Hibernate Envers —
 * every change to role, clearance, or status is versioned.</p>
 *
 * <p><b>Security note:</b> the {@link #password} field stores the hashed credential
 * and must never be exposed in any DTO, API response, or log output.</p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
public class User {

    /**
     * Primary key — UUID generated directly by the database.
     * Used as the public identifier (no separate externalId needed for User).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Functional role defining what operations this operator may perform.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    /**
     * Global administrative flag. When {@code true}, the operator has
     * elevated privileges bypassing standard security constraints.
     * Named {@code admin} to avoid the Lombok double-prefix bug.
     */
    @Column(name = "is_admin")
    private boolean admin;

    /**
     * Security clearance level defining the sensitivity of data
     * this operator is authorised to access.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClearanceLevel clearance;

    /**
     * Current account status — OPERATIONAL, SUSPENDED, INACTIVE, or PENDING_ACTIVATION.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status = UserStatus.PENDING_ACTIVATION;

    /**
     * Technical competencies or specialised skills assigned to the operator.
     * Loaded lazily — only fetched when explicitly accessed.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_tech_stack",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "technology")
    private List<String> techStack;

    /**
     * Hashed authentication credential (BCrypt or Argon2).
     * NEVER expose this field in any DTO, API response, or log output.
     */
    @Column(nullable = false)
    private String password;

    // -------------------------------------------------------------------------
    // Audit metadata
    // -------------------------------------------------------------------------

    @Column(updatable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}