package com.projectfaust.mapper;

import com.projectfaust.dto.response.ProfileResponse;
import com.projectfaust.entity.User;
import org.springframework.stereotype.Component;

/**
 * Component responsible for transforming internal System Operators and Analysts
 * from persistent entities into secure Profile DTOs.
 */
@Component
public class UserMapper {

    /**
     * Maps a User entity to a ProfileResponse.
     * Ensures that sensitive internal fields (like password hashes or internal audit logs)
     * are excluded while providing the necessary operational context for the UI.
     *
     * @param user The internal system user entity.
     * @return A ProfileResponse containing identity, clearance, and role data.
     */
    public ProfileResponse toResponse(User user) {
        if (user == null) return null;

        return new ProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getClearance(),
                user.getStatus(),
                user.getTechStack(),
                user.isAdmin()
        );
    }
}
