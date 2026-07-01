package com.whim.colony.engine;

import com.whim.colony.ColonyState;
import com.whim.colony.api.Action;
import com.whim.colony.domain.Colonist;
import com.whim.colony.domain.Needs;

/**
 * Productive idle labour: the colonist walks to a stockpile and tidies/gathers,
 * yielding a trickle of wood and a small mood lift from useful work. Preferred
 * over aimless wandering when a stockpile exists, so the colony slowly builds up
 * a resource buffer between crises.
 */
public final class HaulJob extends AbstractColonyJob {

    public HaulJob(int targetX, int targetY) {
        super("Haul", targetX, targetY);
    }

    @Override
    protected Action createWorkAction(ColonyState state, Colonist c) {
        return new HaulAction();
    }

    @Override
    public boolean isComplete(ColonyState state, Colonist c) {
        return isWorkFinished();
    }

    /** A few ticks of hauling that deposits wood and nudges mood upward. */
    private static final class HaulAction implements Action {
        private static final int DURATION_TICKS = 5;
        private static final int WOOD_GAINED = 1;
        private static final double MOOD_BONUS = 4.0;

        private int ticks = 0;

        @Override
        public void perform(ColonyState state, Colonist c) {
            if (isFinished()) {
                return;
            }
            ticks++;
            if (ticks == DURATION_TICKS) {
                state.getResources().addWood(WOOD_GAINED);
                Needs needs = c.getNeeds();
                needs.setMood(needs.getMood() + MOOD_BONUS);
            }
        }

        @Override
        public boolean isFinished() {
            return ticks >= DURATION_TICKS;
        }
    }
}
