package com.projectfaust.dto.request;

import com.projectfaust.entity.enums.ClearanceLevel;

public record AgentOnboardingRequest(
        String codename, // Místo fullName pro utajení
        String officialEmail,
        ClearanceLevel assignedLevel,
        boolean requiresFieldAccess
) {}
