package com.whim.populous.engine;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.domain.GameState;

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
    boolean check(GameState gs) {
        if (gs.gameOver()) {
            return true;
        }
        int good = EngineSupport.livePopulation(gs, Allegiance.GOOD);
        int evil = EngineSupport.livePopulation(gs, Allegiance.EVIL);

        // Don't declare victory before anyone has spawned.
        if (good == 0 && evil == 0) {
            return false;
        }
        if (evil == 0) {
            gs.setGameOver(true);
            gs.setWinner(Allegiance.GOOD);
            return true;
        }
        if (good == 0) {
            gs.setGameOver(true);
            gs.setWinner(Allegiance.EVIL);
            return true;
        }
        return false;
    }
}
