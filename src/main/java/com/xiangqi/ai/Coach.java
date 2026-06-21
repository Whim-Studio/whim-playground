package com.xiangqi.ai;

import java.util.List;

import com.xiangqi.core.GameState;

/**
 * The educational tutor. Ranks the side-to-move's candidate moves and returns
 * them best-first with a plain-English explanation, for the UI's Coach mode.
 */
public interface Coach {

    /**
     * Return up to {@code n} of the strongest moves for {@code state.sideToMove()},
     * best first, each with a plain-English explanation. Never returns null
     * (returns an empty list when there are no legal moves).
     */
    List<MoveAdvice> topMoves(GameState state, int n);
}
