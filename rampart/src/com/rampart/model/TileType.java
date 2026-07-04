package com.rampart.model;

/**
 * The kind of terrain occupying a single grid cell.
 *
 * <p>These are the six documented Rampart tile kinds. They are pure state; all
 * transitions (wall -&gt; rubble on a hit, land -&gt; wall on placement, etc.) are
 * performed by the engine (Task 2), never here.
 */
public enum TileType {
    /** Open sea. Ships sail here; nothing may be built on it. */
    WATER,
    /** Buildable dry ground. Cannons and wall pieces may occupy land. */
    LAND,
    /** An intact wall segment. Chains of walls form the loops that enclose castles. */
    WALL,
    /** A wall segment blasted by a ship; a gap in the loop until repaired. */
    RUBBLE,
    /** A cell occupied by a placed cannon (sits on enclosed land). */
    CANNON,
    /** A cell occupied by a castle keep — the thing the player must keep enclosed. */
    CASTLE
}
