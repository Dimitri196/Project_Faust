package com.projectfaust.traces.enums;

/**
 * Confidence level of a trace record — how certain the system or analyst is
 * that the observed event is correctly attributed to the registered subject.
 *
 * <p>Used for analyst triage: records below {@link #PROBABLE} should be flagged
 * for manual review before inclusion in a formal intelligence report.</p>
 *
 * @author Dimitri / Project Faust
 */
public enum TraceConfidence {

    /** Identity confirmed by biometric match, cryptographic proof, or official record. */
    CONFIRMED,

    /** High likelihood — multiple corroborating signals, no contradicting data. */
    PROBABLE,

    /** Plausible — one signal, no corroboration. Treat as lead, not evidence. */
    POSSIBLE,

    /** Unverified raw data — ingested but not reviewed. Default for automated ingest. */
    UNCONFIRMED,

    /** Attribution was explicitly rejected after review. Retained for audit. */
    REJECTED
}
