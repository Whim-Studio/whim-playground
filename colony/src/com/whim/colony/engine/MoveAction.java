package com.whim.colony.engine;

import com.whim.colony.ColonyState;
import com.whim.colony.api.Action;
import com.whim.colony.domain.Colonist;

import java.util.List;

/**
 * Walks a colonist one tile per {@link #perform} along the path already planted
 * in {@link Colonist#getPath()} by the engine's {@link Pathfinder}. The action
 * is finished once the path has been fully consumed (the colonist has arrived).
 *
 * <p>This action is deliberately dumb: it does not plan. Path planning is the
 * {@link Pathfinder}'s job; this just executes the plan a step at a time so the
 * simulation stays decoupled from frame rate (one tile of movement per tick).
 */
public final class MoveAction implements Action {

    @Override
    public void perform(ColonyState state, Colonist c) {
        List<int[]> path = c.getPath();
        if (path.isEmpty()) {
            return;
        }
        int[] step = path.remove(0);
        // Only step onto tiles that are still walkable; if the world changed
        // under us (a wall appeared), abandon the stale path.
        if (state.getMap().inBounds(step[0], step[1])
                && state.getMap().getTile(step[0], step[1]).isWalkable()) {
            c.setPosition(step[0], step[1]);
        } else {
            path.clear();
        }
    }

    @Override
    public boolean isFinished() {
        // Finished state is derived from the colonist's path each tick by the
        // owning job; a MoveAction with an empty path has nothing left to do.
        return true;
    }
}
