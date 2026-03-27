package com.projectfaust.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LocationType {

    CONTINENT(1, "Global Continent"),
    COUNTRY(2, "Sovereign State"),
    PROVINCE(3, "State, Province, or Region"),
    DISTRICT(4, "Administrative District"),
    CITY(5, "City or Municipality"),

    /**
     * Primary urban subdivision (e.g., Prague 12, Brno-střed, Boroughs).
     */
    SUBDIVISION_L1(6, "Primary Subdivision or Borough"),

    /**
     * Secondary urban subdivision (e.g., Modřany, Bory, Neighborhoods).
     */
    SUBDIVISION_L2(7, "Secondary Subdivision or Neighborhood"),

    /**
     * Increased granularity for buildings to allow L2 as a parent.
     */
    FACILITY(8, "Specific Building or Campus"),

    ZONE(9, "Internal Wing or Secure Zone"),
    SUBLOCATION(10, "Specific Room or Office");

    private final int granularity;
    private final String description;

    public boolean isValidChildOf(LocationType parentType) {
        return this.granularity > parentType.getGranularity();
    }
}
