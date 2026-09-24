package com.projectfaust.vehicle;

/**
 * EU vehicle category classification (Directive 2007/46/EC and derivatives).
 *
 * <p>Used as the primary axis for filtering and analytics in the FAUST
 * vehicle intelligence layer.</p>
 *
 * @author Dimitri / Project Faust
 */
public enum VehicleCategory {

    // ── Passenger vehicles ────────────────────────────────────────────────────

    /** M1 — passenger car, up to 8 seats excluding driver. */
    PASSENGER_CAR,

    /** M2 — minibus, 9+ seats, ≤ 5 t. */
    MINIBUS,

    /** M3 — bus / coach, 9+ seats, > 5 t. */
    BUS,

    // ── Goods vehicles ────────────────────────────────────────────────────────

    /** N1 — light goods vehicle / van, ≤ 3.5 t. */
    LIGHT_GOODS,

    /** N2 — medium goods vehicle, 3.5–12 t. */
    MEDIUM_GOODS,

    /** N3 — heavy goods vehicle / truck, > 12 t. */
    HEAVY_GOODS,

    // ── Special purpose ───────────────────────────────────────────────────────

    /** Tractor, agricultural or industrial. */
    TRACTOR,

    /** Trailer or semi-trailer. */
    TRAILER,

    /** Mobile home / motorhome (M1 derived). */
    MOTORHOME,

    /** Ambulance, fire engine, police vehicle. */
    EMERGENCY,

    /** Armoured or security-escort vehicle. */
    ARMOURED,

    // ── Two- and three-wheelers ───────────────────────────────────────────────

    /** L1/L3 — motorcycle or moped. */
    MOTORCYCLE,

    /** L5 — tricycle (motorised). */
    TRICYCLE,

    /** ATV / quad bike. */
    ATV,

    // ── Watercraft / aircraft (cross-modal HUMINT) ────────────────────────────

    /** Registered watercraft (yacht, motorboat, jet-ski). */
    WATERCRAFT,

    /** Registered aircraft (private plane, helicopter, ultralight). */
    AIRCRAFT,

    /** Other / not yet classified. */
    OTHER
}
