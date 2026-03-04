package com.projectfaust.dto.response;

import java.util.List;
import java.util.UUID;

public record InfluenceMapResponse(
        UUID rootId,
        String rootFullName,
        String rootClearance,
        List<PersonConnectionResponse> connections,
        int totalConnections
) {}
