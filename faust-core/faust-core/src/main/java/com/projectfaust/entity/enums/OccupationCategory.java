package com.projectfaust.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines the functional nature of an occupation across both public and private sectors.
 * This categorization allows for cross-sector analysis of power and responsibility.
 */
@Getter
@RequiredArgsConstructor
public enum OccupationCategory {

    /**
     * Strategic oversight, ownership, or legislative mandate.
     * Public: Members of Parliament, Senators, Councilors.
     * Private: Shareholders, Board Members, Owners.
     */
    GOVERNANCE(5, "Governance & Mandate"),

    /**
     * Active leadership, administration, and executive decision-making.
     * Public: Ministers, Prime Ministers, Mayors, Directors of State Agencies.
     * Private: CEOs, CTOs, Managing Directors.
     */
    EXECUTIVE(4, "Executive Leadership"),

    /**
     * Subject matter experts and career professionals.
     * Public: Civil Servants, Diplomats, Legal Experts, Senior Analysts.
     * Private: Senior Engineers, Architects, Consultants.
     */
    SPECIALIST(3, "Professional Specialist"),

    /**
     * Core operational execution and service delivery.
     * Public: Police Officers, Teachers, Clerks, Technicians.
     * Private: Sales Staff, Operations Managers, Service Workers.
     */
    OPERATIONAL(2, "Operations & Service"),

    /**
     * Support functions, maintenance, and logistics.
     */
    TECHNICAL(1, "Technical & Support");

    private final int authorityRank;
    private final String displayName;
}
