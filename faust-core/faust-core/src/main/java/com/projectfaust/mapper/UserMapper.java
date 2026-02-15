package com.projectfaust.mapper;

import com.projectfaust.dto.response.ProfileResponse;
import com.projectfaust.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public ProfileResponse toResponse(User user) {
        return new ProfileResponse(
                user.getId(),                // 1. UUID id
                user.getFullName(),          // 2. String fullName
                user.getEmail(),             // 3. String email (Tady ti v kódu chyběl!)
                user.getRole(),              // 4. String role
                user.getClearance(),         // 5. ClearanceLevel clearance
                user.getStatus(),            // 6. String status
                user.getTechStack(),         // 7. List<String> techStack
                user.isAdmin()               // 8. boolean isAdmin
        );
    }
}
