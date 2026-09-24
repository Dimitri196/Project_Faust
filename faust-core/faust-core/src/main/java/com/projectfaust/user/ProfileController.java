package com.projectfaust.user;

import com.projectfaust.user.dto.AgentOnboardingRequest;
import com.projectfaust.user.dto.ProfileResponse;
import com.projectfaust.user.dto.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for managing operator profiles within Project Faust.
 *
 * <p>Handles the full lifecycle of system operator accounts — onboarding,
 * dossier retrieval, and profile updates.</p>
 *
 * <p>Security tiers: {@code VIEWER} reads own profile, {@code ADMIN} reads
 * all profiles and archive, {@code SUPER_ADMIN} onboards and updates.</p>
 *
 * <p>Base path: {@code /api/v1/profile}</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile Management",
        description = "Operations for managing system operator accounts and profiles")
public class ProfileController {

    private final UserService userService;

    // -------------------------------------------------------------------------
    // Read endpoints
    // -------------------------------------------------------------------------

    /**
     * Retrieves the profile of the currently authenticated operator.
     *
     * <p><b>FIXED:</b> previously accepted {@code @RequestParam email},
     * meaning any caller holding a valid VIEWER-level token could read
     * any other operator's profile simply by supplying a different email —
     * the endpoint was named "get own profile" but never actually verified
     * the caller's identity against the requested email. The email is now
     * extracted from the authenticated JWT principal
     * ({@link Authentication#getName()}) instead, so a caller can only ever
     * retrieve their own profile. There is no longer any parameter through
     * which a different operator's email could be supplied.</p>
     *
     * @param authentication the authenticated request principal, injected by
     *                       Spring Security; {@code getName()} resolves to
     *                       the email used as the JWT subject at login.
     * @return the profile of the authenticated operator.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get own profile",
            description = "Returns the profile of the authenticated operator, " +
                    "resolved from the JWT principal — not a client-supplied parameter.")
    public ResponseEntity<ProfileResponse> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getProfileByEmail(authentication.getName()));
    }

    /**
     * Returns a paginated list of all registered operator accounts.
     *
     * @param pageable pagination and sorting (default: 20 per page).
     * @return a page of operator profiles.
     */
    @GetMapping("/archive")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all operator profiles (paginated)",
            description = "Returns the full agency directory. Admin access required.")
    public ResponseEntity<Page<ProfileResponse>> getArchive(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getArchive(pageable));
    }

    /**
     * Retrieves the detailed administrative dossier for a specific operator.
     *
     * @param id the UUID of the target operator.
     * @return the full operator profile dossier.
     */
    @GetMapping("/dossier/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get operator dossier by UUID",
            description = "Returns the full profile dossier for a specific operator.")
    public ResponseEntity<ProfileResponse> getDossier(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getDossier(id));
    }

    // -------------------------------------------------------------------------
    // Write endpoints
    // -------------------------------------------------------------------------

    /**
     * Onboards a new operator into Project Faust.
     *
     * <p>Creates the account with {@code PENDING_ACTIVATION} status and hashes
     * the temporary password before storage. The operator must complete activation
     * before the account becomes {@code OPERATIONAL}.</p>
     *
     * @param request the onboarding request (validated).
     * @return the newly created operator profile with HTTP 201.
     */
    @PostMapping("/onboard")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Onboard a new operator",
            description = "Creates a new operator account with PENDING_ACTIVATION status. SUPER_ADMIN only.")
    public ResponseEntity<ProfileResponse> onboard(
            @Valid @RequestBody AgentOnboardingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.onboardAgent(request));
    }

    /**
     * Updates specific attributes of an operator's profile.
     *
     * <p>Supports partial updates — only non-null fields in the request are applied.
     * Email and password changes require dedicated endpoints.</p>
     *
     * @param id      the UUID of the operator to update.
     * @param request the partial update request (validated).
     * @return the updated operator profile.
     */
    @PatchMapping("/dossier/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update operator profile",
            description = "Partially updates role, clearance, status, or tech stack. SUPER_ADMIN only.")
    public ResponseEntity<ProfileResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(id, request));
    }
}