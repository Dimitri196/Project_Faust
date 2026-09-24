package com.projectfaust.user;

import com.projectfaust.user.dto.ProfileResponse;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for transforming {@link User} entities into {@link ProfileResponse} DTOs.
 *
 * <p>Uses a manual {@code @Component} rather than MapStruct because the User entity
 * is simple and requires no nested graph resolution. The critical invariant is that
 * the {@link User#getPassword()} field is never included in any response.</p>
 *
 * @author Dimitri / Project Faust
 */
@Component
public class UserMapper {

    /**
     * Maps a {@link User} entity to a {@link ProfileResponse}.
     *
     * <p>The {@code password} field is intentionally excluded.
     * The {@code createdAt} and {@code updatedAt} timestamps are included
     * for account audit trail display.</p>
     *
     * @param user the operator entity.
     * @return a profile response safe for API exposure, or {@code null} if input is null.
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
                user.isAdmin(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}