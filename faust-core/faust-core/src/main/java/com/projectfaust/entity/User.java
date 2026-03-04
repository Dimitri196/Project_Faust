package com.projectfaust.entity;

import com.projectfaust.entity.enums.ClearanceLevel;
import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;
import lombok.*;

/**
 * Represents an internal system operator or analyst within Project Faust.
 * This entity manages authentication, authorization (RBAC), and security clearance levels.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Primary functional role within the system (e.g., "ANALYST", "OPERATOR").
     */
    private String role;

    /**
     * Global administrative flag providing a bypass for standard security constraints.
     */
    private boolean isAdmin;

    /**
     * The security clearance level defining the sensitivity of data this user can access.
     */
    @Enumerated(EnumType.STRING)
    private ClearanceLevel clearance;

    /**
     * Current account status (e.g., "OPERATIONAL", "SUSPENDED", "INACTIVE").
     */
    private String status;

    /**
     * Technical competencies or specialized skills assigned to the operator.
     * Fetched eagerly to ensure immediate availability for resource allocation.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_tech_stack",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "technology")
    private List<String> techStack;

    /**
     * Hashed authentication credential.
     * WARNING: This field must never be exposed in DTOs or public API responses.
     */
    @Column(nullable = false)
    private String password;
}
