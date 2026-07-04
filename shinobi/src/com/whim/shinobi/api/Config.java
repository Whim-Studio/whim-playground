package com.whim.shinobi.api;

/**
 * Shared world constants agreed by ALL tasks. Units are world pixels; time is ticks.
 * The engine advances at {@link #TICK_HZ} ticks/second on a background thread,
 * fully decoupled from the Swing repaint timer. The UI reads these for the camera,
 * plane geometry, and viewport sizing so engine and renderer agree on coordinates.
 *
 * DO NOT modify — Task 1/2/3 all depend on these exact values.
 */
public final class Config {
    private Config() {}

    /** Fixed simulation rate. Engine sleeps to hold this cadence. */
    public static final int TICK_HZ = 60;
    public static final double DT = 1.0 / TICK_HZ;

    /** Logical viewport (the camera window into the level), in world pixels. */
    public static final int VIEW_W = 512;
    public static final int VIEW_H = 448;

    /** Full first-level width in world pixels; camera scrolls 0..(LEVEL_W - VIEW_W). */
    public static final int LEVEL_W = 4096;

    /** A standing entity's collision box size, in world pixels. */
    public static final int ENTITY_W = 28;
    public static final int ENTITY_H = 44;

    /**
     * Two side-scrolling paths (classic Shinobi plane-jump). Each value is the
     * world Y of the GROUND that entities on that plane stand ON (feet Y). An
     * entity's top-left y = groundY - ENTITY_H when grounded. UPPER is drawn
     * higher on screen (background path); LOWER is the foreground path.
     */
    public static final int GROUND_Y_LOWER = 384;
    public static final int GROUND_Y_UPPER = 224;

    /** Physics tuning (world pixels & pixels/tick). Engine owns final behavior. */
    public static final double GRAVITY = 0.55;        // added to vy each tick while airborne
    public static final double MOVE_SPEED = 2.6;      // horizontal walk speed
    public static final double JUMP_VELOCITY = -9.5;  // initial vy on jump

    /** Proximity threshold (world pixels, same-plane): <= is melee, > is projectile. */
    public static final double MELEE_RANGE = 40.0;

    /** Starting resources. */
    public static final int START_LIVES = 3;
    public static final int START_NINJUTSU = 2;
    public static final int LEVEL_TIME_SECONDS = 90;
}
