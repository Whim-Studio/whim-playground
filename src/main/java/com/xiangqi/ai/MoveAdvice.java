package com.xiangqi.ai;

import com.xiangqi.core.Move;

/**
 * A single ranked recommendation from the {@link Coach}: a move, its evaluation
 * for the advised side (centipoints), and a plain-English explanation.
 */
public final class MoveAdvice {

    private final Move move;
    private final int score;
    private final String explanation;

    public MoveAdvice(Move move, int score, String explanation) {
        this.move = move;
        this.score = score;
        this.explanation = explanation;
    }

    public Move move() {
        return move;
    }

    /** Evaluation of this move for the advised side (centipoints). */
    public int score() {
        return score;
    }

    /** Plain-English reason, e.g. "Moving your Cannon to (2,7) forks the Chariot and Horse." */
    public String explanation() {
        return explanation;
    }

    @Override
    public String toString() {
        return "MoveAdvice{" + move + ", score=" + score + ", \"" + explanation + "\"}";
    }
}
