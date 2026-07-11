package com.whim.bc3k.sim.crew;

/**
 * Named interior locations a crew member can occupy. Matches the brief's examples
 * (quarters, galley, bridge, engineering, ...). Crew walk between these; some
 * satisfy needs (QUARTERS = rest, GALLEY = food).
 */
public enum ShipLocation {
    BRIDGE, ENGINEERING, GALLEY, QUARTERS, MEDBAY, TACTICAL, CARGO_BAY, FLIGHT_DECK
}
