package com.whim.shinobi.domain;

import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

/**
 * A hostile actor. Extends {@link Entity} with its {@link Enums.EnemyType}
 * (THUG or sword-NINJA), a blocking flag (NINJAs deflect shuriken), and neutral
 * AI bookkeeping fields the engine drives. Implements {@link Views.EnemyView}.
 *
 * NOTE: no AI <em>logic</em> here — the engine's ThugAI / NinjaAI (Task 2) reads
 * and writes these fields. This class only holds the state.
 */
public class Enemy extends Entity implements Views.EnemyView {
    private final Enums.EnemyType type;

    /** True while actively deflecting (NINJA block pose / shuriken immunity). */
    private boolean blocking = false;

    // ---- AI state the engine drives (no behavior lives here) ----
    /** Generic countdown the engine uses to pace decisions/attacks. */
    private int aiTimer = 0;
    /** Ticks left in the current attack windup/recovery. */
    private int attackTimer = 0;
    /** Patrol direction the engine flips at bounds (-1 left, +1 right). */
    private int patrolDir = -1;
    /** Anchor X the engine can patrol around. */
    private double homeX;
    /** True once the enemy has noticed the player. */
    private boolean aggro = false;
    /** Patrol bounds (world X) the engine flips direction at; default span ±80. */
    private double patrolMinX;
    private double patrolMaxX;

    public Enemy(Aabb box, Enums.Plane plane, Enums.EnemyType type) {
        super(box, plane);
        this.type = type;
        this.homeX = box.x();
        this.patrolMinX = box.x() - 80.0;
        this.patrolMaxX = box.x() + 80.0;
        this.hp = 1;
    }

    // ---- Views.EnemyView ----
    @Override public Enums.EnemyType type() { return type; }
    @Override public boolean blocking() { return blocking; }

    public void setBlocking(boolean blocking) { this.blocking = blocking; }

    public double patrolMinX() { return patrolMinX; }
    public double patrolMaxX() { return patrolMaxX; }
    public void setPatrolMinX(double v) { this.patrolMinX = v; }
    public void setPatrolMaxX(double v) { this.patrolMaxX = v; }

    // ---- AI bookkeeping accessors ----
    public int aiTimer() { return aiTimer; }
    public void setAiTimer(int t) { this.aiTimer = t; }
    public void tickAiTimer() { if (aiTimer > 0) aiTimer--; }

    public int attackTimer() { return attackTimer; }
    public void setAttackTimer(int t) { this.attackTimer = t; }
    public void tickAttackTimer() { if (attackTimer > 0) attackTimer--; }

    public int patrolDir() { return patrolDir; }
    public void setPatrolDir(int dir) { this.patrolDir = dir; }
    public void flipPatrol() { this.patrolDir = -this.patrolDir; }

    public double homeX() { return homeX; }
    public void setHomeX(double homeX) { this.homeX = homeX; }

    public boolean aggro() { return aggro; }
    public void setAggro(boolean aggro) { this.aggro = aggro; }
}
