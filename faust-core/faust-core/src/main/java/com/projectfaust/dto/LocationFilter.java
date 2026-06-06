package com.projectfaust.dto;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.LocationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationFilter {
    private String query;
    private LocationType type;
    private UUID parentId;
    private Boolean rootOnly;
    private Double north, south, east, west;
    private ClearanceLevel maxClearance;
}