package com.xiangqi.ai;

import com.xiangqi.core.GameState;
import com.xiangqi.core.Move;

/**
 * A computer player for Xiangqi (Chinese Chess).
 *
 * <p>Implementations choose a move for {@code state.sideToMove()}. The returned
 * move is always one of {@code state.legalMoves()}.
 */
public interface XiangqiAI {

    /** Choose a legal move for {@code state.sideToMove()}. */
    Move chooseMove(GameState state);
}
