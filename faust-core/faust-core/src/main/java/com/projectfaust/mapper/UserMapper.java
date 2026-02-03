package com.projectfaust.mapper;

import com.projectfaust.dto.response.ProfileResponse;
import com.projectfaust.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public ProfileResponse toResponse(User user) {
        // Tady máš plnou kontrolu nad transformací dat z tvého CV [cite: 4, 7]
        return new ProfileResponse(
                user.getId().toString(),       // UUID -> String [cite: 17]
                user.getFullName(),            // "Dimitri Bodzewicz" [cite: 1]
                user.getRole(),                // "Software Engineer" [cite: 4]
                user.isAdmin(),                // Tvoje Security logika [cite: 14]
                user.getClearance(),           // ClearanceLevel [cite: 16]
                user.getTechStack(),           // List technologií (Java, React, atd.) [cite: 9, 12]
                user.getStatus()               // "OPERATIONAL" [cite: 16]
        );
    }
}