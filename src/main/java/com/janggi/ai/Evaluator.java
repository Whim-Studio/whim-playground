package com.janggi.ai;

import com.janggi.core.GameState;
import com.janggi.core.Side;

/**
 * Static evaluation function for a Janggi position.
 *
 * <p>The score is returned from {@code perspective}'s point of view: a higher
 * number means the position is better for {@code perspective}. By convention an
 * evaluator is roughly symmetric, i.e. {@code evaluate(s, side)} and
 * {@code evaluate(s, side.opponent())} sum to approximately zero (modulo a
 * side-to-move bonus).
 */
public interface Evaluator {

    /**
     * Score {@code state} from {@code perspective}'s point of view.
     *
     * @param state       the position to score
     * @param perspective the side whose advantage the score reflects
     * @return centipawn-style score; higher is better for {@code perspective}
     */
    int evaluate(GameState state, Side perspective);
}
