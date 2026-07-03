package com.whim.populous.engine;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.domain.GameStateManager;

/**
 * Watches populations and declares a winner the moment one side is wiped out.
 * Populous is won by GOOD when EVIL population hits 0 (and vice-versa). When a
 * result is reached it flags the state as over; the engine stops the loop.
 */
final class VictoryMonitor {

    /**
     * @return true if the game just ended (or is already over), so the caller
     *         can halt the simulation loop.
     */
    boolean check(GameStateManager mgr) {
        if (mgr.isGameOver()) {
            return true;
        }
        int good = mgr.population(Allegiance.GOOD);
        int evil = mgr.population(Allegiance.EVIL);

        // Don't declare victory on tick 0 before anyone has spawned.
        if (good == 0 && evil == 0) {
            return false;
        }
        if (evil == 0) {
            mgr.setGameOver(Allegiance.GOOD);
            return true;
        }
        if (good == 0) {
            mgr.setGameOver(Allegiance.EVIL);
            return true;
        }
        return false;
    }
}
