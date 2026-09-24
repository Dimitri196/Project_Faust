package com.projectfaust.mobility;

/**
 * Source system codes for mobility intelligence within Project Faust.
 *
 * <p>Covers national road / border / parking systems (CE/DACH),
 * Schengen / EU border management systems, toll operators,
 * and internal intelligence feed types.</p>
 *
 * @author Dimitri / Project Faust
 */
public enum MobilitySourceSystem {

    // ── National road / traffic police (CE/DACH) ──────────────────────────────

    /** Czech traffic offences registry (Centrum dopravních přestupků). */
    DOPRAVNI_PRESTUPKY_CZ,

    /** Slovak traffic police registry. */
    POLICIA_SK_DOPRAVA,

    /** Polish Road Transport Inspectorate (GITD). */
    GITD_PL,

    /** Hungarian traffic police. */
    POLICE_HU_TRAFFIC,

    /** Austrian Federal Ministry of the Interior — road traffic. */
    BMI_AT_TRAFFIC,

    // ── National border / customs ─────────────────────────────────────────────

    /** Czech border police (Cizinecká policie). */
    BORDER_CZ,

    /** Slovak border police. */
    BORDER_SK,

    /** Polish Border Guard (Straż Graniczna). */
    BORDER_PL,

    /** Hungarian National Directorate-General for Aliens Policing. */
    BORDER_HU,

    /** Austrian border police / BVT. */
    BORDER_AT,

    // ── EU / Schengen border management systems ───────────────────────────────

    /** Schengen Information System (SIS II / SIS III). */
    SIS,

    /** EU Entry/Exit System (EES). */
    EES,

    /** European Travel Information and Authorisation System. */
    ETIAS,

    /** Visa Information System (VIS). */
    VIS,

    /** Interpol travel intelligence (stolen travel documents, TDAWN). */
    INTERPOL_TRAVEL,

    // ── Toll / e-vignette operators ───────────────────────────────────────────

    /** Czech motorway toll (Mytocz / CzechToll). */
    MYTO_CZ,

    /** Slovak electronic toll system (eMYTO). */
    EMYTO_SK,

    /** Austrian motorway toll (ASFINAG). */
    ASFINAG_AT,

    // ── Surveillance / ANPR ───────────────────────────────────────────────────

    /** Automatic Number Plate Recognition feed. */
    ANPR,

    /** CCTV / smart-city camera network. */
    CCTV,

    // ── Internal intelligence feed types ─────────────────────────────────────

    OSINT,
    MANUAL_ENTRY,
    PARTNER_FEED,
    LEGACY_IMPORT,
    UNKNOWN
}
