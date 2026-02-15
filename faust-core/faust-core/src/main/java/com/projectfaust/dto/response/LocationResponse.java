package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.LocationType;

import java.util.UUID;

public record LocationResponse(
        UUID externalId,
        String name,
        LocationType type,
        String isoCode,
        UUID parentExternalId,
        String parentName
) {}
