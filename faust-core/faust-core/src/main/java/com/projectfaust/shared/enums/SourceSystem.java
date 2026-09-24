package com.projectfaust.shared.enums;

import com.projectfaust.ingest.core.ContractSourceMapper;

/**
 * Identifies the external data source a record was ingested from.
 *
 * <p>Each value corresponds to one implementation of
 * {@link ContractSourceMapper}. New countries or
 * systems are added here and given a corresponding mapper — the rest of
 * the ingest pipeline (deduplication, persistence, linking) is source-agnostic.</p>
 *
 * @author Dimitri / Project Faust
 */
public enum SourceSystem {

    /**
     * Hlidac Statu (CZ) — Czech civic initiative aggregating public contracts.
     * Trust level: UNVERIFIED — civic source, not an official state register.
     * API: <a href="https://www.hlidacstatu.cz">...</a>
     */
    HLIDAC_STATU_CZ,

    /**
     * TED (Tenders Electronic Daily) — the EU's official public procurement
     * journal, published by the Publications Office of the EU.
     * Covers all EU member state contracts above EU threshold values.
     * Trust level: OFFICIAL_REGISTRY — highest in the current pipeline.
     * API: <a href="https://api.ted.europa.eu/v3/notices/search">...</a>
     */
    TED_EU,

    /**
     * Theses.cz (CZ) — Czech academic thesis registry.
     * Reserved for future ingestion of academic records.
     */
    THESES_CZ
}