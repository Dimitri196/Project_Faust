package com.projectfaust.auth.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.UserRole;

import java.util.UUID;

/**
 * Response DTO returned on successful authentication.
 *
 * @param token        the signed JWT token to be sent in subsequent requests
 *                     as {@code Authorization: Bearer <token>}.
 * @param expiresIn    token lifetime in milliseconds.
 * @param userId       the authenticated operator's account UUID.
 * @param email        the authenticated operator's email.
 * @param fullName     the authenticated operator's display name.
 * @param role         the operator's functional role.
 * @param clearance    the operator's security clearance level.
 * @author Dimitri / Project Faust
 */
public record AuthResponse(
        String token,
        long expiresIn,
        UUID userId,
        String email,
        String fullName,
        UserRole role,
        ClearanceLevel clearance
) {}