package com.projectfaust.mobility;

/**
 * Type of mobility intelligence event recorded in Project Faust.
 *
 * <p>A single {@link MobilityEvent} entity covers all types; the active type
 * drives which field clusters are relevant (border cluster, violation cluster,
 * surveillance cluster).</p>
 *
 * @author Dimitri / Project Faust
 */
public enum MobilityEventType {

    /** Entry or exit at a land / sea / air border crossing point. */
    BORDER_CROSSING,

    /** Parking violation — ticket issued, vehicle clamped or towed. */
    PARKING_VIOLATION,

    /** Legitimate parking record (location intelligence, no offence). */
    PARKING_RECORD,

    /** Moving traffic violation: speeding, red light, lane offence, etc. */
    TRAFFIC_VIOLATION,

    /** Toll-gantry passage — confirms route and timestamp. */
    TOLL_RECORD,

    /** Police / security checkpoint stop — may or may not result in fine. */
    CHECKPOINT_STOP,

    /** ANPR or CCTV camera sighting of a vehicle or person. */
    SURVEILLANCE_SIGHTING,

    /** Sea or air port entry / departure processing. */
    PORT_ENTRY,

    /** Fuel purchase or fleet card transaction (location + time anchor). */
    FUEL_TRANSACTION,

    OTHER
}
