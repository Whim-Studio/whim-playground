package com.whim.powermonger.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.whim.powermonger.api.Enums.Allegiance;
import com.whim.powermonger.api.Enums.Job;
import com.whim.powermonger.api.Enums.TerrainType;
import com.whim.powermonger.domain.MapGrid;
import com.whim.powermonger.domain.Tile;
import com.whim.powermonger.domain.Townsperson;
import com.whim.powermonger.domain.WorldState;

/**
 * Artificial life for neutral townspeople. Each person picks a resource node
 * appropriate to its {@link Job} (farming on GRASS, fishing at a water edge, herding
 * on open grass/hill, crafting in town), steers toward it a small step each tick,
 * works there a while, then picks a fresh node and loops. No player input.
 *
 * Per-person target/work state is kept here (engine-thread confined); domain
 * Townspeople stay passive position holders.
 */
public final class LifeAI {

    /** Tiles moved per tick by a walking townsperson (before the weather factor). */
    private static final double WALK_SPEED = 0.035;
    /** How close counts as "arrived" at a node. */
    private static final double ARRIVE_EPS = 0.20;
    /** Ticks spent working a node before wandering off again. */
    private static final int WORK_TICKS = 60;
    /** Search radius (tiles) when hunting for an appropriate resource node. */
    private static final int SEARCH_RADIUS = 8;

    private static final class Goal {
        double tx;
        double ty;
        int workLeft;
    }

    private final Map<Integer, Goal> goals = new HashMap<Integer, Goal>();

    public void tick(WorldState w, Random rng) {
        MapGrid g = w.grid();
        double step = WALK_SPEED * Math.max(0.2, w.movementFactor());
        for (int i = 0; i < w.townspeople().size(); i++) {
            Townsperson p = w.townspeople().get(i);
            if (p.allegiance() != Allegiance.NEUTRAL) {
                continue; // conscripted folk stop tending their trade
            }
            Goal goal = goals.get(p.id());
            if (goal == null) {
                goal = chooseGoal(p, g, rng);
                goals.put(p.id(), goal);
            }
            double dx = goal.tx - p.x();
            double dy = goal.ty - p.y();
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist <= ARRIVE_EPS) {
                // At the node: work it down, then pick a new one.
                if (goal.workLeft > 0) {
                    goal.workLeft--;
                } else {
                    goals.put(p.id(), chooseGoal(p, g, rng));
                }
            } else {
                double nx = p.x() + dx / dist * Math.min(step, dist);
                double ny = p.y() + dy / dist * Math.min(step, dist);
                p.setPosition(nx, ny);
            }
        }
    }

    private Goal chooseGoal(Townsperson p, MapGrid g, Random rng) {
        int cx = (int) Math.round(p.x());
        int cy = (int) Math.round(p.y());
        int[] node = findNode(p.job(), cx, cy, g, rng);
        Goal goal = new Goal();
        if (node != null) {
            goal.tx = node[0] + 0.5;
            goal.ty = node[1] + 0.5;
        } else {
            // Nothing suitable nearby — wander to a random walkable tile close by.
            int[] w2 = randomWalkable(cx, cy, g, rng);
            goal.tx = w2[0] + 0.5;
            goal.ty = w2[1] + 0.5;
        }
        goal.workLeft = WORK_TICKS + rng.nextInt(WORK_TICKS);
        return goal;
    }

    /** Scan an expanding box for a tile matching the job; return {x,y} or null. */
    private int[] findNode(Job job, int cx, int cy, MapGrid g, Random rng) {
        int[] best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                int x = cx + dx;
                int y = cy + dy;
                if (!g.inBounds(x, y)) {
                    continue;
                }
                Tile t = g.tile(x, y);
                if (t == null || !suits(job, t, g, x, y)) {
                    continue;
                }
                // Prefer richer, closer nodes; a little randomness spreads the crowd.
                int score = t.foodPotential() - (Math.abs(dx) + Math.abs(dy)) * 3 + rng.nextInt(6);
                if (score > bestScore) {
                    bestScore = score;
                    best = new int[] { x, y };
                }
            }
        }
        return best;
    }

    private boolean suits(Job job, Tile t, MapGrid g, int x, int y) {
        switch (job) {
            case FARMING:
                return t.terrain() == TerrainType.GRASS && t.foodPotential() > 0;
            case FISHING:
                return isWaterEdge(t, g, x, y);
            case HERDING:
                return t.terrain() == TerrainType.GRASS || t.terrain() == TerrainType.HILL;
            case CRAFTING:
                return t.terrain() == TerrainType.TOWN;
            case IDLE:
            default:
                return isLand(t);
        }
    }

    private boolean isWaterEdge(Tile t, MapGrid g, int x, int y) {
        if (!isLand(t)) {
            return false;
        }
        int[][] n = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int i = 0; i < n.length; i++) {
            Tile adj = g.tile(x + n[i][0], y + n[i][1]);
            if (adj != null && (adj.terrain() == TerrainType.SHALLOW_WATER
                    || adj.terrain() == TerrainType.DEEP_WATER)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLand(Tile t) {
        return t.terrain() != TerrainType.DEEP_WATER && t.terrain() != TerrainType.SHALLOW_WATER;
    }

    private int[] randomWalkable(int cx, int cy, MapGrid g, Random rng) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int x = cx + rng.nextInt(2 * SEARCH_RADIUS + 1) - SEARCH_RADIUS;
            int y = cy + rng.nextInt(2 * SEARCH_RADIUS + 1) - SEARCH_RADIUS;
            if (g.inBounds(x, y)) {
                Tile t = g.tile(x, y);
                if (t != null && isLand(t)) {
                    return new int[] { x, y };
                }
            }
        }
        int x = Math.max(0, Math.min(g.width() - 1, cx));
        int y = Math.max(0, Math.min(g.height() - 1, cy));
        return new int[] { x, y };
    }
}
