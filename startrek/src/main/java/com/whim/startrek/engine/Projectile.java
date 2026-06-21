package com.whim.startrek.engine;

import com.whim.startrek.domain.Race;

/**
 * A live weapon in flight during a real-time battle: a phaser beam ({@code torpedo == false}) or a
 * photon torpedo ({@code torpedo == true}). Mutable; {@link BattleSimulator} advances it each step.
 */
public class Projectile {

    private double x;
    private double y;
    private double vx;
    private double vy;
    private final boolean torpedo;
    private final Race owner;
    private final int damage;
    private double traveled;
    private final double maxTravel;
    private boolean spent;

    Projectile(double x, double y, double vx, double vy, boolean torpedo, Race owner, int damage, double maxTravel) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.torpedo = torpedo;
        this.owner = owner;
        this.damage = damage;
        this.maxTravel = maxTravel;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getVx() {
        return vx;
    }

    public double getVy() {
        return vy;
    }

    public boolean isTorpedo() {
        return torpedo;
    }

    public Race getOwner() {
        return owner;
    }

    public int getDamage() {
        return damage;
    }

    boolean isSpent() {
        return spent;
    }

    void markSpent() {
        this.spent = true;
    }

    /** Advance by {@code dt} seconds; flags itself spent once it outruns its range. */
    void advance(double dt) {
        double dx = vx * dt;
        double dy = vy * dt;
        x += dx;
        y += dy;
        traveled += Math.sqrt(dx * dx + dy * dy);
        if (traveled >= maxTravel) {
            spent = true;
        }
    }
}
