package com.projectfaust.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Arrays;

/**
 * Defines the security clearance required to access specific data points
 * within the personnel and institutional registry.
 */
@Getter
@RequiredArgsConstructor
public enum ClearanceLevel {

    /**
     * Access to public metadata only.
     * Entity names, institution titles, and basic public identifiers.
     */
    LEVEL_1_PUBLIC(1, "Public", "Basic institutional metadata and public names only"),

    /**
     * Access to organizational structures.
     * Ability to see hierarchy trees and current occupants of positions.
     */
    LEVEL_2_INTERNAL(2, "Internal", "Structural overview and current occupants of nodes"),

    /**
     * Access to restricted contact information.
     * Direct emails, phone numbers, and office locations.
     */
    LEVEL_3_CONFIDENTIAL(3, "Confidential", "Personal contact details and restricted attributes"),

    /**
     * Access to professional history and dossiers.
     * Full appointment history, detailed biographies, and internal systemic notes.
     */
    LEVEL_4_SECRET(4, "Secret", "Full career history, detailed biographies, and audit logs"),

    /**
     * Unrestricted access to sensitive financial and risk data.
     * Salaries, allowances, detailed benefits, and cross-entity risk relations.
     */
    LEVEL_5_TOP_SECRET(5, "Top Secret", "Unrestricted access including financial data and risk analysis");

    private final int weight;
    private final String label;
    private final String description;

    /**
     * Checks if this clearance level meets or exceeds the required level.
     *
     * @param required The level to compare against.
     * @return true if access should be granted.
     */
    public boolean canAccess(ClearanceLevel required) {
        return this.weight >= required.getWeight();
    }

    /**
     * Helper to resolve Level from an integer (e.g., from DB or JWT).
     */
    public static ClearanceLevel fromWeight(int weight) {
        return Arrays.stream(ClearanceLevel.values())
                .filter(l -> l.getWeight() == weight)
                .findFirst()
                .orElse(LEVEL_1_PUBLIC);
    }
}
