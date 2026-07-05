package com.whim.populous.engine;

import com.whim.populous.domain.GameState;
import com.whim.populous.domain.GameStateManager;

/**
 * Accrues mana for both sides once per tick. The exact weighting (sum of each
 * side's settlement mana weights, clamped to {@code maxMana}) is owned by the
 * domain via {@link GameStateManager#accrueMana(GameState)}; the engine simply
 * drives it on the simulation thread so the accrual policy stays in one place.
 */
final class ManaSystem {

    void accrue(GameStateManager manager, GameState gs) {
        manager.accrueMana(gs);
    }
}
