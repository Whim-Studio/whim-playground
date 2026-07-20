package com.heroquest.logic;

import com.heroquest.model.GameState;
import com.heroquest.model.Hero;
import com.heroquest.model.Monster;
import com.heroquest.model.Phase;
import com.heroquest.model.Point;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Zargon's turn: every revealed monster advances toward the nearest Hero along
 * the shortest open path and attacks if it ends adjacent.
 */
public final class ZargonAI {
    private static final int[][] STEPS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final CombatEngine combat;

    public ZargonAI(CombatEngine combat) {
        this.combat = combat;
    }

    public void runZargonPhase(GameState state) {
        state.setPhase(Phase.ZARGON);
        for (Monster m : state.getLivingMonsters()) {
            // Only monsters the Heroes have already discovered are active.
            if (!state.getMap().tileAt(m.getPosition()).isRevealed()) {
                continue;
            }
            actMonster(state, m);
            if (state.isDefeat()) {
                return;
            }
        }
    }

    private void actMonster(GameState state, Monster m) {
        Hero target = nearestHero(state, m);
        if (target == null) {
            return;
        }

        int move = m.baseMovement();
        // Step toward the target while movement remains and not yet adjacent.
        while (move > 0 && !m.getPosition().isOrthogonalNeighbour(target.getPosition())) {
            Map<Point, Integer> dist = bfsFrom(state, target.getPosition(), m.getPosition());
            Point best = null;
            int bestD = Integer.MAX_VALUE;
            Integer here = dist.get(m.getPosition());
            int cur = here == null ? Integer.MAX_VALUE : here;
            for (int[] s : STEPS) {
                Point n = m.getPosition().translate(s[0], s[1]);
                if (!state.getMap().isWalkable(n) || state.isOccupied(n)) {
                    continue;
                }
                Integer d = dist.get(n);
                if (d != null && d < bestD && d < cur) {
                    bestD = d;
                    best = n;
                }
            }
            if (best == null) {
                break; // blocked / no progress possible
            }
            m.setPosition(best);
            move--;
            // Re-evaluate nearest hero after moving.
            target = nearestHero(state, m);
            if (target == null) {
                return;
            }
        }

        if (m.getPosition().isOrthogonalNeighbour(target.getPosition())) {
            CombatEngine.Result r = combat.resolveAttack(m, target);
            state.log(m.getName() + " attacks " + target.getName() + ": " + r.skulls
                    + " skulls vs " + r.blocks + " shields -> " + r.damage + " Body Points.");
            if (r.fatal) {
                state.log(target.getName() + " has fallen!");
            }
        }
    }

    private Hero nearestHero(GameState state, Monster m) {
        Hero best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Hero h : state.getLivingHeroes()) {
            int d = m.getPosition().manhattan(h.getPosition());
            if (d < bestDist) {
                bestDist = d;
                best = h;
            }
        }
        return best;
    }

    /** BFS distance field from {@code origin} across open squares (monster start allowed). */
    private Map<Point, Integer> bfsFrom(GameState state, Point origin, Point moverStart) {
        Map<Point, Integer> dist = new HashMap<Point, Integer>();
        Queue<Point> frontier = new ArrayDeque<Point>();
        dist.put(origin, 0);
        frontier.add(origin);
        while (!frontier.isEmpty()) {
            Point cur = frontier.poll();
            int d = dist.get(cur);
            for (int[] s : STEPS) {
                Point n = cur.translate(s[0], s[1]);
                if (dist.containsKey(n)) {
                    continue;
                }
                if (!state.getMap().isWalkable(n)) {
                    continue;
                }
                // Treat occupied squares as blocked, except the mover's own square
                // (so it has a distance value to compare against).
                if (state.isOccupied(n) && !n.equals(moverStart)) {
                    continue;
                }
                dist.put(n, d + 1);
                frontier.add(n);
            }
        }
        return dist;
    }
}
