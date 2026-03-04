package com.projectfaust.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines the functional nature of an occupation across both public and private sectors.
 * This categorization enables cross-sector analysis of power, responsibility, and influence.
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
     * Public: Ministers, Prime Ministers, Mayors, Agency Directors.
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
     * Essential support functions, maintenance, and logistics.
     */
    TECHNICAL(1, "Technical & Support");

    /**
     * Numeric rank used to measure the level of influence or authority (5 = highest).
     */
    private final int authorityRank;

    /**
     * Human-readable label for UI display.
     */
    private final String displayName;
}
