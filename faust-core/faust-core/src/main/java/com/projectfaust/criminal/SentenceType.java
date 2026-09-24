package com.projectfaust.criminal;

/**
 * Primary type of sentence imposed following conviction.
 *
 * <p>A single conviction may carry multiple sentence components
 * (e.g. imprisonment + fine). The entity stores the primary type;
 * secondary components are captured in {@link CriminalRecord#analyticalNote}
 * or a future {@code SentenceComponent} extension.</p>
 *
 * @author Dimitri / Project Faust
 */
public enum SentenceType {

    /** Custodial sentence — immediate incarceration. */
    IMPRISONMENT,

    /** Custodial sentence conditionally suspended (probation). */
    SUSPENDED,

    /** Monetary penalty. */
    FINE,

    /** Unpaid community work. */
    COMMUNITY_SERVICE,

    /** Supervised probation without incarceration. */
    PROBATION,

    /** Detention at registered address under electronic monitoring. */
    HOUSE_ARREST,

    /** Prohibition from entry or deportation order. */
    DEPORTATION,

    /** Forfeiture of assets or profits. */
    ASSET_FORFEITURE,

    /** Temporary or permanent ban from profession or public office. */
    DISQUALIFICATION,

    /** Combination of several sentence types (see analyticalNote). */
    COMBINED,

    /** Acquitted or charges dropped — no sentence imposed. */
    NONE,

    /** Sentence details unavailable from source. */
    UNKNOWN
}
