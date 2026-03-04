package com.projectfaust.dto.response;

import java.util.List;
import java.util.UUID;

public record ConnectionPathResponse(
        int degrees,
        List<String> pathNames,
        List<UUID> pathIds,
        boolean found
) {}
