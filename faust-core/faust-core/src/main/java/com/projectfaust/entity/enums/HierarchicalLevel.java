package com.projectfaust.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines the administrative or territorial scope of an institution.
 * Used to enforce hierarchical integrity and categorize systemic nodes.
 */
@Getter
@RequiredArgsConstructor
public enum HierarchicalLevel {

    /**
     * Supranational entities and organizations with global or continental reach.
     * Examples: UN, EU, NATO, Interpol.
     */
    INTERNATIONAL(1, "Supranational bodies like EU or UN"),

    /**
     * Central government authorities and state-wide institutions.
     * Examples: Ministries, Intelligence Services (BIS), Supreme Courts.
     */
    NATIONAL(2, "Central government level"),

    /**
     * Intermediate administration level representing larger territorial units.
     * Examples: Regions (Kraje), Provinces, or Voivodeships.
     */
    REGIONAL(3, "State, Province, or Voivodeship"),

    /**
     * Governing bodies for specific cities or municipalities.
     * Examples: City Halls, Municipal Authorities.
     */
    LOCAL(4, "Municipal or City level"),

    /**
     * Smallest administrative units or local subdivisions.
     * Examples: District Councils, Neighborhood Committees.
     */
    SUB_LOCAL(5, "Districts or Neighborhood councils");

    private final int rank;
    private final String description;

    /**
     * Validates if this level is logically allowed to be a child of the parent level.
     * Prevents cases like a National Ministry being placed under a Local Council.
     *
     * @param parentLevel The level of the potential parent institution.
     * @return true if the hierarchical relationship is logically sound.
     */
    public boolean isAllowedUnder(HierarchicalLevel parentLevel) {
        return this.rank >= parentLevel.getRank();
    }
}
