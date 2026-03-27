package com.projectfaust.entity.filters;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.LocationType;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationFilter {
    private String query;
    private LocationType type;
    private Set<LocationType> types;

    private UUID parentId;

    private Boolean rootOnly;
    private String isoCode;
    private ClearanceLevel maxClearance;
    private Boolean active;

    private Double north;
    private Double south;
    private Double east;
    private Double west;

    @Builder.Default
    private boolean includeSubtree = false;

    private UUID countryId;
    private UUID provinceId;
}
