package com.projectfaust.medical;

/**
 * Source system codes for medical intelligence within Project Faust.
 *
 * <p>Covers national health information systems (CE/DACH region),
 * forensic laboratories, international health bodies, and internal
 * intelligence-feed types.</p>
 *
 * @author Dimitri / Project Faust
 */
public enum MedicalSourceSystem {

    // ── National health information systems (CE/DACH) ─────────────────────────

    /** Ústav zdravotnických informací a statistiky ČR (Czech Republic). */
    UZIS_CZ,

    /** Národné centrum zdravotníckych informácií (Slovakia). */
    NCZI_SK,

    /** Centrum e-Zdrowia / CSIOZ (Poland). */
    CSIOZ_PL,

    /** Országos Kórházi Főigazgatóság (Hungary). */
    OKFO_HU,

    /** ELGA — Elektronische Gesundheitsakte (Austria). */
    ELGA_AT,

    // ── Forensic laboratories ─────────────────────────────────────────────────

    /** Czech national forensic laboratory. */
    FORENSIC_LAB_CZ,

    /** Slovak national forensic laboratory. */
    FORENSIC_LAB_SK,

    // ── International health & intelligence bodies ────────────────────────────

    /** World Health Organisation registry. */
    WHO,

    /** Europol medical / health intelligence. */
    EUROPOL_HEALTH,

    /** Interpol health / forensic data. */
    INTERPOL,

    // ── Internal intelligence feed types ─────────────────────────────────────

    /** Open-source intelligence. */
    OSINT,

    /** Declaratory source — subject self-reported or attorney-submitted. */
    DECLARATORY,

    MANUAL_ENTRY,
    PARTNER_FEED,
    LEGACY_IMPORT,
    UNKNOWN
}
