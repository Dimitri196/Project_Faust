package com.projectfaust.mobility;

/**
 * Severity classification for mobility violations
 * ({@link MobilityEventType#PARKING_VIOLATION}, {@link MobilityEventType#TRAFFIC_VIOLATION},
 * {@link MobilityEventType#CHECKPOINT_STOP}).
 *
 * @author Dimitri / Project Faust
 */
public enum MobilityViolationSeverity {

    /** Administrative — minor fine, no points. */
    ADMINISTRATIVE,

    /** Points-eligible minor violation. */
    LOW,

    /** Points-eligible, mandatory court appearance possible. */
    MEDIUM,

    /** Serious — licence suspension, vehicle confiscation. */
    HIGH,

    /** Criminal offence — e.g. hit-and-run, DUI causing injury. */
    CRIMINAL
}
