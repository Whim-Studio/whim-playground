package com.whim.kenshi.api;

/**
 * World-wide tuning constants shared by every task. All coordinates are in
 * <b>world units</b> (1 unit == 1 pixel at zoom 1.0). (x, y) is the CENTER of a
 * character. This class is owned by the orchestrator; tasks read it but must not
 * change the seam values (adding new derived constants in your own package is
 * fine).
 */
public final class Config {

    private Config() {}

    // ---- Simulation clock (Real-Time-with-Pause) -------------------------
    /** Fixed simulation rate. The engine ticks this many times per second on a
     * background thread, fully decoupled from Swing repaint. */
    public static final int TICK_HZ = 20;
    /** Seconds of world time per tick. */
    public static final double DT = 1.0 / TICK_HZ;
    /** In-world seconds that elapse per real tick at 1x game speed. Kenshi runs
     * a compressed clock so hunger/healing are observable in a demo session. */
    public static final double WORLD_SECONDS_PER_TICK = 6.0;

    // ---- Map -------------------------------------------------------------
    /** Map is a square grid of TILES. */
    public static final int MAP_TILES = 96;
    /** Size of one tile in world units. */
    public static final double TILE_SIZE = 64.0;
    /** Total map extent in world units. */
    public static final double WORLD_SIZE = MAP_TILES * TILE_SIZE;

    // ---- Viewport (UI hints; the UI owns the real camera) ----------------
    public static final int VIEW_W = 1000;
    public static final int VIEW_H = 680;
    public static final double MIN_ZOOM = 0.25;
    public static final double MAX_ZOOM = 3.0;
    public static final double DEFAULT_ZOOM = 1.0;

    // ---- Character geometry ---------------------------------------------
    /** Collision / picking radius of a character in world units. */
    public static final double CHAR_RADIUS = 12.0;
    /** Base move speed in world units per WORLD second (modulated by Athletics
     * and by crawling). */
    public static final double BASE_MOVE_SPEED = 3.2;
    /** Multiplier applied to move speed while a character is crawling (both legs
     * disabled). */
    public static final double CRAWL_SPEED_MULT = 0.28;
    /** Distance within which a melee attacker can strike its target. */
    public static final double MELEE_RANGE = 22.0;

    // ---- Anatomy ---------------------------------------------------------
    /** Default maximum HP for the torso parts (Head, Chest, Stomach). */
    public static final double TORSO_PART_MAX = 100.0;
    /** Default maximum HP for the limbs (arms, legs). */
    public static final double LIMB_PART_MAX = 100.0;
    /** A part is "disabled" once current HP falls to/below this fraction of max
     * (negative HP is allowed; -max is the death floor for a part). */
    public static final double PART_DISABLED_AT = 0.0;
    /** Lowest a part may fall to (== -max). Reaching it on Head/Chest is lethal. */
    public static final double PART_MIN_FRACTION = -1.0;

    // ---- Survival --------------------------------------------------------
    public static final double HUNGER_MAX = 1000.0;
    /** Hunger consumed per WORLD second. Empty stomach begins starvation damage. */
    public static final double HUNGER_DECAY_PER_SEC = 0.30;
    public static final double BLOOD_MAX = 100.0;
    /** Blood regained per WORLD second while not bleeding. */
    public static final double BLOOD_REGEN_PER_SEC = 0.05;
    /** A character passes out from blood loss below this value. */
    public static final double BLOOD_UNCONSCIOUS_AT = 25.0;
    /** Natural HP healed per WORLD second per part when fed and resting. */
    public static final double HEAL_PER_SEC = 0.20;

    // ---- Combat ----------------------------------------------------------
    /** Base per-swing hit chance before skill differential is applied. */
    public static final double BASE_HIT_CHANCE = 0.55;
    /** Hit chance gained/lost per point of (attackerMelee - defenderDefence). */
    public static final double HIT_CHANCE_PER_SKILL = 0.03;
    public static final double MIN_HIT_CHANCE = 0.05;
    public static final double MAX_HIT_CHANCE = 0.95;
    /** Fraction of a landed hit's damage converted into a bleed-rate bump. */
    public static final double BLEED_FROM_DAMAGE = 0.06;

    // ---- Skills ----------------------------------------------------------
    public static final int SKILL_MIN = 1;
    public static final int SKILL_MAX = 100;

    // ---- Selection / picking --------------------------------------------
    /** Extra world-unit slack added to CHAR_RADIUS when picking with a click. */
    public static final double PICK_SLACK = 6.0;
}
