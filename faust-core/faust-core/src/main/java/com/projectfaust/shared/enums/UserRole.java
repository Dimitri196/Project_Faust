package com.projectfaust.shared.enums;

/**
 * Defines the functional roles available to operators within Project Faust.
 *
 * <p>Roles are used in {@code @PreAuthorize} annotations across all controllers.
 * Spring Security's {@code hasRole('X')} convention checks for {@code ROLE_X}
 * in the authority list — the {@code ROLE_} prefix is added by Spring automatically
 * when roles are granted via {@code SimpleGrantedAuthority("ROLE_" + role.name())}.</p>
 *
 * <p><b>Hierarchy (lowest to highest):</b></p>
 * <ul>
 *   <li>{@link #VIEWER} — read-only access to cleared data.</li>
 *   <li>{@link #ANALYST} — can create and update intelligence records.</li>
 *   <li>{@link #ADMIN} — full system access including bulk operations.</li>
 *   <li>{@link #SUPER_ADMIN} — unrestricted access including user management.</li>
 * </ul>
 *
 * @author Dimitri / Project Faust
 */
public enum UserRole {

    /**
     * Read-only access to intelligence data within the operator's clearance level.
     * Cannot create, update, or delete any records.
     */
    VIEWER,

    /**
     * Can create and update intelligence records (persons, institutions, appointments, etc.).
     * Cannot perform bulk imports or manage other user accounts.
     */
    ANALYST,

    /**
     * Full operational access including bulk imports and system-wide operations.
     * Cannot manage user accounts or modify security configurations.
     */
    ADMIN,

    /**
     * Unrestricted access — includes user account management, clearance assignment,
     * and security configuration. Reserved for system administrators.
     */
    SUPER_ADMIN
}