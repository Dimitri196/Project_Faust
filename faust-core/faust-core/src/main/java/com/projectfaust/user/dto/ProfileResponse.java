package com.projectfaust.user.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.UserRole;
import com.projectfaust.shared.enums.UserStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for the authenticated operator's profile.
 *
 * <p>The {@link #password} field from the {@link com.projectfaust.user.User} entity
 * is intentionally absent — credentials must never cross the API boundary.</p>
 *
 * @param id          the operator's account UUID.
 * @param fullName    the operator's display name.
 * @param email       the operator's login email.
 * @param role        the operator's functional role.
 * @param clearance   the operator's security clearance level.
 * @param status      the current account status.
 * @param techStack   technical skills assigned to the operator.
 * @param admin       whether the operator has global admin privileges.
 * @param createdAt   timestamp when the account was created.
 * @param updatedAt   timestamp of the most recent profile update.
 * @author Dimitri / Project Faust
 */
public record ProfileResponse(
        UUID id,
        String fullName,
        String email,
        UserRole role,
        ClearanceLevel clearance,
        UserStatus status,
        List<String> techStack,
        boolean admin,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}