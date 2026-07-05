package com.whim.populous.domain;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Views.FollowerView;

/**
 * A single walker ("person") belonging to one deity. Implements the read-only
 * {@link FollowerView}; the engine's follower AI drives the mutable setters.
 *
 * <p>Position is fractional tile-space so movement is sub-tile smooth. Health and
 * stamina are clamped to 0..100: stamina drains while walking and recovers at a
 * settlement, health drops on LAVA and hits zero on drowning (WATER/SHALLOW/SWAMP).
 */
public final class Follower implements FollowerView {

    private Allegiance allegiance;
    private double x;
    private double y;
    private int health = 100;
    private int stamina = 100;
    private boolean alive = true;

    public Follower(Allegiance allegiance, double x, double y) {
        this.allegiance = allegiance;
        this.x = x;
        this.y = y;
    }

    // ---- FollowerView -------------------------------------------------------

    @Override public double x() { return x; }
    @Override public double y() { return y; }
    @Override public Allegiance allegiance() { return allegiance; }
    @Override public int health() { return health; }
    @Override public int stamina() { return stamina; }
    @Override public boolean alive() { return alive; }

    // ---- engine mutation ----------------------------------------------------

    public void setAllegiance(Allegiance a) { this.allegiance = a; }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void moveBy(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }

    public void setHealth(int h) { this.health = clamp(h); }
    public void damage(int amount) {
        this.health = clamp(this.health - amount);
        if (this.health == 0) {
            this.alive = false;
        }
    }
    public void heal(int amount) { this.health = clamp(this.health + amount); }

    public void setStamina(int s) { this.stamina = clamp(s); }
    public void drainStamina(int amount) { this.stamina = clamp(this.stamina - amount); }
    public void recoverStamina(int amount) { this.stamina = clamp(this.stamina + amount); }

    /** Kill this follower (drowned, killed in battle, Armageddon, etc.). */
    public void kill() {
        this.alive = false;
        this.health = 0;
    }

    public void setAlive(boolean alive) { this.alive = alive; }

    /** Nearest integer tile column the follower occupies. */
    public int tileCol() { return (int) Math.floor(x); }
    /** Nearest integer tile row the follower occupies. */
    public int tileRow() { return (int) Math.floor(y); }

    private static int clamp(int v) {
        if (v < 0) {
            return 0;
        }
        return v > 100 ? 100 : v;
    }
}
