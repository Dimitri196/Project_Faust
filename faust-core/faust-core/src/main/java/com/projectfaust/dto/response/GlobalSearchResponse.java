package com.projectfaust.dto.response;

import java.util.UUID;

public record GlobalSearchResponse(
        UUID id,
        String displayName,
        String category,
        String subLabel,
        Double rankScore
) {}
