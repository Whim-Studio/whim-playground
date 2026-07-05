package com.whim.kenshi.engine;

import java.util.List;

/**
 * Engine-side per-character scratch state that does NOT belong in the domain:
 * spawn/home position, wander/patrol bookkeeping, the current movement goal, an
 * active pathfinder waypoint list, and the current combat target id. Keyed by
 * character id inside {@link GameEngine} and confined to the tick thread.
 */
final class AiMemory {

    double homeX;
    double homeY;

    /** World-seconds remaining before the next wander re-roll. */
    double wanderTimer;

    /** Active movement goal in world units (valid only when {@link #hasGoal}). */
    double goalX;
    double goalY;
    boolean hasGoal;

    /** Cached waypoint path toward the goal and the goal it was computed for. */
    List<double[]> path;
    double pathGoalX;
    double pathGoalY;
    int pathIndex;

    /** Current combat target character id, or null. */
    String combatTargetId;

    /** Patrol waypoints for guards (world units), and the current leg. */
    double[][] patrol;
    int patrolIndex;

    AiMemory(double homeX, double homeY) {
        this.homeX = homeX;
        this.homeY = homeY;
    }

    void clearGoal() {
        hasGoal = false;
        path = null;
        pathIndex = 0;
    }
}
