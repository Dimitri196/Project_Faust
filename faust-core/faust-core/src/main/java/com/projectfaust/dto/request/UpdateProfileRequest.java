package com.projectfaust.dto.request;

import com.projectfaust.entity.enums.ClearanceLevel;

import java.util.List;

public record UpdateProfileRequest(
        String fullName,
        String role,
        ClearanceLevel clearance,
        String status,
        List<String> techStack
) {}
