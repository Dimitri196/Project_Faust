package com.projectfaust.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines the levels of geographical and organizational granularity
 * for the localization of assets, institutions, and subjects.
 */
@Getter
@RequiredArgsConstructor
public enum LocationType {

    /**
     * Large landmasses or global regions (e.g., Europe, North America).
     */
    CONTINENT(1, "Global Continent"),

    /**
     * Sovereign states or nations (e.g., Czechia, USA). Usually follows ISO 3166-1.
     */
    COUNTRY(2, "Sovereign State"),

    /**
     * Major administrative subdivisions such as Regions, Provinces, or Federal States.
     * Examples: Středočeský kraj, Texas, Bavaria.
     */
    PROVINCE(3, "State, Province, or Region"),

    /**
     * Intermediate administrative areas like Counties or Sectors.
     */
    DISTRICT(4, "Administrative District"),

    /**
     * Municipal level, representing cities, towns, or statutory municipalities.
     */
    CITY(5, "City or Municipality"),

    /**
     * Specific urban subdivisions, neighborhoods, or city boroughs.
     * Examples: Prague 6, Manhattan, Mitte.
     */
    SUBDIVISION(6, "City Subdivision or Neighborhood"),

    /**
     * A specific building, campus, or facility complex.
     * Examples: BIS Headquarters, The Pentagon, Strakova Akademie.
     */
    FACILITY(7, "Specific Building or Campus"),

    /**
     * A designated area within a facility, such as a wing, floor, or secure section.
     */
    ZONE(8, "Internal Wing or Zone"),

    /**
     * Terminal level representing a specific room, office, lab, or server rack.
     */
    SUBLOCATION(9, "Specific Room or Office");

    private final int granularity;
    private final String description;

    /**
     * Validates if a parent location is geographically broader than the child.
     * * @param parentType The LocationType of the parent node.
     * @return true if the hierarchical relationship is logically valid.
     */
    public boolean isValidChildOf(LocationType parentType) {
        return this.granularity > parentType.getGranularity();
    }
}
