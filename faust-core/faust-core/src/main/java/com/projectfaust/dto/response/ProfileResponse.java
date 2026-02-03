package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.ClearanceLevel;
import java.util.List;

public record ProfileResponse(
        String publicId,
        String fullName,
        String role,
        boolean isAdmin,
        ClearanceLevel clearance,
        List<String> techStack,
        String status
) {}
