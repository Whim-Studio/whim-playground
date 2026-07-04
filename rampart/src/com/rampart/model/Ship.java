package com.rampart.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An enemy vessel. Holds a sub-cell floating-point position, health, heading, a
 * waypoint path the engine steers along, and a firing-cadence counter. Pure state
 * — the engine (Task 2) owns movement, path following, and firing; it only reads
 * and writes these fields.
 */
public class Ship implements ShipView {
    private final ShipType type;
    private double x;
    private double y;
    private int health;
    private Direction heading = Direction.EAST;
    private boolean alive = true;

    /** Ordered waypoints the engine steers this ship along (grid coordinates). */
    private final List<Coord> path = new ArrayList<Coord>();
    /** Index into {@link #path} of the current target waypoint (engine-owned). */
    private int pathIndex;
    /** Milliseconds until this ship may fire again (engine-owned counter). */
    private long fireCooldownMillis;

    /**
     * Creates a ship of the given type with its type's base health.
     *
     * @param type the ship class (must be non-null)
     * @param x    initial sub-cell column
     * @param y    initial sub-cell row
     */
    public Ship(ShipType type, double x, double y) {
        this(type, x, y, type == null ? 0 : type.baseHealth());
    }

    /**
     * Creates a ship with explicit health.
     *
     * @param type   the ship class (must be non-null)
     * @param x      initial sub-cell column
     * @param y      initial sub-cell row
     * @param health starting hit points
     */
    public Ship(ShipType type, double x, double y, int health) {
        if (type == null) throw new IllegalArgumentException("type must not be null");
        this.type = type;
        this.x = x;
        this.y = y;
        this.health = health;
    }

    @Override public ShipType type() { return type; }
    @Override public double x() { return x; }
    @Override public double y() { return y; }
    @Override public int health() { return health; }
    @Override public Direction heading() { return heading; }
    @Override public boolean alive() { return alive; }

    // ---- Position / heading (engine only) ----

    /**
     * Sets the sub-cell position.
     *
     * @param x column (fractional)
     * @param y row (fractional)
     */
    public void setPosition(double x, double y) { this.x = x; this.y = y; }

    /** @param x the new fractional column */
    public void setX(double x) { this.x = x; }

    /** @param y the new fractional row */
    public void setY(double y) { this.y = y; }

    /** @param heading the new heading (must be non-null) */
    public void setHeading(Direction heading) {
        if (heading == null) throw new IllegalArgumentException("heading must not be null");
        this.heading = heading;
    }

    // ---- Health (engine only) ----

    /** @param health the new hit points */
    public void setHealth(int health) { this.health = health; }

    /**
     * Applies damage and updates the alive flag when health drops to zero.
     *
     * @param amount hit points to remove
     * @return {@code true} if this hit sank the ship
     */
    public boolean damage(int amount) {
        health -= amount;
        if (health <= 0) {
            health = 0;
            alive = false;
        }
        return !alive;
    }

    /** @param alive the new afloat state */
    public void setAlive(boolean alive) { this.alive = alive; }

    // ---- Path / waypoints (engine only) ----

    /** @return an unmodifiable view of this ship's waypoint path */
    public List<Coord> path() { return Collections.unmodifiableList(path); }

    /**
     * Replaces the waypoint path and resets the path cursor to the start.
     *
     * @param waypoints the new path; {@code null} is treated as empty
     */
    public void setPath(List<Coord> waypoints) {
        path.clear();
        if (waypoints != null) path.addAll(waypoints);
        pathIndex = 0;
    }

    /**
     * Appends a single waypoint to the path.
     *
     * @param waypoint the cell to append (ignored if null)
     */
    public void addWaypoint(Coord waypoint) {
        if (waypoint != null) path.add(waypoint);
    }

    /** @return the index of the current target waypoint */
    public int pathIndex() { return pathIndex; }

    /** @param pathIndex the new path cursor (engine only) */
    public void setPathIndex(int pathIndex) { this.pathIndex = pathIndex; }

    // ---- Firing cadence (engine only) ----

    /** @return milliseconds until this ship may fire again */
    public long fireCooldownMillis() { return fireCooldownMillis; }

    /** @param millis remaining fire cooldown (clamped at zero) */
    public void setFireCooldownMillis(long millis) {
        this.fireCooldownMillis = Math.max(0L, millis);
    }

    /**
     * Decrements the fire cooldown by elapsed time (engine tick helper).
     *
     * @param dtMillis elapsed milliseconds
     */
    public void decFireCooldown(long dtMillis) {
        fireCooldownMillis = Math.max(0L, fireCooldownMillis - Math.max(0L, dtMillis));
    }

    @Override
    public String toString() {
        return "Ship(" + type + "," + x + "," + y + ",hp=" + health + (alive ? "" : ",sunk") + ")";
    }
}
