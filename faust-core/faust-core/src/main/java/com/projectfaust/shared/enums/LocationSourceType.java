package com.projectfaust.shared.enums;

public enum LocationSourceType {
    MANUAL,       // entered by an operator directly
    OVERPASS,     // imported from OpenStreetMap via Overpass API
    KATASTR,      // imported from Czech land registry
    GEONAMES,     // imported from Geonames API
    HUMINT,       // provided by a human source
    SEED          // loaded from locations.json seed file
}
