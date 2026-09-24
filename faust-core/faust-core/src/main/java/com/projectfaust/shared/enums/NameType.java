package com.projectfaust.shared.enums;

/**
 * Defines the category of a person's name or identity within the Faust system.
 * <p>
 * Used for tracking name history, maiden names, and operational aliases
 * for intelligence and security purposes.
 */
public enum NameType {

    /**
     * Current legal name as registered in official government records.
     */
    LEGAL,

    /**
     * Maiden name (surname at birth).
     * Critical for tracing family ties and security clearance background checks.
     */
    MAIDEN,

    /**
     * Operational cover name used by intelligence services (e.g., BIS, VZ).
     * Usually associated with higher classification levels.
     */
    ALIAS,

    /**
     * Artistic name or pseudonym (common for public figures).
     */
    PSEUDONYM,

    /**
     * Historical name that does not qualify as a maiden name (e.g., changed for personal reasons).
     */
    HISTORICAL,

    /**
     * Religious or monastic name.
     */
    RELIGIOUS
}
