package com.projectfaust.controller;

import com.projectfaust.dto.request.AgentOnboardingRequest;
import com.projectfaust.dto.request.UpdateProfileRequest;
import com.projectfaust.dto.response.ProfileResponse;
import com.projectfaust.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing system operators, field agents, and analyst profiles.
 * Facilitates administrative tasks including onboarding, dossier management,
 * and operational status updates.
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    /**
     * Retrieves the profile associated with the currently authenticated session.
     * * @param email The identifier for the requesting operator.
     * @return The operator's profile details.
     */
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(@RequestParam String email) {
        return ResponseEntity.ok(userService.getProfileByEmail(email));
    }

    /**
     * Accesses the complete agency directory (Archive).
     * Provides a list of all registered personnel and their high-level statuses.
     *
     * @return A list of all operator profiles in the system.
     */
    @GetMapping("/archive")
    public ResponseEntity<List<ProfileResponse>> getArchive() {
        return ResponseEntity.ok(userService.getArchive());
    }

    /**
     * Retrieves a detailed administrative dossier for a specific agent.
     *
     * @param id The unique identifier (UUID) of the subject agent.
     * @return The full profile dossier.
     */
    @GetMapping("/dossier/{id}")
    public ResponseEntity<ProfileResponse> getDossier(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getDossier(id));
    }

    /**
     * Initiates the entry sequence for a new agency asset.
     * Configures initial credentials, clearance levels, and role assignments.
     *
     * @param request Data required for agent registration and role assignment.
     * @return The newly generated profile, including system-assigned identifiers.
     */
    @PostMapping("/onboard")
    public ResponseEntity<ProfileResponse> onboard(@RequestBody AgentOnboardingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.onboardAgent(request));
    }

    /**
     * Modifies specific attributes of an agent's dossier.
     * Used for updating operational status, tech stack competencies, or clearance updates.
     *
     * @param id The UUID of the profile to be updated.
     * @param request The set of attributes to be modified.
     * @return The updated profile response.
     */
    @PatchMapping("/dossier/{id}")
    public ResponseEntity<ProfileResponse> update(@PathVariable UUID id, @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(id, request));
    }
}
