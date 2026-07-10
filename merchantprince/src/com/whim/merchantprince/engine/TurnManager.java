package com.whim.merchantprince.engine;

import com.whim.merchantprince.model.Family;
import com.whim.merchantprince.model.GameState;

/**
 * Drives one full turn (one year) of the game loop (ARCHITECTURE.md). The player
 * acts freely during their turn (trade, dispatch units, politics via the UI); this
 * manager resolves everything that happens when they end the turn:
 * rivals act, units advance & face hazards, world events fire, prices drift, the
 * year advances, and win/loss is checked.
 *
 * <p>Owned by the integration task (T5) — wiring here is finalised once the engines
 * are merged, but the ordering below is the contract every engine plugs into.
 */
public final class TurnManager {
    private TurnManager() { }

    public static void endTurn(GameState s, Rng rng) {
        if (s.gameOver) return;

        // 1. Rival families take their turns.
        for (Family f : s.families) {
            if (!f.human) AIPlayer.takeTurn(s, f, rng);
        }

        // 2. Units in transit advance one turn and face storms/piracy.
        TravelEngine.advanceUnits(s, rng);

        // 3. World events (plague, war, interdict, reformation).
        EventEngine.rollYearlyEvents(s, rng);

        // 4. Markets drift toward equilibrium with a yearly shock.
        PricingEngine.driftPrices(s, rng);

        // 5. Advance the calendar.
        s.year++;

        // 6. Resolve victory / end-of-era scoring.
        WinConditions.checkVictory(s);
    }
}
