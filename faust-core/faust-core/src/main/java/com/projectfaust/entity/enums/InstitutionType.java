package com.projectfaust.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Categorizes the nature of an institution based on its branch of power
 * or its functional role within the state and private sectors.
 */
@Getter
@RequiredArgsConstructor
public enum InstitutionType {

    /**
     * Government bodies responsible for daily administration.
     * Includes the Prime Minister's Office and all Ministries.
     */
    EXECUTIVE("EXEC", "Executive Branch"),

    /**
     * Law-making bodies of the state.
     * Includes the Chamber of Deputies and the Senate.
     */
    LEGISLATIVE("LEG", "Legislative Branch"),

    /**
     * The system of courts and legal authorities.
     * Includes the Supreme Court, Constitutional Court, and Prosecutors.
     */
    JUDICIAL("JUD", "Judicial Branch"),

    /**
     * Independent bodies overseeing specific sectors.
     * Includes the Central Bank (CNB) and Supreme Audit Office (NKU).
     */
    REGULATORY("REG", "Regulatory Body"),

    /**
     * Defense forces and military administration.
     * Includes the General Staff and specific military units.
     */
    MILITARY("MIL", "Military Forces"),

    /**
     * Secret services and counter-intelligence agencies.
     * Includes BIS (Security Information Service) and Military Intelligence.
     */
    INTELLIGENCE("INT", "Intelligence Community"),

    /**
     * Civil society organizations and non-profits.
     * Includes foundations, charities, and advocacy groups.
     */
    NGO("NGO", "Non-Governmental Organization"),

    /**
     * Entities operating within the commercial sphere.
     * Includes state-owned enterprises and private sector partners.
     */
    PRIVATE("PRIV", "Private Sector");

    private final String categoryCode;
    private final String displayName;
}
