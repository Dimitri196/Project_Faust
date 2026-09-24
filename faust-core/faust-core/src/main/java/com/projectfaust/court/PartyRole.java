package com.projectfaust.court;

/**
 * Role of a party ({@link CourtRecordParty}) within a court proceeding.
 *
 * @author Dimitri / Project Faust
 */
public enum PartyRole {

    /** The accused in a criminal proceeding / respondent in civil. */
    DEFENDANT,

    /** The initiating party in a civil, administrative or commercial case. */
    PLAINTIFF,

    /** The prosecuting authority in a criminal proceeding. */
    PROSECUTOR,

    /** Party harmed by the offense (criminal proceedings). */
    VICTIM,

    /** Person called to testify. */
    WITNESS,

    /** Court-appointed or party-appointed expert. */
    EXPERT_WITNESS,

    /** Attorney, advocate, or legal proxy acting on behalf of a party. */
    LEGAL_REPRESENTATIVE,

    /** Court-appointed insolvency or liquidation administrator. */
    INSOLVENCY_ADMINISTRATOR,

    /** Third party with a legal interest in the outcome. */
    INTERVENER,

    /** Role not captured by the above values. */
    OTHER
}
