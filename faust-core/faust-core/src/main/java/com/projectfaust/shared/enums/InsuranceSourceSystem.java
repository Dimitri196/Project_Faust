package com.projectfaust.shared.enums;

/**
 * Identifies the origin system or registry from which an insurance record
 * was ingested into Project Faust.
 *
 * <p>Used together with a source-specific reference ID for idempotent
 * deduplication during repeated ingest cycles.</p>
 *
 * @author Dimitri / Project Faust
 * Notice: for next level app or version the table of InsuranceSourceSystem will be extended. It is a must!!!
 */
public enum InsuranceSourceSystem {

    /** Czech Insurance Association (Česká asociace pojišťoven) registry. */
    CAP_CZ,

    /** Slovak Insurance Association (Slovenská asociácia poisťovní) registry. */
    SAP_SK,

    /** Polish Insurance Association (Polska Izba Ubezpieczeń) registry. */
    PIU_PL,

    /** Hungarian Insurance Association (Magyar Biztosítók Szövetsége) registry. */
    MABISZ_HU,

    /** Austrian Insurance Association (Versicherungsverband Österreich) registry. */
    VVO_AT,

    /** Declaratory disclosure — subject self-reported in an official declaration. */
    DECLARATORY,

    /** Manual entry or analyst annotation within Project Faust. */
    MANUAL_ENTRY,

    /** OSINT — open-source intelligence (media, leaks, public filings). */
    OSINT,

    /** Cross-border data-sharing partner or allied-service feed. */
    PARTNER_FEED,

    /** Legacy import from the predecessor FAUST v1 data store. */
    LEGACY_IMPORT,

    /** Source system not yet classified. */
    UNKNOWN
}