package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.ClearanceLevel;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String fullName,
        String email,
        String role,
        ClearanceLevel clearance,
        String status,
        List<String> techStack,
        boolean isAdmin
) {}
