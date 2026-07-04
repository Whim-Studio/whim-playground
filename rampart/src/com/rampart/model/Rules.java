package com.rampart.model;

/**
 * The single source of truth for every tunable numeric constant in this Rampart
 * recreation. Task 2 (engine) and Task 3 (ui) MUST read values from here rather
 * than hardcoding magic numbers, so all three layers agree.
 *
 * <p>The rationale for each value is documented in {@code RESEARCH.md} alongside
 * the arcade rules it encodes. This class is not instantiable.
 */
public final class Rules {
    private Rules() {}

    // ---- Grid geometry ----
    /** Number of columns (cells across) in the playfield. */
    public static final int GRID_COLS = 30;
    /** Number of rows (cells down) in the playfield. */
    public static final int GRID_ROWS = 22;

    // ---- Phase durations (milliseconds) ----
    /** Length of the cannon-placement BUILD phase. */
    public static final long BUILD_PHASE_MILLIS = 15_000L;
    /** Length of the BATTLE phase. */
    public static final long BATTLE_PHASE_MILLIS = 30_000L;
    /** Length of the REPAIR phase. */
    public static final long REPAIR_PHASE_MILLIS = 20_000L;
    /** Length of the between-rounds transition splash. */
    public static final long ROUND_TRANSITION_MILLIS = 2_500L;

    // ---- Cannons ----
    /** Cannons available to place in round 1. */
    public static final int CANNON_POOL_BASE = 3;
    /** Extra cannons granted to the pool per additional round. */
    public static final int CANNON_POOL_PER_ROUND = 1;
    /** Hard cap on cannons a player may have at once. */
    public static final int CANNON_MAX = 8;
    /** Cells a single cannon occupies (footprint side, so 1 == a 1x1 cell). */
    public static final int CANNON_FOOTPRINT = 1;
    /** Reload/cooldown time between shots for one cannon, in milliseconds. */
    public static final long CANNON_RELOAD_MILLIS = 800L;
    /** Starting ammunition per cannon in a battle phase ({@code -1} == unlimited). */
    public static final int CANNON_START_AMMO = -1;
    /** Radius (in cells) of the blast a cannonball leaves on impact. */
    public static final int CANNON_BLAST_RADIUS = 1;

    // ---- Castles / survival ----
    /** Minimum castles that must be enclosed at end of REPAIR to survive a round. */
    public static final int MIN_ENCLOSED_CASTLES_TO_SURVIVE = 1;

    // ---- Territory / scoring ----
    /** Score awarded per enclosed land cell counted at end of a round. */
    public static final int SCORE_PER_TERRITORY_CELL = 10;
    /** Bonus score for surviving a round. */
    public static final int SCORE_ROUND_SURVIVAL_BONUS = 1_000;
    /**
     * Fraction (0..1) of total buildable land the player should enclose to earn
     * the "good territory" bonus. Informational threshold for the engine/HUD.
     */
    public static final double TERRITORY_GOOD_FRACTION = 0.40;

    // ---- Repair-phase wall pieces ----
    /** How many upcoming pieces are previewed (current + queued). */
    public static final int REPAIR_QUEUE_SIZE = 3;
    /** Number of distinct rotation states every wall piece cycles through. */
    public static final int PIECE_ROTATIONS = 4;

    // ---- Ships ----
    /** Ships that spawn in round 1; the engine may add more each round. */
    public static final int SHIPS_BASE = 3;
    /** Additional ships added to the wave per subsequent round. */
    public static final int SHIPS_PER_ROUND = 1;

    /**
     * Convenience: cannons available in the given (1-based) round, clamped to
     * {@link #CANNON_MAX}. Pure arithmetic helper, safe in the model layer.
     *
     * @param round the 1-based round number
     * @return the cannon pool size for that round
     */
    public static int cannonPoolForRound(int round) {
        int pool = CANNON_POOL_BASE + CANNON_POOL_PER_ROUND * Math.max(0, round - 1);
        return Math.min(pool, CANNON_MAX);
    }

    /**
     * Convenience: number of ships in the wave for the given (1-based) round.
     *
     * @param round the 1-based round number
     * @return the ship count for that round
     */
    public static int shipsForRound(int round) {
        return SHIPS_BASE + SHIPS_PER_ROUND * Math.max(0, round - 1);
    }
}
