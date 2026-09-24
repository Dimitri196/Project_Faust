package com.projectfaust.court;

/**
 * Hierarchical level of the court in which a proceeding took place.
 *
 * @author Dimitri / Project Faust
 */
public enum CourtLevel {

    /** Lowest-tier court (Czech: Okresní, Slovak: Okresný, Polish: Rejonowy). */
    DISTRICT,

    /** Mid-tier court — first-instance for serious matters and appeal court for district. */
    REGIONAL,

    /** Second-level appeal court. */
    HIGH,

    /** Highest ordinary court. */
    SUPREME,

    /** Constitutional court / tribunal. */
    CONSTITUTIONAL,

    /** Specialised court (administrative, military, anti-corruption). */
    SPECIALISED,

    /** Supranational — ECHR, ECJ, ICC, etc. */
    INTERNATIONAL,

    /** Level not recorded or not applicable (e.g. arbitration). */
    OTHER
}
