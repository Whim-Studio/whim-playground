package com.whim.colony.engine;

import com.whim.colony.ColonyState;
import com.whim.colony.api.Action;
import com.whim.colony.domain.Colonist;
import com.whim.colony.domain.Needs;

/**
 * Sleeps in place, restoring the colonist's rest need over a run of ticks. Unlike
 * {@link ConsumeAction} it costs no resources — just time — which is why a tired
 * colonist is unproductive until the nap completes.
 */
public final class RestAction implements Action {

    /** Rest points restored per sleeping tick. */
    private static final double RESTORE_PER_TICK = 12.0;
    /** How many ticks a full sleep takes. */
    private static final int DURATION_TICKS = 8;

    private int ticksSlept = 0;

    @Override
    public void perform(ColonyState state, Colonist c) {
        if (isFinished()) {
            return;
        }
        Needs needs = c.getNeeds();
        needs.setRest(needs.getRest() + RESTORE_PER_TICK);
        ticksSlept++;
    }

    @Override
    public boolean isFinished() {
        return ticksSlept >= DURATION_TICKS;
    }
}
