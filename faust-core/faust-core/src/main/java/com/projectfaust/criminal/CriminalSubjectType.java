package com.projectfaust.criminal;

/**
 * Discriminator indicating whether the subject of a criminal record is
 * a natural person or a legal entity (institution).
 *
 * <p>Corporate criminal liability exists in Czech, Slovak, Polish, Hungarian
 * and Austrian law, making institution-level records a real-world requirement.</p>
 *
 * @author Dimitri / Project Faust
 */
public enum CriminalSubjectType {

    /** Natural person — linked via {@link CriminalRecord#subjectPerson}. */
    PERSON,

    /** Legal entity — linked via {@link CriminalRecord#subjectInstitution}. */
    INSTITUTION
}
