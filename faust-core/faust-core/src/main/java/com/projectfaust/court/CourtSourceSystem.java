package com.projectfaust.court;

/**
 * Source systems from which court record data is ingested.
 *
 * @author Dimitri / Project Faust
 */
public enum CourtSourceSystem {

    /** Czech InfoSoud / justice.cz public court register. */
    INFOSOUD_CZ,

    /** Slovak court register (susr.sk / orsr.sk proceedings). */
    COURT_REG_SK,

    /** Polish National Court Register / portal orzeczeń. */
    NCR_PL,

    /** Hungarian court register (birosag.hu). */
    COURT_REG_HU,

    /** Austrian court register (ris.bka.gv.at). */
    COURT_REG_AT,

    /** European Court of Human Rights (ECHR) HUDOC database. */
    ECHR,

    /** Court of Justice of the European Union (CJEU). */
    CJEU,

    /** International Criminal Court (ICC). */
    ICC,

    /** Interpol — case referrals. */
    INTERPOL,

    /** Eurojust — cross-border coordination. */
    EUROJUST,

    /** OSINT sources (legal news, judgment databases). */
    OSINT,

    /** Declaratory — subject self-disclosed or obtained via FOI. */
    DECLARATORY,

    /** Manual entry by a FAUST analyst. */
    MANUAL_ENTRY,

    /** Partner feed from a trusted intelligence-sharing agreement. */
    PARTNER_FEED,

    /** Historical import from a legacy system. */
    LEGACY_IMPORT,

    /** Source not recorded. */
    UNKNOWN
}
