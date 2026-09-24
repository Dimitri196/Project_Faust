package com.projectfaust.shared.enums;

/**
 * Billing frequency at which insurance premiums are collected.
 *
 * <p>Used to normalise premium amounts across policies when computing
 * annual insurance cost exposure for a subject's financial profile.</p>
 *
 * @author Dimitri / Project Faust
 */
public enum PremiumFrequency {

    /** Premium is collected once per week. */
    WEEKLY,

    /** Premium is collected once per calendar month. */
    MONTHLY,

    /** Premium is collected every two months. */
    BIMONTHLY,

    /** Premium is collected every three months (quarterly). */
    QUARTERLY,

    /** Premium is collected twice per year. */
    SEMIANNUALLY,

    /** Premium is collected once per year. */
    ANNUALLY,

    /** Single one-off premium; no recurring billing cycle. */
    SINGLE_PREMIUM,

    /** Billing frequency is not known or not disclosed. */
    UNKNOWN
}