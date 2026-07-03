package com.whim.cardwoven.engine;

/**
 * Cumulative per-player tallies the live domain model does not itself retain,
 * but which victory tracking needs across turns (e.g. total raiders crushed).
 * Shared by {@link CombatResolver} (writer) and {@link VictoryMonitor} (reader).
 */
final class EngineStats {
    final int[] raidersDefeated;
    final int[] buildingsDestroyed;

    EngineStats(int playerCount) {
        this.raidersDefeated = new int[playerCount];
        this.buildingsDestroyed = new int[playerCount];
    }

    /** Total military "kills" (raiders + rival buildings) for a player. */
    int militaryScore(int playerIndex) {
        return raidersDefeated[playerIndex] + buildingsDestroyed[playerIndex];
    }
}
