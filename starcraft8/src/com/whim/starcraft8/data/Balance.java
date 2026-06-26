package com.whim.starcraft8.data;

/** Tunable global constants for the simulation. */
public final class Balance {
    private Balance() {}

    public static final int TICKS_PER_SECOND = 60;
    public static final int WORKER_CARRY = 8;
    public static final int GATHER_TICKS = 40;
    public static final int START_MINERALS = 50;
    public static final int START_GAS = 0;
    public static final int START_SUPPLY_CAP = 10;
    public static final int SUPPLY_MAX = 200;

    /** Resource amounts per field/geyser. */
    public static final int MINERAL_FIELD_AMOUNT = 1500;
    public static final int GEYSER_AMOUNT = 5000;

    /** Default map dimensions. */
    public static final int MAP_WIDTH = 48;
    public static final int MAP_HEIGHT = 48;

    /** Number of worker units each player starts with. */
    public static final int START_WORKERS = 4;

    /** Mineral patches / geysers placed per start location. */
    public static final int MINERALS_PER_BASE = 6;
    public static final int GEYSERS_PER_BASE = 1;

    /** Protoss shield regen: +1 shield every N ticks while out of combat. */
    public static final int SHIELD_REGEN_TICKS = 32;

    /** Min damage dealt after armor reduction. */
    public static final int MIN_DAMAGE = 1;
}
