package com.tiwas.mahjong.model;

/**
 * The scoring contract. Implementations compute base points, apply doubles, and
 * produce a final (limit-capped, rounded-down) score, recording the breakdown in
 * a {@link ScoreSheet}.
 */
public interface Scorable {

    /** Sum the base points for the hand's melds plus flowers/seasons and bonus. */
    int calculateBasePoints(Hand hand, ScoreSheet sheet);

    /** Apply the multiplicative doubles to the base, recording each on the sheet. */
    int applyDoubles(int basePoints, int doubles, ScoreSheet sheet);

    /** The final score after the limit cap and rounding down. */
    int getFinalScore(ScoreSheet sheet, int limit);
}
