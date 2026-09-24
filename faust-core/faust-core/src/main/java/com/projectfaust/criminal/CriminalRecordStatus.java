package com.projectfaust.criminal;

/**
 * Lifecycle status of a criminal record entry.
 *
 * @author Dimitri / Project Faust
 */
public enum CriminalRecordStatus {

    /** Final conviction handed down and not overturned. */
    CONVICTION,

    /** Acquitted at trial or on appeal. */
    ACQUITTAL,

    /** Charges dropped or prosecution discontinued. */
    CASE_DROPPED,

    /** Sentence suspended pending good behaviour. */
    SUSPENDED_SENTENCE,

    /** Subject released on parole / conditions. */
    PAROLE,

    /** Pardoned by head of state or competent authority. */
    PARDONED,

    /** Record legally expunged or rehabilitated. */
    EXPUNGED,

    /** Proceedings ongoing — indictment not yet concluded. */
    PENDING,

    /** Warrant issued; subject not yet apprehended. */
    WANTED,

    /** Status cannot be confirmed from available sources. */
    UNKNOWN
}
