package com.projectfaust.user;

import com.projectfaust.shared.enums.UserRole;
import com.projectfaust.shared.enums.UserStatus;
import com.projectfaust.user.dto.AgentOnboardingRequest;
import com.projectfaust.user.dto.ProfileResponse;
import com.projectfaust.user.dto.UpdateProfileRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Service managing the lifecycle of system operators and intelligence analysts.
 *
 * <p>Handles agent onboarding with secure password hashing, profile retrieval,
 * and profile updates. All password handling uses {@link PasswordEncoder}
 * (BCrypt) — plaintext passwords are never stored.</p>
 *
 * <p><b>Lazy collection note:</b> {@link User#getTechStack()} is mapped as
 * {@code @ElementCollection(fetch = FetchType.LAZY)}. Every read path that
 * returns a {@link ProfileResponse} must force-initialize this collection
 * via {@link Hibernate#initialize(Object)} <em>before</em> the transaction
 * closes — otherwise Jackson throws {@code LazyInitializationException}
 * during serialization, after the Hibernate session is already gone.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Registers a new operator into the system.
     *
     * <p>The temporary password from the request is hashed with BCrypt before
     * storage — it is never persisted in plaintext. The account is created
     * with {@link UserStatus#PENDING_ACTIVATION} status — the operator must
     * complete activation (e.g. first-login password change) before becoming
     * {@link UserStatus#OPERATIONAL}.</p>
     *
     * @param request the onboarding request containing identity and credentials.
     * @return the newly created operator profile.
     * @throws IllegalStateException if an account with the given email already exists.
     */
    @Transactional
    public ProfileResponse onboardAgent(AgentOnboardingRequest request) {
        if (userRepository.existsByEmail(request.officialEmail())) {
            throw new IllegalStateException(
                    "DUPLICATE_EMAIL: An account with email " + request.officialEmail() + " already exists.");
        }

        User agent = new User();
        agent.setFullName(request.codename());
        agent.setEmail(request.officialEmail());
        agent.setClearance(request.assignedLevel());
        agent.setRole(request.initialRole());
        agent.setStatus(UserStatus.PENDING_ACTIVATION);
        agent.setAdmin(false);
        agent.setPassword(passwordEncoder.encode(request.temporaryPassword()));
        agent.setTechStack(new ArrayList<>());

        User saved = userRepository.save(agent);
        log.info("FAUST_USER: New operator onboarded — {}.", agent.getFullName());
        // techStack was just set to a non-lazy ArrayList above, already
        // initialized — no Hibernate.initialize() needed here.
        return userMapper.toResponse(saved);
    }

    /**
     * Updates an existing operator's profile metadata and technical competencies.
     *
     * <p>Only non-null fields in the request are applied — omitting a field
     * leaves the current value unchanged.</p>
     *
     * @param id      the UUID of the operator to update.
     * @param request the partial update request.
     * @return the updated operator profile.
     * @throws EntityNotFoundException if no operator matches the given UUID.
     */
    @Transactional
    public ProfileResponse updateProfile(UUID id, UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND: " + id));

        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.role() != null)     user.setRole(request.role());
        if (request.clearance() != null) user.setClearance(request.clearance());
        if (request.status() != null)   user.setStatus(request.status());

        if (request.techStack() != null) {
            // Accessing the collection here already triggers lazy initialization
            // within the open transaction (clear()/addAll() require the
            // collection to be loaded), so techStack is safe to serialize
            // after this method returns.
            user.getTechStack().clear();
            user.getTechStack().addAll(request.techStack());
        } else {
            // NEW: even if techStack isn't being updated, it must still be
            // initialized before the transaction closes so the returned
            // ProfileResponse can be serialized safely.
            Hibernate.initialize(user.getTechStack());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Retrieves an operator profile by email address.
     *
     * @param email the registered email of the operator.
     * @return the operator's profile.
     * @throws EntityNotFoundException if no operator matches the given email.
     */
    @Transactional(readOnly = true)
    public ProfileResponse getProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND: " + email));

        // NEW: force-initialize the lazy techStack collection while the
        // Hibernate session is still open. Without this, Jackson throws
        // LazyInitializationException ("no session") when serializing
        // ProfileResponse.techStack after the transaction has closed.
        Hibernate.initialize(user.getTechStack());

        return userMapper.toResponse(user);
    }

    /**
     * Retrieves a paginated list of all operator accounts.
     *
     * @param pageable pagination and sorting parameters.
     * @return a page of operator profiles.
     */
    @Transactional(readOnly = true)
    public Page<ProfileResponse> getArchive(Pageable pageable) {
        // NEW: same lazy-initialization fix applied across the page contents
        // before mapping — otherwise every entry's techStack would fail
        // serialization identically.
        Page<User> users = userRepository.findAll(pageable);
        users.forEach(u -> Hibernate.initialize(u.getTechStack()));
        return users.map(userMapper::toResponse);
    }

    /**
     * Retrieves all operators with a specific role.
     *
     * @param role the role to filter by.
     * @return list of operators with the given role.
     */
    @Transactional(readOnly = true)
    public java.util.List<ProfileResponse> getByRole(UserRole role) {
        var users = userRepository.findByRole(role);
        // NEW: same lazy-initialization fix.
        users.forEach(u -> Hibernate.initialize(u.getTechStack()));
        return users.stream()
                .map(userMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves a specific operator's profile by UUID.
     *
     * @param id the UUID of the operator.
     * @return the operator's profile.
     * @throws EntityNotFoundException if no operator matches the given UUID.
     */
    @Transactional(readOnly = true)
    public ProfileResponse getDossier(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND: " + id));

        // NEW: same lazy-initialization fix.
        Hibernate.initialize(user.getTechStack());

        return userMapper.toResponse(user);
    }
}