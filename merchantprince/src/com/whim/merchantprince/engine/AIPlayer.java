package com.whim.merchantprince.engine;

import com.whim.merchantprince.model.Family;
import com.whim.merchantprince.model.GameState;

/**
 * Heuristic AI for the three rival families (GAME_DESIGN_REFERENCE §1). Each rival
 * takes a turn: trade for profit, dispatch idle units, and occasionally invest in
 * politics. Deliberately simple but non-trivial so rivals grow their net worth.
 *
 * <p>Contract frozen for T0. Full heuristics to be completed by the Politics task (T2).
 */
public final class AIPlayer {
    private AIPlayer() { }

    /** Run one rival family's turn. */
    public static void takeTurn(GameState s, Family f, Rng rng) {
        // TODO(T2): buy-low/sell-high routing for the family's units, dispatch idle
        // ships, and opportunistic bribery. Keep it lightweight.
        if (f.human || f.eliminated) return;
        // Placeholder: rivals earn a modest trading income so the score board moves.
        f.florins += rng.range(200, 600);
    }
}
