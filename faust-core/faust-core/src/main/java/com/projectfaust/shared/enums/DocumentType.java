package com.projectfaust.shared.enums;

/**
 * Classification of identity and registration documents
 * tracked within the Project Faust person intelligence module.
 *
 * @author Dimitri / Project Faust
 */
public enum DocumentType {

    // ── Travel documents ─────────────────────────────────────────────────────

    /** Standard biometric passport. */
    PASSPORT,

    /** Diplomatic passport — indicates official state function. */
    PASSPORT_DIPLOMATIC,

    /** Service passport — military, intelligence, government officials. */
    PASSPORT_SERVICE,

    /** Emergency travel document issued for a single journey. */
    EMERGENCY_TRAVEL_DOCUMENT,

    /** Stateless person travel document (Convention Travel Document). */
    TRAVEL_DOCUMENT_STATELESS,

    // ── National identity documents ───────────────────────────────────────────

    /** National identity card (OP in CZ/SK, Personalausweis in DE). */
    NATIONAL_ID_CARD,

    /** Permanent residency card / long-term visa. */
    RESIDENCE_PERMIT,

    /** Refugee status document. */
    REFUGEE_DOCUMENT,

    // ── Driving / vehicle ────────────────────────────────────────────────────

    /** Civilian driver's license. */
    DRIVERS_LICENSE,

    /** Vehicle registration certificate. */
    VEHICLE_REGISTRATION,

    // ── Tax & business registration ───────────────────────────────────────────

    /**
     * Czech/Slovak company registration number (IČO).
     * Key for cross-referencing with Hlidač Státu contracts.
     */
    ICO,

    /**
     * Czech/Slovak VAT registration number (DIČ, format CZ+IČO).
     * Used for cross-border supplier identification.
     */
    DIC,

    /** National tax identification number (TIN). */
    TAX_ID,

    /** EU-wide unique identifier for legal entities (EUID). */
    EUID,

    /** Legal Entity Identifier — ISO 17442, 20-char global standard. */
    LEI,

    /** D-U-N-S number — Dun & Bradstreet global business identifier. */
    DUNS,

    // ── Social & state registries ─────────────────────────────────────────────

    /** National social security / insurance number. */
    SOCIAL_SECURITY_NUMBER,

    /** Health insurance card / number. */
    HEALTH_INSURANCE_ID,

    /** Pension / retirement fund identifier. */
    PENSION_ID,

    /** Birth certificate number. */
    BIRTH_CERTIFICATE,

    /** Military service booklet / conscription record. */
    MILITARY_BOOKLET,

    // ── Intelligence & security ───────────────────────────────────────────────

    /** Official security clearance certificate. */
    SECURITY_CLEARANCE_CERT,

    /** Press / media accreditation card. */
    PRESS_ACCREDITATION,

    /** Diplomatic accreditation card issued by MFA. */
    DIPLOMATIC_ACCREDITATION,

    // ── Other ────────────────────────────────────────────────────────────────

    /** Any document not covered by the above categories. */
    OTHER
}
