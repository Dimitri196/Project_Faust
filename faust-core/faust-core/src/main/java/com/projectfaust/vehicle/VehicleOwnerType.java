package com.projectfaust.vehicle;

/**
 * Discriminator for whether a vehicle owner or operator is a natural person
 * or a legal entity (institution).
 *
 * @author Dimitri / Project Faust
 */
public enum VehicleOwnerType {

    /** Natural person — linked via {@code ownerPerson} / {@code operatorPerson}. */
    PERSON,

    /** Legal entity — linked via {@code ownerInstitution} / {@code operatorInstitution}. */
    INSTITUTION
}
