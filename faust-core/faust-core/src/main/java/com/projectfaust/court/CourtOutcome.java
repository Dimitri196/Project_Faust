package com.projectfaust.court;

/**
 * Outcome of a concluded court proceeding.
 *
 * @author Dimitri / Project Faust
 */
public enum CourtOutcome {

    /** Defendant found guilty / claim upheld. */
    CONVICTION_UPHELD,

    /** Defendant found not guilty / claim dismissed. */
    ACQUITTAL,

    /** Parties reached an out-of-court or court-approved settlement. */
    SETTLEMENT,

    /** Case dismissed for procedural or jurisdictional reasons. */
    DISMISSED,

    /** Judgment in favour of the plaintiff / claimant. */
    JUDGMENT_FOR_PLAINTIFF,

    /** Judgment in favour of the defendant / respondent. */
    JUDGMENT_FOR_DEFENDANT,

    /** Lower-court ruling upheld on appeal. */
    APPEAL_UPHELD,

    /** Lower-court ruling overturned on appeal. */
    APPEAL_OVERTURNED,

    /** Partial success for both sides. */
    PARTIAL,

    /** Proceeding still ongoing — no final judgment yet. */
    ONGOING,

    /** Proceeding suspended (e.g. subject died, fugitive). */
    SUSPENDED,

    /** Outcome unknown or not yet recorded. */
    UNKNOWN
}
