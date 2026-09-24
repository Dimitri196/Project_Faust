package com.projectfaust.criminal;

/**
 * High-level classification of the criminal offense.
 *
 * <p>Coarse enough to enable cross-jurisdictional aggregation; detailed
 * offense codes from national registries are stored in
 * {@link CriminalRecord#statute} as free text.</p>
 *
 * @author Dimitri / Project Faust
 */
public enum OffenseCategory {

    /** Murder, assault, kidnapping, robbery with violence. */
    VIOLENT,

    /** Theft, burglary, vandalism, arson without injury. */
    PROPERTY,

    /** Fraud, embezzlement, money laundering, tax evasion, insider trading. */
    FINANCIAL,

    /** Drug possession, trafficking, production. */
    DRUG,

    /** Hacking, ransomware, identity theft, illegal interception. */
    CYBERCRIME,

    /** Offenses linked to organized criminal groups or syndicates. */
    ORGANIZED_CRIME,

    /** Terrorism, extremism, financing of terrorism. */
    TERRORISM,

    /** Bribery, corruption, abuse of public office. */
    CORRUPTION,

    /** Defamation, public disturbance, disorderly conduct. */
    PUBLIC_ORDER,

    /** DUI, dangerous driving, hit-and-run. */
    TRAFFIC,

    /** Human trafficking, exploitation, forced labor. */
    TRAFFICKING,

    /** War crimes, crimes against humanity, genocide. */
    WAR_CRIMES,

    /** Environmental offenses — illegal dumping, pollution. */
    ENVIRONMENTAL,

    /** Offense category not covered by the above values. */
    OTHER
}
