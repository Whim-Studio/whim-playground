package com.whim.oggalaxy.api;

/**
 * Universe-wide scalar constants. Central so the simulation and UI agree on the
 * shape of the world, the tick clock, and the tunable combat / expedition rules.
 *
 * A "tick" is the unit of simulated time. One tick == {@link #SECONDS_PER_TICK}
 * seconds of in-game time. Production, build timers and fleet flight are all
 * expressed in ticks. The engine can advance one tick (manual "advance turn") or
 * many ticks per real second (fast-forward), but the math is identical.
 */
public final class GameConfig {

    private GameConfig() {
    }

    // --- Universe geometry ---
    public static final int GALAXIES = 4;
    public static final int SYSTEMS_PER_GALAXY = 100;
    public static final int POSITIONS_PER_SYSTEM = 15;

    // --- Clock ---
    /** In-game seconds represented by a single simulation tick. 1 tick = 1 hour of game time. */
    public static final int SECONDS_PER_TICK = 3600;
    /** Universe economy speed multiplier (OGame-style). Production is per hour * this. */
    public static final int ECONOMY_SPEED = 6;
    /** Universe fleet speed multiplier. */
    public static final int FLEET_SPEED = 6;

    // --- Starting conditions ---
    public static final int HOME_FIELDS_BASE = 163;          // usable building slots on the home world
    public static final double START_METAL = 1000;
    public static final double START_CRYSTAL = 800;
    public static final double START_DEUTERIUM = 200;
    public static final double START_DARK_MATTER = 8000;

    // --- Storage ---
    /** Base capacity of every resource with a level-0 store. */
    public static final double BASE_STORAGE = 100000;

    // --- Energy ---
    /** Below 100% energy ratio, mine output scales linearly by (produced/consumed). */
    public static final double MIN_ENERGY_FACTOR = 0.0;

    // --- Fusion / fuel ---
    public static final double FUSION_DEUT_PER_ENERGY = 0.0; // fusion deut consumption handled in Formulas

    // --- Combat ---
    public static final int COMBAT_MAX_ROUNDS = 6;
    /** Fraction of destroyed ship/defense metal+crystal that becomes a harvestable debris field. */
    public static final double DEBRIS_FIELD_RATIO = 0.30;
    /** Whether destroyed deuterium also contributes to debris (OG Galaxy variant). */
    public static final boolean DEBRIS_INCLUDES_DEUTERIUM = false;
    /** A shot whose value is below defender.shield * this fraction is fully absorbed (bounce). */
    public static final double SHIELD_BOUNCE_FRACTION = 0.01;
    /** Fraction of a destroyed planet's defenses that auto-rebuild after a defence (OGame: ~0.7). */
    public static final double DEFENSE_REBUILD_CHANCE = 0.70;
    /** Max fraction of resources an attacker can loot from a planet. */
    public static final double MAX_PLUNDER_FRACTION = 0.50;

    // --- Moon creation from debris (deterministic here: threshold on debris size) ---
    /** 1% moon chance per 100k debris in OGame; capped. We expose the linear coefficient and cap. */
    public static final double MOON_CHANCE_PER_DEBRIS = 1.0 / 100000.0; // -> probability
    public static final double MOON_CHANCE_CAP = 0.20;

    // --- Expeditions ---
    public static final int EXPEDITION_MAX_DURATION_TICKS = 3;
    public static final int EXPEDITION_MIN_DURATION_TICKS = 1;

    // --- Score ---
    /** Points per 1000 resources invested (OGame convention). */
    public static final double POINTS_PER_1000_RES = 1.0;

    public static boolean validCoords(int galaxy, int system, int position) {
        return galaxy >= 1 && galaxy <= GALAXIES
                && system >= 1 && system <= SYSTEMS_PER_GALAXY
                && position >= 1 && position <= POSITIONS_PER_SYSTEM;
    }
}
