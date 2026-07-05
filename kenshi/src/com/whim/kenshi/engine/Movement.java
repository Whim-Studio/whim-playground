package com.whim.kenshi.engine;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums.MoveState;
import com.whim.kenshi.domain.Character;

import java.util.List;

/**
 * Executes a character's movement toward the goal chosen by {@link CharacterAI},
 * following a {@link Pathfinder} waypoint list so water is routed around. Updates
 * position + heading, applies crawl speed, and returns the distance actually
 * moved this step (used to train Athletics).
 */
final class Movement {

    private final Pathfinder pathfinder;

    Movement(Pathfinder pathfinder) {
        this.pathfinder = pathfinder;
    }

    /** @return world-units moved this step (0 if stationary). */
    double advance(Character c, AiMemory mem, double dtWorld) {
        MoveState ms = c.moveState();
        if (ms == MoveState.DEAD || ms == MoveState.DOWNED) {
            return 0.0;
        }
        if (!mem.hasGoal) {
            return 0.0;
        }

        // (Re)compute the path when we have none or the goal shifted a tile+.
        boolean stale = mem.path == null
                || Math.hypot(mem.goalX - mem.pathGoalX, mem.goalY - mem.pathGoalY) > Config.TILE_SIZE;
        if (stale) {
            List<double[]> p = pathfinder.path(c.x(), c.y(), mem.goalX, mem.goalY);
            mem.path = p;
            mem.pathIndex = 0;
            mem.pathGoalX = mem.goalX;
            mem.pathGoalY = mem.goalY;
        }

        if (mem.path == null || mem.path.isEmpty()) {
            return 0.0;
        }

        double speed = SkillSystem.moveSpeed(c);
        if (ms == MoveState.CRAWLING) {
            speed *= Config.CRAWL_SPEED_MULT;
        }
        double budget = speed * dtWorld;
        double moved = 0.0;

        while (budget > 1e-6 && mem.pathIndex < mem.path.size()) {
            double[] wp = mem.path.get(mem.pathIndex);
            double dx = wp[0] - c.x();
            double dy = wp[1] - c.y();
            double d = Math.hypot(dx, dy);
            if (d <= 1e-6) {
                mem.pathIndex++;
                continue;
            }
            c.setHeading(Math.atan2(dy, dx));
            if (d <= budget) {
                c.setX(wp[0]);
                c.setY(wp[1]);
                moved += d;
                budget -= d;
                mem.pathIndex++;
            } else {
                double nx = c.x() + dx / d * budget;
                double ny = c.y() + dy / d * budget;
                c.setX(nx);
                c.setY(ny);
                moved += budget;
                budget = 0.0;
            }
        }

        if (mem.pathIndex >= mem.path.size()) {
            // Reached the end of the current path.
            mem.path = null;
        }
        return moved;
    }
}
