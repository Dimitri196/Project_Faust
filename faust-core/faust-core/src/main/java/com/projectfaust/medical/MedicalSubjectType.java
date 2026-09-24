package com.projectfaust.medical;

/**
 * Discriminator for the subject of a {@link MedicalRecord}.
 *
 * <p>Exactly one of {@code subjectPerson} / {@code subjectInstitution} must be
 * non-null when the discriminator value is set. Institutions represent entities
 * such as field hospitals, quarantine facilities, or care homes.</p>
 *
 * @author Dimitri / Project Faust
 */
public enum MedicalSubjectType {

    /** Natural person — the usual HUMINT subject. */
    PERSON,

    /** Legal entity — facility, hospital, or organisation. */
    INSTITUTION
}
