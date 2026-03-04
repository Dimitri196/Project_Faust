package com.projectfaust.dto.request;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.LocationType;

import java.util.UUID;

public record LocationRequest(
        String name,
        LocationType type,
        String isoCode,
        UUID parentExternalId,
        ClearanceLevel clearance,
        Double latitude,
        Double longitude
) {}
