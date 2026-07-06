package com.whim.starcommand.model;

import java.io.Serializable;

/**
 * A combatant on the tactical ground/boarding grid. Player units are backed by
 * a {@link Character} (so wounds carry back to the roster); enemy units are
 * generated for the encounter.
 */
public class GroundUnit implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Side { PLAYER, ENEMY }

    public String name;
    public Side side;
    public int x;
    public int y;
    public int hp;
    public int maxHp;
    public int accuracy;   // 0..100-ish to-hit contribution
    public int minDamage;
    public int maxDamage;
    public int moveRange;   // tiles per turn
    public int attackRange; // tiles (1 = melee)
    public boolean acted = false;

    /** The backing crew member for PLAYER units; null for enemies. */
    public transient Character source;

    public boolean alive() { return hp > 0; }

    public int dist(GroundUnit o) {
        return Math.abs(x - o.x) + Math.abs(y - o.y);
    }
}
