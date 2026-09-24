package com.projectfaust.shared.enums;

/**
 * Classifies the evidentiary provenance and verification state of an intelligence data item.
 * <p>
 * This classification follows advanced counter-intelligence evaluation standards, mapping
 * the reliability of the ingestion vector against operational counter-surveillance risks.
 * It determines how the presentation layer (SPA) visualizes connection certainty and guides
 * automated link analysis algorithms.
 * </p>
 *
 * @author Dimitri
 */
public enum VerificationStatus {

    /**
     * Data retrieved directly from an official, verified state or commercial registry
     * (e.g., Land Registry, Central Bank Ledger, corporate business register).
     * Administrative forgery is low; legal trace is absolute.
     */
    OFFICIAL_REGISTRY,

    /**
     * Obtained via direct tactical operations or technical SIGINT intercept
     * (e.g., active cellular triangulation, lawful intercept, wiretapping, or device forensic dump).
     * Highly accurate structural data.
     */
    TECHNICAL_INTERCEPT,

    /**
     * Sourced from vetted, verified Human Intelligence (HUMINT)—such as a compromised internal asset,
     * field operator report, or reliable informant. High tactical value, but requires ongoing validation.
     */
    VETTED_HUMINT,

    /**
     * Extracted from verified Open-Source Intelligence (OSINT) channels, public court records,
     * or leaked structural archives. The source is authenticated, though the target may have
     * abandoned the channel.
     */
    VERIFIED_OSINT,

    /**
     * Information received through an unvetted source, anonymous tip-off, or raw, unanalyzed
     * field telemetry. Highly speculative; highlighted in the SPA with high visual transparency
     * to prevent analytical bias.
     */
    UNVERIFIED,

    /**
     * Explicitly identified as a deceptive vector intentionally planted by the target or an
     * adversary service (Counter-Intelligence Countermeasure).
     * <p>
     * <b>Operational Use:</b> Kept active in the database to trace who else interacts with this
     * honeypot or disinformation channel, serving as a critical trap to catch "Faust."
     * </p>
     */
    DECEPTION_MARKER,

    /**
     * Confirmed by analysts to be completely obsolete, burned, or no longer tied to the target
     * (e.g., a phone number recycled by a telecom operator and now belonging to an unrelated civilian).
     */
    EXPIRED_DEPRECATING,

    /**
     * Two or more independent sources provide directly contradictory data.
     * Analyst review required before this record influences link analysis.
     * Displayed in the SPA with a conflict indicator.
     */
    CONFLICTING,

    /**
     * Data has been ingested (via Kafka, scraper, or manual entry) but has not yet
     * been reviewed and promoted by a cleared analyst.
     * Default state for all automated ingest pipelines.
     */
    PENDING_REVIEW
}