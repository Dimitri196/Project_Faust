package com.projectfaust.user.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.UserRole;
import com.projectfaust.shared.enums.UserStatus;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for updating an operator's profile within Project Faust.
 *
 * <p>Email and password are deliberately excluded — changes to security-critical
 * credentials require dedicated endpoints with additional verification steps.</p>
 *
 * @param fullName   the operator's updated display name.
 * @param role       the updated functional role.
 * @param clearance  the updated security clearance level.
 * @param status     the updated account status.
 * @param techStack  updated list of technical skills.
 * @author Dimitri / Project Faust
 */
public record UpdateProfileRequest(

        @Size(max = 255, message = "Full name must not exceed 255 characters.")
        String fullName,

        UserRole role,
        ClearanceLevel clearance,
        UserStatus status,

        @Size(max = 50, message = "Tech stack must not exceed 50 entries.")
        List<String> techStack

) {}