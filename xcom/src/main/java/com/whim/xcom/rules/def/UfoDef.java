package com.whim.xcom.rules.def;

/**
 * A UFO type (Small Scout … Battleship). Drives interception difficulty, the
 * crash-site map size and the recoverable Alien Alloys / Elerium on salvage.
 */
public interface UfoDef extends GameDef {

    /** Hull hit-points for air combat. */
    int hullPoints();

    /** Cruise speed in knots-equivalent units. */
    int speed();

    /** UFO weapon power (damage per hit against an interceptor). */
    int weaponPower();

    /** Weapon range band. */
    int weaponRange();

    /** Battlescape map size in tiles per side at the crash/landing site. */
    int mapSize();

    /** Approximate crew size (min) for the ground assault. */
    int minCrew();

    /** Approximate crew size (max) for the ground assault. */
    int maxCrew();
}
