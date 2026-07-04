package com.whim.shinobi.domain;

import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

/**
 * Abstract base for every moving actor (player, enemy, projectile). Holds a
 * mutable {@link Aabb} box, velocity, and coarse {@link Enums} state. Implements
 * {@link Views.EntityView} by delegating box coordinates to its {@link Aabb}.
 *
 * All movement/collision behavior lives in the engine (Task 2); this class is a
 * pure state holder.
 */
public abstract class Entity implements Views.EntityView {
    protected final Aabb box;
    protected double vx;
    protected double vy;

    protected Enums.Plane plane;
    protected Enums.Facing facing = Enums.Facing.RIGHT;
    protected Enums.EntityState state = Enums.EntityState.IDLE;

    protected boolean alive = true;
    protected int hp = 1;

    /** Physics state the engine sets each tick: feet resting on ground/platform. */
    protected boolean grounded = false;

    protected Entity(Aabb box, Enums.Plane plane) {
        this.box = box;
        this.plane = plane;
    }

    // ---- Views.BoxView / EntityView (read-only snapshot) ----
    @Override public double x() { return box.x(); }
    @Override public double y() { return box.y(); }
    @Override public double w() { return box.w(); }
    @Override public double h() { return box.h(); }
    @Override public Enums.Plane plane() { return plane; }
    @Override public Enums.Facing facing() { return facing; }
    @Override public Enums.EntityState state() { return state; }
    @Override public boolean alive() { return alive; }

    // ---- Direct state access for the engine ----
    public Aabb box() { return box; }

    public double vx() { return vx; }
    public double vy() { return vy; }
    public void setVx(double vx) { this.vx = vx; }
    public void setVy(double vy) { this.vy = vy; }
    public void setVelocity(double vx, double vy) { this.vx = vx; this.vy = vy; }

    public boolean grounded() { return grounded; }
    public void setGrounded(boolean grounded) { this.grounded = grounded; }

    public void setPlane(Enums.Plane plane) { this.plane = plane; }
    public void setFacing(Enums.Facing facing) { this.facing = facing; }
    public void setState(Enums.EntityState state) { this.state = state; }

    public void setAlive(boolean alive) { this.alive = alive; }
    /** Mark dead: clears alive and sets the DEAD render state. */
    public void kill() { this.alive = false; this.state = Enums.EntityState.DEAD; }

    public int hp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }
    /** Subtract damage; returns true if this drops hp to 0 or below. */
    public boolean damage(int amount) {
        this.hp -= amount;
        return this.hp <= 0;
    }
}
