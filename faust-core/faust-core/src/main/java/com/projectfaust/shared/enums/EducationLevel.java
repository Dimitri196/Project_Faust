package com.projectfaust.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Standardized levels of education attained by personnel.
 * Includes weights for analytical sorting and labels for professional display.
 */
@Getter
@RequiredArgsConstructor
public enum EducationLevel {

    /**
     * Secondary education with a school-leaving examination (e.g., High School Diploma).
     */
    SECONDARY(1, "Secondary", "HS"),

    /**
     * Post-secondary non-university professional education (e.g., Associate degree, DiS.).
     */
    HIGHER_VOCATIONAL(2, "Higher Vocational", "HVE"),

    /**
     * Undergraduate university degree (e.g., Bc., BcA., B.A., B.Sc.).
     */
    BACHELOR(3, "Bachelor", "Bachelors"),

    /**
     * Graduate university degree (e.g., Mgr., Ing., M.A., M.Sc.)
     * including medical and law professional doctorates (MUDr., JUDr., PhDr.).
     */
    MASTER(4, "Master", "Masters"),

    /**
     * Postgraduate research degree (e.g., Ph.D.,  Th.D., CSc., D.Sc.).
     */
    DOCTORATE(5, "Doctorate", "PhD");

    /**
     * Numeric weight for sorting or filtering by academic seniority.
     */
    private final int weight;

    /**
     * Full human-readable name for UI display.
     */
    private final String displayName;

    /**
     * Short identifier for compact UI badges or reporting.
     */
    private final String shortLabel;

    /**
     * Returns true if this education level is equal to or higher than the target.
     * Useful for verifying qualification requirements for specific Nodes or Roles.
     */
    public boolean satisfies(EducationLevel required) {
        return this.weight >= required.getWeight();
    }
}
