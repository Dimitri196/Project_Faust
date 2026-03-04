package com.projectfaust.service;

import com.projectfaust.dto.request.AgentOnboardingRequest;
import com.projectfaust.dto.request.UpdateProfileRequest;
import com.projectfaust.dto.response.ProfileResponse;
import com.projectfaust.entity.User;
import com.projectfaust.mapper.UserMapper;
import com.projectfaust.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service managing the lifecycle of system operators and intelligence analysts.
 * Orchestrates agent onboarding, dossier retrieval, and profile synchronization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Retrieves a user profile based on their registered email address.
     *
     * @param email The unique email address of the operator.
     * @return The profile response containing public identity and security data.
     * @throws RuntimeException if the subject is not found in the archive.
     */
    @Transactional(readOnly = true)
    public ProfileResponse getProfileByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Subject not found in archive."));
    }

    /**
     * Retrieves the complete archive of all system users.
     *
     * @return A list of all registered profiles.
     */
    @Transactional(readOnly = true)
    public List<ProfileResponse> getArchive() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    /**
     * Accesses a specific user's dossier by their unique identifier.
     *
     * @param id The UUID of the operator.
     * @return The detailed profile response.
     * @throws RuntimeException if the dossier is inaccessible or missing.
     */
    @Transactional(readOnly = true)
    public ProfileResponse getDossier(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Access denied to dossier: " + id));
    }

    /**
     * Registers a new agent into the system with initial security parameters.
     * Sets a temporary deployment password and assigns functional roles.
     *
     * @param request Data required for new agent registration.
     * @return The profile response of the newly onboarded agent.
     */
    @Transactional
    public ProfileResponse onboardAgent(AgentOnboardingRequest request) {
        User agent = new User();
        agent.setFullName(request.codename()); // Map codename to full name for registration
        agent.setEmail(request.officialEmail());
        agent.setClearance(request.assignedLevel());
        agent.setRole(request.requiresFieldAccess() ? "FIELD_OPERATIVE" : "ANALYST");
        agent.setStatus("ACTIVE");
        agent.setAdmin(false);
        agent.setPassword("INIT_SECRET_2026"); // Initial temporary credential
        agent.setTechStack(new ArrayList<>());

        User saved = userRepository.save(agent);
        log.info("System_Status: New agent successfully onboarded: {}", agent.getFullName());
        return userMapper.toResponse(saved);
    }

    /**
     * Updates an existing user's profile metadata and technical competencies.
     *
     * @param id The UUID of the user to be updated.
     * @param request The set of modified attributes.
     * @return The updated profile response.
     */
    @Transactional
    public ProfileResponse updateProfile(UUID id, UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found: " + id));

        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.role() != null) user.setRole(request.role());
        if (request.clearance() != null) user.setClearance(request.clearance());
        if (request.status() != null) user.setStatus(request.status());

        if (request.techStack() != null) {
            user.getTechStack().clear();
            user.getTechStack().addAll(request.techStack());
        }

        return userMapper.toResponse(userRepository.save(user));
    }
}
