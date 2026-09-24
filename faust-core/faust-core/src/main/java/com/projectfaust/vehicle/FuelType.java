package com.projectfaust.vehicle;

/**
 * Fuel / propulsion type of a registered vehicle.
 *
 * @author Dimitri / Project Faust
 */
public enum FuelType {

    PETROL,
    DIESEL,
    LPG,
    CNG,
    HYBRID_PETROL,
    HYBRID_DIESEL,
    PLUG_IN_HYBRID,
    ELECTRIC,
    HYDROGEN,
    BIODIESEL,
    ETHANOL,
    /** Two-stroke mix (motorcycles, older vehicles). */
    TWO_STROKE,
    /** Aviation fuel (AvGas / Jet-A). */
    AVIATION_FUEL,
    OTHER,
    UNKNOWN
}
