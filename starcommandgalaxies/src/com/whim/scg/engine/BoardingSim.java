package com.whim.scg.engine;

import com.whim.scg.api.Enums;
import com.whim.scg.api.GridPos;
import com.whim.scg.model.BoardingModel;
import com.whim.scg.model.CrewModel;
import com.whim.scg.model.GameState;

import java.util.Random;

/** Turn-lite tile boarding: player moves/attacks freely, hostiles act on a cadence. */
final class BoardingSim {
    private final Random rng;
    private static final double HOSTILE_CADENCE = 1.1; // seconds between hostile actions

    BoardingSim(Random rng) { this.rng = rng; }

    // ---- player intents -------------------------------------------------
    boolean move(BoardingModel b, CrewModel c, GridPos to) {
        if (b == null || b.over || c == null || !c.alive() || to == null) return false;
        if (!b.walkable(to)) return false;
        if (c.boardingPos == null || c.boardingPos.manhattan(to) != 1) return false;
        if (b.occupant(to) != null) return false;
        c.boardingPos = to;
        return true;
    }

    boolean attack(GameState st, BoardingModel b, CrewModel attacker, CrewModel target) {
        if (b == null || b.over || attacker == null || target == null) return false;
        if (!attacker.alive() || !target.alive()) return false;
        if (attacker.boardingPos == null || target.boardingPos == null) return false;
        if (attacker.boardingPos.manhattan(target.boardingPos) != 1) return false;
        int dmg = meleeDamage(attacker);
        target.hp = Math.max(0, target.hp - dmg);
        if (attacker.faction == Enums.Faction.FEDERATION) {
            st.logLine(attacker.name + " strikes for " + dmg);
            if (!target.alive()) { st.logLine("Hostile down"); attacker.gainXp(20); }
        } else if (!target.alive()) {
            st.logLine(target.name + " has fallen");
        }
        return true;
    }

    private int meleeDamage(CrewModel c) {
        int skill = c.skill(Enums.StatType.COMBAT);
        int base = 5 + skill / 8;
        return base + rng.nextInt(4);
    }

    // ---- hostile AI -----------------------------------------------------
    void tick(GameState st, double dt) {
        BoardingModel b = st.boarding;
        if (b == null || b.over) return;
        for (CrewModel h : b.hostiles) {
            if (!h.alive()) continue;
            h.actTimer += dt;
            if (h.actTimer < HOSTILE_CADENCE) continue;
            h.actTimer = 0;
            act(st, b, h);
        }
        checkEnd(st, b);
    }

    private void act(GameState st, BoardingModel b, CrewModel h) {
        CrewModel target = nearestFriendly(b, h);
        if (target == null || h.boardingPos == null || target.boardingPos == null) return;
        int d = h.boardingPos.manhattan(target.boardingPos);
        if (d == 1) {
            attack(st, b, h, target);
        } else {
            stepToward(b, h, target.boardingPos);
        }
    }

    private void stepToward(BoardingModel b, CrewModel h, GridPos goal) {
        GridPos from = h.boardingPos;
        Enums.Direction bestDir = null;
        int bestDist = Integer.MAX_VALUE;
        for (Enums.Direction dir : Enums.Direction.values()) {
            GridPos to = from.step(dir);
            if (!b.walkable(to) || b.occupant(to) != null) continue;
            int nd = to.manhattan(goal);
            if (nd < bestDist) { bestDist = nd; bestDir = dir; }
        }
        if (bestDir != null) h.boardingPos = from.step(bestDir);
    }

    private CrewModel nearestFriendly(BoardingModel b, CrewModel h) {
        CrewModel best = null;
        int bd = Integer.MAX_VALUE;
        for (CrewModel f : b.friendlies) {
            if (!f.alive() || f.boardingPos == null) continue;
            int d = h.boardingPos.manhattan(f.boardingPos);
            if (d < bd) { bd = d; best = f; }
        }
        return best;
    }

    // ---- win / lose -----------------------------------------------------
    void checkEnd(GameState st, BoardingModel b) {
        if (b.over) return;
        boolean anyHostile = false;
        for (CrewModel h : b.hostiles) if (h.alive()) { anyHostile = true; break; }
        boolean anyFriendly = false;
        CrewModel atObjective = null;
        for (CrewModel f : b.friendlies) {
            if (f.alive()) {
                anyFriendly = true;
                if (b.objectivePos != null && b.objectivePos.equals(f.boardingPos)) atObjective = f;
            }
        }
        if (!anyHostile) { b.over = true; b.playerWon = true; }
        else if (atObjective != null) { b.over = true; b.playerWon = true; }
        else if (!anyFriendly) { b.over = true; b.playerWon = false; }
    }
}
