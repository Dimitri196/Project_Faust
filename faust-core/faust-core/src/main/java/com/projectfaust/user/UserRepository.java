package com.projectfaust.user;

import com.projectfaust.shared.enums.UserRole;
import com.projectfaust.shared.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing internal operator accounts within Project Faust.
 *
 * <p>Extends {@link JpaRepository} with {@code UUID} as the primary key type —
 * consistent with the {@code GenerationType.UUID} strategy on the {@link User} entity.</p>
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Retrieves an operator by their registered email address.
     * Used during authentication and security audits.
     *
     * @param email the unique email address of the operator.
     * @return an {@link Optional} containing the operator, or empty if not found.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether an account with the given email already exists.
     * Used during onboarding to prevent duplicate account creation.
     *
     * @param email the email address to check.
     * @return {@code true} if an account with the given email exists.
     */
    boolean existsByEmail(String email);

    /**
     * Returns all operators assigned a specific functional role.
     * Used by administrators to audit role assignments.
     *
     * @param role the role to filter by.
     * @return list of operators with the given role.
     */
    List<User> findByRole(UserRole role);

    /**
     * Returns all operators with a specific account status.
     * Useful for surfacing suspended or pending-activation accounts
     * on the admin dashboard.
     *
     * @param status the account status to filter by.
     * @return list of operators with the given status.
     */
    List<User> findByStatus(UserStatus status);
}