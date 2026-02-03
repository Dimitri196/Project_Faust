package com.projectfaust.entity.enums;

public enum OccupationCategory {
    /**
     * Top-level political roles (Ministers, Deputy Ministers/Náměstek,
     * Political Advisors). Usually tied to the election cycle.
     */
    POLITICAL,

    /**
     * Career civil servants under the State Service Act (Státní služba).
     * e.g., Sekční šéf, Odborný rada. These remain across governments.
     */
    CIVIL_SERVICE,

    /**
     * Standard employment (Zákoník práce) - specialized experts,
     * analysts, or specialized management not under the Service Act.
     */
    CONTRACTUAL,

    /**
     * Support staff, IT maintenance, logistics, and manual labor.
     */
    TECHNICAL,

    /**
     * Temporary members of advisory bodies, councils, or working groups.
     */
    ADVISORY
}
