package com.whim.colony.engine;

import com.whim.colony.ColonyState;
import com.whim.colony.api.Action;
import com.whim.colony.domain.Colonist;
import com.whim.colony.domain.Needs;

/**
 * Drives a hungry colonist to a food source (a stockpile tile, or their own tile
 * if there is none) and eats. Complete once the colonist is comfortably fed or
 * the meal has run its course.
 */
public final class EatJob extends AbstractColonyJob {

    /** Hunger level at or above which the colonist stops eating. */
    private static final double SATED_LEVEL = 80.0;

    public EatJob(int targetX, int targetY) {
        super("Eat", targetX, targetY);
    }

    @Override
    protected Action createWorkAction(ColonyState state, Colonist c) {
        return new ConsumeAction();
    }

    @Override
    public boolean isComplete(ColonyState state, Colonist c) {
        Needs needs = c.getNeeds();
        return needs.getHunger() >= SATED_LEVEL || isWorkFinished();
    }
}
