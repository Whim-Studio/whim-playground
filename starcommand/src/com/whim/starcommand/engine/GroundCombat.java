package com.whim.starcommand.engine;

import com.whim.starcommand.model.Character;
import com.whim.starcommand.model.GroundUnit;

import java.util.ArrayList;
import java.util.List;

/**
 * Turn-based tactical ground/boarding combat on a tile grid, fully decoupled
 * from Swing so it can be unit-tested headlessly. The player moves and attacks
 * with each squad member; {@link #endPlayerTurn} runs a simple enemy AI.
 */
public class GroundCombat {

    public enum Result { ONGOING, PLAYER_WON, PLAYER_LOST }

    public final int width;
    public final int height;
    public final List<GroundUnit> units = new ArrayList<GroundUnit>();
    public Result result = Result.ONGOING;

    private final Rng rng;

    public GroundCombat(Rng rng, int width, int height) {
        this.rng = rng;
        this.width = width;
        this.height = height;
    }

    /** Build a player unit from a crew member, placed at (x,y). */
    public GroundUnit addPlayer(Character c, int x, int y) {
        GroundUnit u = new GroundUnit();
        u.name = c.name;
        u.side = GroundUnit.Side.PLAYER;
        u.x = x; u.y = y;
        u.maxHp = c.maxHp;
        u.hp = Math.max(1, c.hp);
        u.accuracy = 45 + c.accuracy * 2;
        u.minDamage = 2 + c.strength / 4;
        u.maxDamage = 5 + c.strength / 2;
        u.moveRange = 1 + Math.max(1, c.speed / 6);
        u.attackRange = "Marine".equals(c.role) || "Pilot".equals(c.role) ? 4 : 3;
        u.source = c;
        units.add(u);
        return u;
    }

    /** Build an enemy unit. */
    public GroundUnit addEnemy(String name, int x, int y, int hp, int acc,
                               int minDmg, int maxDmg, int move, int range) {
        GroundUnit u = new GroundUnit();
        u.name = name;
        u.side = GroundUnit.Side.ENEMY;
        u.x = x; u.y = y;
        u.hp = u.maxHp = hp;
        u.accuracy = acc;
        u.minDamage = minDmg;
        u.maxDamage = maxDmg;
        u.moveRange = move;
        u.attackRange = range;
        units.add(u);
        return u;
    }

    public boolean occupied(int x, int y) {
        for (GroundUnit u : units) if (u.alive() && u.x == x && u.y == y) return true;
        return false;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /** Move a unit to (x,y) if reachable this turn and the tile is free. */
    public boolean move(GroundUnit u, int x, int y) {
        if (u.acted || !u.alive() || result != Result.ONGOING) return false;
        if (!inBounds(x, y) || occupied(x, y)) return false;
        if (Math.abs(u.x - x) + Math.abs(u.y - y) > u.moveRange) return false;
        u.x = x; u.y = y;
        return true;
    }

    /** Attack a target in range; marks the attacker as acted. Returns a log line. */
    public String attack(GroundUnit attacker, GroundUnit target, List<String> log) {
        if (attacker.acted || !attacker.alive() || !target.alive()) return null;
        if (attacker.dist(target) > attacker.attackRange) return "Out of range.";
        attacker.acted = true;
        int hitChance = attacker.accuracy - target.moveRange * 3;
        String line;
        if (rng.chance(Math.max(15, Math.min(95, hitChance)))) {
            int dmg = rng.range(attacker.minDamage, attacker.maxDamage);
            target.hp -= dmg;
            line = attacker.name + " hits " + target.name + " for " + dmg + ".";
            if (target.hp <= 0) {
                target.hp = 0;
                line += " " + target.name + " is down!";
                syncSource(target);
            }
        } else {
            line = attacker.name + " misses " + target.name + ".";
        }
        if (log != null) log.add(line);
        checkResult();
        return line;
    }

    /** Whether any player unit still has an action available. */
    public boolean playerHasActions() {
        for (GroundUnit u : units)
            if (u.side == GroundUnit.Side.PLAYER && u.alive() && !u.acted) return true;
        return false;
    }

    /** End the player's turn: run enemy AI, then reset actions for the next round. */
    public List<String> endPlayerTurn() {
        List<String> log = new ArrayList<String>();
        if (result != Result.ONGOING) return log;
        for (GroundUnit e : new ArrayList<GroundUnit>(units)) {
            if (e.side != GroundUnit.Side.ENEMY || !e.alive()) continue;
            e.acted = false;
            enemyAct(e, log);
            if (result != Result.ONGOING) break;
        }
        // start the player's next round
        for (GroundUnit u : units) if (u.alive()) u.acted = false;
        return log;
    }

    private void enemyAct(GroundUnit e, List<String> log) {
        GroundUnit target = nearestPlayer(e);
        if (target == null) return;
        // step toward the target up to moveRange, then attack if able
        int steps = e.moveRange;
        while (steps-- > 0 && e.dist(target) > e.attackRange) {
            stepToward(e, target);
        }
        if (e.dist(target) <= e.attackRange) {
            e.acted = false; // enemies attack regardless of prior move
            attack(e, target, log);
        }
    }

    private void stepToward(GroundUnit e, GroundUnit t) {
        int nx = e.x + Integer.signum(t.x - e.x);
        int ny = e.y;
        if (nx == e.x || occupied(nx, ny) || !inBounds(nx, ny)) {
            nx = e.x;
            ny = e.y + Integer.signum(t.y - e.y);
        }
        if (inBounds(nx, ny) && !occupied(nx, ny)) { e.x = nx; e.y = ny; }
    }

    private GroundUnit nearestPlayer(GroundUnit e) {
        GroundUnit best = null;
        int bd = Integer.MAX_VALUE;
        for (GroundUnit u : units) {
            if (u.side != GroundUnit.Side.PLAYER || !u.alive()) continue;
            int d = e.dist(u);
            if (d < bd) { bd = d; best = u; }
        }
        return best;
    }

    private void syncSource(GroundUnit u) {
        if (u.source != null) { u.source.hp = 0; u.source.alive = false; }
    }

    /** Push surviving player unit HP back onto their crew records. */
    public void writeBackWounds() {
        for (GroundUnit u : units) {
            if (u.side == GroundUnit.Side.PLAYER && u.source != null) {
                u.source.hp = u.hp;
                u.source.alive = u.hp > 0;
            }
        }
    }

    private void checkResult() {
        boolean anyPlayer = false, anyEnemy = false;
        for (GroundUnit u : units) {
            if (!u.alive()) continue;
            if (u.side == GroundUnit.Side.PLAYER) anyPlayer = true;
            else anyEnemy = true;
        }
        if (!anyEnemy) result = Result.PLAYER_WON;
        else if (!anyPlayer) result = Result.PLAYER_LOST;
    }
}
