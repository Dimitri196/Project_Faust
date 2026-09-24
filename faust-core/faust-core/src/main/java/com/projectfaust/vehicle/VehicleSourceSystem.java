package com.projectfaust.vehicle;

/**
 * Origin system from which a vehicle record was ingested into Project Faust.
 *
 * <p>Used together with {@code sourceReferenceId} for idempotent
 * deduplication across repeated ingest cycles.</p>
 *
 * @author Dimitri / Project Faust
 */
public enum VehicleSourceSystem {

    /** Czech central vehicle registry (Centrální registr vozidel, MDČr). */
    CRV_CZ,

    /** Slovak vehicle registry (Evidencia vozidiel, MDV SR). */
    EV_SK,

    /** Polish vehicle registry (Centralna Ewidencja Pojazdów, CEPiK). */
    CEP_PL,

    /** Hungarian vehicle registry (Közlekedési Nyilvántartó Hatóság). */
    KNH_HU,

    /** Austrian vehicle registry (Kraftfahrzeugzentralregister, KZR). */
    KZR_AT,

    /** EU cross-border EUCARIS vehicle data exchange. */
    EUCARIS,

    /** Aircraft registry (e.g. CAA, EASA). */
    AIRCRAFT_REGISTRY,

    /** Maritime / watercraft registry. */
    MARITIME_REGISTRY,

    /** Declaratory disclosure — subject self-reported. */
    DECLARATORY,

    /** OSINT — open-source intelligence (media, leaks, public filings). */
    OSINT,

    /** Manual entry or analyst annotation within Project Faust. */
    MANUAL_ENTRY,

    /** Cross-border data-sharing partner or allied-service feed. */
    PARTNER_FEED,

    /** Legacy import from the predecessor FAUST v1 data store. */
    LEGACY_IMPORT,

    UNKNOWN
}
