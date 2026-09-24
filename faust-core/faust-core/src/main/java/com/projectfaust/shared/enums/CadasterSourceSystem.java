package com.projectfaust.shared.enums;

/**
 * Identifies the country-specific cadaster registry a real estate
 * ownership record was ingested from.
 *
 * <p>Each value corresponds to one {@link com.projectfaust.ingest.realestate.core.RealEstateSourceMapper}
 * implementation. Adding a new country requires adding a value here
 * and creating the corresponding mapper + fetch service.</p>
 *
 * @author Dimitri / Project Faust
 */
public enum CadasterSourceSystem {

    /**
     * Czech Republic — ČÚZK Katastr nemovitostí (ISKN).
     * Remote Access API: paid, requires registration.
     * Status: STUB — not yet implemented.
     */
    CUZK_CZ,

    /**
     * Slovakia — Kataster nehnuteľností (ÚGKK SR).
     * Status: planned.
     */
    KATASTER_SK,

    /**
     * Poland — Elektroniczne Księgi Wieczyste (EKW).
     * Status: planned.
     */
    EKW_PL,

    /**
     * Austria — Grundbuch (BMJ).
     * Status: planned.
     */
    GRUNDBUCH_AT,

    /**
     * Germany — Grundbuch (varies by Bundesland).
     * Status: planned.
     */
    GRUNDBUCH_DE,

    /**
     * Generic/manual entry — for records entered manually by analysts
     * when no API source is available.
     */
    MANUAL
}