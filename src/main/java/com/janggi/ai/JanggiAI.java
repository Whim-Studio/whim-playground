package com.janggi.ai;

import com.janggi.core.GameState;
import com.janggi.core.Move;

/**
 * A Janggi move-selection strategy.
 *
 * <p>Implementations choose a move for {@code state.sideToMove()}. The returned
 * move MUST be legal for that side (it may be a pass — {@code new Move(p, p)} —
 * if a pass is the only or best legal option).
 */
public interface JanggiAI {

    /**
     * Choose a move for {@code state.sideToMove()}.
     *
     * @param state the current game state (never mutated by the AI)
     * @return a legal move for the side to move
     */
    Move chooseMove(GameState state);
}
