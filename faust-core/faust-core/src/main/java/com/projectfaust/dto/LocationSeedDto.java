package com.projectfaust.dto;

import com.projectfaust.entity.enums.LocationType;
import java.util.List;

public record LocationSeedDto(
        String name,
        LocationType type,
        String isoCode,
        List<LocationSeedDto> children
) {}
