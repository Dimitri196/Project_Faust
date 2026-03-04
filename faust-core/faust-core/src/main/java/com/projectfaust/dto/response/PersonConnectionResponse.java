package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.ConnectionType;

import java.time.LocalDate;
import java.util.UUID;

public record PersonConnectionResponse(
        UUID connectionId,
        UUID targetId,
        String targetFullName,
        ConnectionType type,
        Double influenceScore,
        String description,
        String targetCurrentPosition,
        LocalDate startDate,
        boolean isActive
) {}
