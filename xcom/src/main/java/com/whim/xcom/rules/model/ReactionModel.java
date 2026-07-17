package com.whim.xcom.rules.model;

/**
 * Reaction-fire arbitration. In 1994 a watching unit may interrupt a moving
 * enemy when its <em>reaction score</em> exceeds the mover's:
 *
 * <pre>reactionScore = reactions × currentTU / maxTU</pre>
 */
public interface ReactionModel {

    /** @return the reaction score {@code reactions × currentTU / maxTU}. */
    double reactionScore(int reactions, int currentTU, int maxTU);

    /**
     * @return {@code true} if the reactor interrupts the mover — i.e. the
     *         reactor's score is strictly greater than the mover's.
     */
    boolean triggers(int reactorReactions, int reactorCurrentTU, int reactorMaxTU,
                     int moverReactions, int moverCurrentTU, int moverMaxTU);
}
