package com.projectfaust.medical;

/**
 * Broad clinical category for a {@link MedicalRecord} of type
 * {@link MedicalRecordType#CLINICAL_PROFILE}, {@link MedicalRecordType#PSYCHIATRIC_ASSESSMENT},
 * or {@link MedicalRecordType#DISABILITY_ASSESSMENT}.
 *
 * @author Dimitri / Project Faust
 */
public enum MedicalConditionCategory {

    PHYSICAL,
    PSYCHIATRIC,
    NEUROLOGICAL,
    SUBSTANCE_ABUSE,
    INFECTIOUS,
    CHRONIC,
    TRAUMA,
    ONCOLOGICAL,
    GENETIC,
    CONGENITAL,
    OCCUPATIONAL,
    OTHER
}
