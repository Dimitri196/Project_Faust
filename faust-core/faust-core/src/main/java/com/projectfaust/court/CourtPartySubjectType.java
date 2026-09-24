package com.projectfaust.court;

/**
 * Discriminator for whether a {@link CourtRecordParty} is a natural person
 * or a legal entity (institution).
 *
 * @author Dimitri / Project Faust
 */
public enum CourtPartySubjectType {

    /** Natural person — linked via {@link CourtRecordParty#person}. */
    PERSON,

    /** Legal entity — linked via {@link CourtRecordParty#institution}. */
    INSTITUTION
}
