package com.whim.colony.api;

import com.whim.colony.ColonyState;
import com.whim.colony.domain.Colonist;

/**
 * A single executable step towards completing a {@link Job} (e.g. "take one step
 * along the path", "apply one tick of construction work"). The engine (Task 2)
 * calls {@link #perform} each tick until {@link #isFinished()} reports true, then
 * moves on to the next action.
 */
public interface Action {

    /**
     * Execute one step of this action, mutating {@code state} and/or {@code c}
     * as appropriate (moving the colonist, decrementing work remaining, etc.).
     *
     * @param state the shared colony state to mutate
     * @param c     the colonist carrying out the action
     */
    void perform(ColonyState state, Colonist c);

    /**
     * @return true once this action has run to completion and should not be
     * performed again. The engine uses this to advance to the next step.
     */
    boolean isFinished();
}
