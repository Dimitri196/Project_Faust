package com.projectfaust.shared.enums;

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
     * Examples: Ministries, Intelligence Services, Supreme Courts.
     */
    NATIONAL(2, "Central government and state-wide institutions"),

    /**
     * Intermediate administration level representing larger territorial units.
     * Examples: Regions (Kraje), Provinces, or Voivodeships.
     */
    REGIONAL(3, "State, Regional, or Provincial administration"),

    /**
     * Governing bodies for specific cities or municipalities.
     * Examples: City Halls, Municipal Authorities.
     */
    LOCAL(4, "Municipal or City level administration"),

    /**
     * Smallest administrative units or local subdivisions.
     * Examples: District Councils, Neighborhood Committees.
     */
    SUB_LOCAL(5, "Districts, Boroughs, or Neighborhood councils");

    /**
     * Numeric rank used to enforce hierarchical order (1 = highest).
     */
    private final int rank;

    /**
     * Contextual description of the administrative scope.
     */
    private final String description;

    /**
     * Validates if this level is logically allowed to be a child of the parent level.
     * Prevents invalid nesting, such as placing a National Ministry under a Local Council.
     *
     * @param parentLevel The level of the potential parent institution.
     * @return true if the hierarchical relationship is logically sound.
     */
    public boolean isAllowedUnder(HierarchicalLevel parentLevel) {
        return this.rank >= parentLevel.getRank();
    }
}
