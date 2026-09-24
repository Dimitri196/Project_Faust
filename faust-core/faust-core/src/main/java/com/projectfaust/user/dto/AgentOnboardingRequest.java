package com.projectfaust.user.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for onboarding a new operator into Project Faust.
 *
 * @param codename        the operator's display name or cover designation.
 * @param officialEmail   the official login email (must be unique).
 * @param assignedLevel   the initial security clearance level.
 * @param initialRole     the initial functional role.
 * @param temporaryPassword a temporary plaintext password — hashed by the service before storage.
 * @author Dimitri / Project Faust
 */
public record AgentOnboardingRequest(

        @NotBlank(message = "Operator name is required.")
        String codename,

        @NotBlank(message = "Official email is required.")
        @Email(message = "Must be a valid email address.")
        String officialEmail,

        @NotNull(message = "Security clearance level is required.")
        ClearanceLevel assignedLevel,

        @NotNull(message = "Initial role is required.")
        UserRole initialRole,

        @NotBlank(message = "Temporary password is required.")
        String temporaryPassword

) {}