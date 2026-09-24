package com.projectfaust.shared.enums;


/**
 * Source system from which a document record was ingested.
 *
 * @author Dimitri / Project Faust
 */
public enum DocumentSourceSystem {

    // ── Manual entry ─────────────────────────────────────────────────────────
    /** Analyst manually entered document data. */
    MANUAL,

    /** Document scanned and data extracted via OCR. */
    SCAN_OCR,

    /** MRZ (Machine Readable Zone) parsed from travel document photo. */
    MRZ_PARSER,

    // ── Czech state registries ────────────────────────────────────────────────
    /** ARES — Administrativní registr ekonomických subjektů (CZ). */
    ARES_CZ,

    /** Katastr nemovitostí — Czech Land Registry. */
    KATASTER_CZ,

    /** MPSV — Ministry of Labour and Social Affairs (CZ). */
    MPSV_CZ,

    /** Czech Police / border control system. */
    POLICE_CZ,

    // ── Slovak state registries ───────────────────────────────────────────────
    /** ORSR — Obchodný register SR (SK). */
    ORSR_SK,

    /** Slovak Land Registry. */
    KATASTER_SK,

    // ── International ─────────────────────────────────────────────────────────
    /** GLEIF — Global LEI Foundation. */
    GLEIF,

    /** Interpol database cross-reference. */
    INTERPOL,

    /** EU sanctions registry. */
    EU_SANCTIONS,

    /** UN sanctions list. */
    UN_SANCTIONS,

    /** OFAC — US Treasury sanctions list. */
    OFAC,

    // ── OSINT ─────────────────────────────────────────────────────────────────
    /** Open source intelligence — web scraping, public registries. */
    OSINT,

    /** HUMINT source — field agent report. */
    HUMINT,

    /** SIGINT — signals intelligence intercept. */
    SIGINT
}
