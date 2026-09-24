package com.projectfaust.shared.enums;

/**
 * Classification of insurance policy types tracked within Project Faust.
 *
 * <p>Used to categorise insurance records ingested from official registries
 * and declaratory sources. Provides the primary analytical dimension for
 * assessing a subject's financial exposure and liability profile.</p>
 *
 * @author Dimitri / Project Faust
 */
public enum InsuranceType {

    /** Standard health insurance covering medical treatment and hospitalisation. */
    HEALTH,

    /** Life insurance — term, whole-life, or endowment policies. */
    LIFE,

    /** Dental coverage (may be bundled with HEALTH or standalone). */
    DENTAL,

    /** Vision coverage (may be bundled with HEALTH or standalone). */
    VISION,

    /** Motor vehicle third-party liability (povinné ručení in CZ). */
    VEHICLE_LIABILITY,

    /** Comprehensive motor vehicle coverage (havarijní pojištění in CZ). */
    VEHICLE_COMPREHENSIVE,

    /** Real estate property insurance — buildings and contents. */
    PROPERTY,

    /** General liability insurance for individuals or legal entities. */
    LIABILITY,

    /** Travel insurance covering medical emergencies and trip cancellations. */
    TRAVEL,

    /** Directors and officers liability (D&O). */
    DIRECTORS_AND_OFFICERS,

    /** Professional indemnity / errors & omissions. */
    PROFESSIONAL_INDEMNITY,

    /** Business interruption insurance. */
    BUSINESS_INTERRUPTION,

    /** Cyber risk and data breach insurance. */
    CYBER,

    /** Agricultural and livestock insurance. */
    AGRICULTURAL,

    /** Marine and cargo insurance. */
    MARINE,

    /** Aviation hull and liability insurance. */
    AVIATION,

    /** Keyman / key-person insurance on a named executive. */
    KEY_PERSON,

    /** Credit insurance protecting against debtor default. */
    CREDIT,

    /** Pension or retirement annuity product. */
    PENSION,

    /** Catch-all for policy types not yet modelled explicitly. */
    OTHER
}