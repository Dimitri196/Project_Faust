package com.projectfaust.mobility;

/**
 * Travel direction at a border crossing or port entry event.
 * Only meaningful for {@link MobilityEventType#BORDER_CROSSING} and
 * {@link MobilityEventType#PORT_ENTRY}; set to {@code N_A} for all other types.
 *
 * @author Dimitri / Project Faust
 */
public enum MobilityDirection {

    /** Subject / vehicle entering the country. */
    ENTRY,

    /** Subject / vehicle leaving the country. */
    EXIT,

    /** Direction confirmed but entry/exit not determined (transit). */
    TRANSIT,

    /** Not applicable — event has no directional meaning. */
    N_A
}
