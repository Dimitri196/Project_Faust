package com.projectfaust.repository;

import com.projectfaust.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing internal system users and their authentication credentials.
 * Handles access control and account lookup for the Project Faust administrative platform.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Retrieves a system user by their registered email address.
     * Primarily used during the authentication process and security audits.
     *
     * @param email The unique email address associated with the user account.
     * @return An Optional containing the found User, or empty if no account matches the email.
     */
    Optional<User> findByEmail(String email);
}
