package com.whim.colony.engine;

import com.whim.colony.ColonyState;
import com.whim.colony.api.Action;
import com.whim.colony.domain.Colonist;

/**
 * The fallback job when all needs are satisfied: stroll to a nearby walkable
 * tile. Pure movement — there is no work action — so the job is complete as soon
 * as the colonist reaches the destination (its path empties).
 */
public final class IdleWanderJob extends AbstractColonyJob {

    public IdleWanderJob(int targetX, int targetY) {
        super("Wander", targetX, targetY);
    }

    @Override
    protected Action createWorkAction(ColonyState state, Colonist c) {
        return null; // nothing to do on arrival
    }

    @Override
    public boolean isComplete(ColonyState state, Colonist c) {
        // Done once we have arrived (no path left) at the wander target.
        return !c.hasPath() && c.getX() == getTargetX() && c.getY() == getTargetY();
    }
}
