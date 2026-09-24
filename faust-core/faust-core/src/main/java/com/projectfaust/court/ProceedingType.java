package com.projectfaust.court;

/**
 * Type of court proceeding recorded in a {@link CourtRecord}.
 *
 * @author Dimitri / Project Faust
 */
public enum ProceedingType {

    /** Criminal prosecution — state vs. defendant. */
    CRIMINAL,

    /** Civil dispute between private parties. */
    CIVIL,

    /** Dispute between a private party and a public authority. */
    ADMINISTRATIVE,

    /** Corporate / commercial dispute (insolvency, contract, IP). */
    COMMERCIAL,

    /** Constitutional review of laws or fundamental rights. */
    CONSTITUTIONAL,

    /** Private arbitration or mediation proceeding. */
    ARBITRATION,

    /** Family law — divorce, custody, inheritance. */
    FAMILY,

    /** Proceeding type not covered by the above values. */
    OTHER
}
