package com.whim.babylon5.engine;

/**
 * The three AI skill tiers. Each tier maps to a distinct decision strategy inside
 * {@link AIPlayer}: EASY is greedy/random, MEDIUM is heuristic, HARD adds one-ply
 * look-ahead with a board evaluation.
 */
public enum AiDifficulty {
    EASY, MEDIUM, HARD
}
