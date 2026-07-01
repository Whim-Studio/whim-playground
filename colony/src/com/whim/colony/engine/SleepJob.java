package com.whim.colony.engine;

import com.whim.colony.ColonyState;
import com.whim.colony.api.Action;
import com.whim.colony.domain.Colonist;
import com.whim.colony.domain.Needs;

/**
 * Drives a tired colonist to a bed (or their own tile if there is none) and
 * sleeps. Complete once the colonist is rested or the nap has finished.
 */
public final class SleepJob extends AbstractColonyJob {

    /** Rest level at or above which the colonist wakes up. */
    private static final double RESTED_LEVEL = 85.0;

    public SleepJob(int targetX, int targetY) {
        super("Sleep", targetX, targetY);
    }

    @Override
    protected Action createWorkAction(ColonyState state, Colonist c) {
        return new RestAction();
    }

    @Override
    public boolean isComplete(ColonyState state, Colonist c) {
        Needs needs = c.getNeeds();
        return needs.getRest() >= RESTED_LEVEL || isWorkFinished();
    }
}
