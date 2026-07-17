package com.whim.xcom.rules.def;

/**
 * A base facility (Living Quarters, Laboratory, Radar, Hangar, …). Footprint is
 * a square of {@code size × size} tiles on the 6×6 base grid.
 */
public interface FacilityDef extends GameDef {

    int buildCostDollars();

    int buildTimeDays();

    int monthlyMaintenanceDollars();

    /** Tiles per side (1 for most; 2 for the Hangar). */
    int size();

    /** Detection range in nautical-mile-equivalent units, or 0 if not a radar. */
    int detectionRange();

    /** Detection chance per 30-min tick as a percentage, or 0. */
    int detectionChancePercent();

    /** Capacity provided (beds, storage, lab space, workshop space, …); 0 if none. */
    int capacity();
}
