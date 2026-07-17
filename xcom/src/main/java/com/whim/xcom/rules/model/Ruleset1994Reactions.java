package com.whim.xcom.rules.model;

/**
 * 1994 reaction-fire model: {@code reactionScore = reactions × currentTU / maxTU};
 * a watcher interrupts a mover when its score is strictly greater.
 * Source: UFOpaedia "Reactions".
 */
public final class Ruleset1994Reactions implements ReactionModel {

    @Override
    public double reactionScore(int reactions, int currentTU, int maxTU) {
        if (maxTU <= 0) {
            return 0.0;
        }
        return reactions * (double) currentTU / (double) maxTU;
    }

    @Override
    public boolean triggers(int reactorReactions, int reactorCurrentTU, int reactorMaxTU,
                            int moverReactions, int moverCurrentTU, int moverMaxTU) {
        double reactor = reactionScore(reactorReactions, reactorCurrentTU, reactorMaxTU);
        double mover = reactionScore(moverReactions, moverCurrentTU, moverMaxTU);
        return reactor > mover;
    }
}
