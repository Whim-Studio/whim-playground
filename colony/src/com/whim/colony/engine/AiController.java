package com.whim.colony.engine;

import com.whim.colony.ColonyState;
import com.whim.colony.domain.BuildingType;
import com.whim.colony.domain.Colonist;
import com.whim.colony.domain.ColonyMap;
import com.whim.colony.domain.MapTile;
import com.whim.colony.domain.Needs;

import java.util.List;
import java.util.Random;

/**
 * The colonist decision layer. Each tick the {@link Simulation} asks the
 * controller to (re)assign a {@link com.whim.colony.api.Job} to any colonist
 * that is idle or has finished its current job. Priority, highest first:
 *
 * <ol>
 *   <li>a CRITICAL need (hunger or rest below {@link Needs#CRITICAL_THRESHOLD}) —
 *       whichever is worse is addressed first;</li>
 *   <li>a LOW need (below {@link Needs#LOW_THRESHOLD});</li>
 *   <li>otherwise idle work — haul at a stockpile, or wander.</li>
 * </ol>
 *
 * <p>When a job is assigned the controller also plants a path to its target tile
 * into {@link Colonist#getPath()} via the {@link Pathfinder}, so the job's move
 * phase has something to follow.
 */
public final class AiController {

    private final Pathfinder pathfinder;
    private final Random rng;

    public AiController(Pathfinder pathfinder, Random rng) {
        this.pathfinder = pathfinder;
        this.rng = rng;
    }

    /**
     * Ensure {@code c} has a live job. If its current job is null or complete, a
     * fresh job is chosen by priority and a path to it is planted. Does nothing
     * if the colonist is mid-job.
     */
    public void assignIfIdle(ColonyState state, Colonist c) {
        if (c.getCurrentJob() != null && !c.getCurrentJob().isComplete(state, c)) {
            return; // still busy
        }
        // Clear any stale path from the finished job before planning the next.
        c.getPath().clear();

        AbstractColonyJob job = chooseJob(state, c);
        c.setCurrentJob(job);
        planPath(state, c, job.getTargetX(), job.getTargetY());
    }

    /** Pick the highest-priority job for the colonist's current condition. */
    private AbstractColonyJob chooseJob(ColonyState state, Colonist c) {
        Needs needs = c.getNeeds();
        double hunger = needs.getHunger();
        double rest = needs.getRest();

        boolean hungerCritical = hunger < Needs.CRITICAL_THRESHOLD;
        boolean restCritical = rest < Needs.CRITICAL_THRESHOLD;

        // 1) Critical needs win; break ties toward the more depleted need.
        if (hungerCritical || restCritical) {
            if (hungerCritical && (!restCritical || hunger <= rest)) {
                return eatJob(state, c);
            }
            return sleepJob(state, c);
        }

        // 2) Low needs, again worst-first.
        boolean hungerLow = hunger < Needs.LOW_THRESHOLD;
        boolean restLow = rest < Needs.LOW_THRESHOLD;
        if (hungerLow || restLow) {
            if (hungerLow && (!restLow || hunger <= rest)) {
                return eatJob(state, c);
            }
            return sleepJob(state, c);
        }

        // 3) Idle: haul at a stockpile if one exists, else wander.
        int[] stockpile = nearestBuilding(state.getMap(), c, BuildingType.STOCKPILE);
        if (stockpile != null) {
            return new HaulJob(stockpile[0], stockpile[1]);
        }
        int[] spot = randomWalkableNear(state.getMap(), c);
        return new IdleWanderJob(spot[0], spot[1]);
    }

    private AbstractColonyJob eatJob(ColonyState state, Colonist c) {
        int[] src = nearestBuilding(state.getMap(), c, BuildingType.STOCKPILE);
        if (src == null) {
            src = new int[]{c.getX(), c.getY()};
        }
        return new EatJob(src[0], src[1]);
    }

    private AbstractColonyJob sleepJob(ColonyState state, Colonist c) {
        int[] bed = nearestBuilding(state.getMap(), c, BuildingType.BED);
        if (bed == null) {
            bed = new int[]{c.getX(), c.getY()};
        }
        return new SleepJob(bed[0], bed[1]);
    }

    /** Plant a path from the colonist to (tx,ty); leaves the path empty if already there. */
    private void planPath(ColonyState state, Colonist c, int tx, int ty) {
        c.getPath().clear();
        List<int[]> path = pathfinder.findPath(state.getMap(), c.getX(), c.getY(), tx, ty);
        c.getPath().addAll(path);
    }

    /**
     * @return the coordinates of the nearest walkable tile carrying a building of
     * {@code type} (by Manhattan distance), or {@code null} if none exists.
     */
    private int[] nearestBuilding(ColonyMap map, Colonist c, BuildingType type) {
        int best = Integer.MAX_VALUE;
        int[] found = null;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                MapTile tile = map.getTile(x, y);
                if (tile == null || !tile.hasBuilding() || !tile.isWalkable()) {
                    continue;
                }
                if (tile.getBuilding().getType() != type) {
                    continue;
                }
                int d = Math.abs(x - c.getX()) + Math.abs(y - c.getY());
                if (d < best) {
                    best = d;
                    found = new int[]{x, y};
                }
            }
        }
        return found;
    }

    /** Pick a nearby walkable tile to wander to, falling back to standing still. */
    private int[] randomWalkableNear(ColonyMap map, Colonist c) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int dx = rng.nextInt(7) - 3; // [-3..3]
            int dy = rng.nextInt(7) - 3;
            int nx = c.getX() + dx;
            int ny = c.getY() + dy;
            if (map.inBounds(nx, ny)) {
                MapTile tile = map.getTile(nx, ny);
                if (tile != null && tile.isWalkable()) {
                    return new int[]{nx, ny};
                }
            }
        }
        return new int[]{c.getX(), c.getY()};
    }
}
