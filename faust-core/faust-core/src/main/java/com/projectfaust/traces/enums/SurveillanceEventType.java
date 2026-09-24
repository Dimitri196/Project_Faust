package com.projectfaust.traces.enums;

/**
 * Classifies the type of operative surveillance event recorded in
 * {@link com.projectfaust.traces.SurveillanceEvent}.
 *
 * @author Dimitri / Project Faust
 */
public enum SurveillanceEventType {

    /** Subject physically observed at a location. */
    SIGHTING,

    /** Subject entered a building, zone, or country. */
    ENTRY,

    /** Subject exited a building, zone, or country. */
    EXIT,

    /** Subject observed meeting one or more other persons. */
    MEETING,

    /** Subject crossed an international or internal border. */
    BORDER_CROSSING,

    /** Subject's vehicle spotted — identity of driver not confirmed. */
    VEHICLE_SPOTTING,

    /** Subject was detained or arrested. */
    ARRESTED,

    /** Subject demonstrated counter-surveillance behaviour. */
    SURVEILLANCE_DETECTED,

    /** Subject observed conducting a dead-drop or clandestine exchange. */
    DEAD_DROP,

    /** Subject photographed or otherwise documented without direct contact. */
    PHOTOGRAPHED
}
