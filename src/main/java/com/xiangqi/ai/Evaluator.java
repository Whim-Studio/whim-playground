package com.xiangqi.ai;

import com.xiangqi.core.GameState;
import com.xiangqi.core.Side;

/**
 * Static evaluation of a Xiangqi position.
 *
 * <p>The score is returned from {@code perspective}'s point of view in
 * centipoints, where higher is better for {@code perspective}. A symmetric
 * evaluator satisfies {@code evaluate(s, side) == -evaluate(s, side.opponent())}.
 */
public interface Evaluator {

    /** Score {@code state} from {@code perspective}'s point of view (centipoints). */
    int evaluate(GameState state, Side perspective);
}
