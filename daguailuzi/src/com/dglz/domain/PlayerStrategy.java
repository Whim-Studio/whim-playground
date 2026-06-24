package com.dglz.domain;

import com.dglz.engine.GameState;

/** AI strategy. Implemented by Task 2. */
public interface PlayerStrategy {
    /**
     * Return the combination this seat plays, or null to PASS. Must be legal for the
     * current trick (engine re-validates).
     */
    Combination decideMove(GameState state, int seat);
}
