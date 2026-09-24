package com.projectfaust.medical;

/**
 * Discriminates between clinical HUMINT profile data and forensic/incident records.
 *
 * <p>All types share a single entity — the active type determines which field
 * clusters are populated. See {@link MedicalRecord} for the full field layout.</p>
 *
 * @author Dimitri / Project Faust
 */
public enum MedicalRecordType {

    /** Full clinical HUMINT profile: conditions, diagnoses, treatment history. */
    CLINICAL_PROFILE,

    /** Post-incident or post-mortem forensic physical examination. */
    FORENSIC_EXAMINATION,

    /** Toxicology screening — substances detected, dosage, metabolites. */
    TOXICOLOGY_REPORT,

    /** Psychiatric / psychological evaluation. */
    PSYCHIATRIC_ASSESSMENT,

    /** Security clearance or employment fitness-for-duty assessment. */
    FITNESS_FOR_DUTY,

    /** Statutory disability rating (0–100 %). */
    DISABILITY_ASSESSMENT,

    /** Official cause-of-death declaration (autopsy or clinical). */
    CAUSE_OF_DEATH,

    /** Field injury report — sustained in an incident / operation. */
    INCIDENT_INJURY_REPORT,

    /** Prescription or chronic medication record. */
    PRESCRIPTION_RECORD,

    OTHER
}
