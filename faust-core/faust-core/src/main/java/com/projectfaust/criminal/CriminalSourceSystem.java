package com.projectfaust.criminal;

/**
 * Source systems from which criminal record data is ingested.
 *
 * @author Dimitri / Project Faust
 */
public enum CriminalSourceSystem {

    /** Czech criminal registry (Rejstřík trestů ČR). */
    RT_CZ,

    /** Slovak criminal registry (Register trestov SR). */
    RTPO_SK,

    /** Polish National Criminal Register (Krajowy Rejestr Karny). */
    KRK_PL,

    /** Hungarian criminal registry (Bűnügyi nyilvántartás). */
    BUGYI_HU,

    /** Austrian criminal registry (Strafregister). */
    SREG_AT,

    /** Europol information system / SIENA intelligence exchange. */
    EUROPOL,

    /** Interpol notices and diffusions (Red, Blue, Yellow…). */
    INTERPOL,

    /** Schengen Information System — criminal alert entries. */
    SIS,

    /** EU Agency for Law Enforcement Cooperation — ETIAS / PNR. */
    EUROJUST,

    /** Declaratory ingest — subject self-declared or disclosed. */
    DECLARATORY,

    /** Open-source intelligence. */
    OSINT,

    /** Manual entry by a FAUST analyst. */
    MANUAL_ENTRY,

    /** Partner feed from a trusted intelligence-sharing agreement. */
    PARTNER_FEED,

    /** Historical import from a legacy system. */
    LEGACY_IMPORT,

    /** Source not recorded. */
    UNKNOWN
}
