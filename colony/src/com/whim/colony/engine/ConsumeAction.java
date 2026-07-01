package com.whim.colony.engine;

import com.whim.colony.ColonyState;
import com.whim.colony.api.Action;
import com.whim.colony.domain.Colonist;
import com.whim.colony.domain.Needs;

/**
 * Eats one meal: pulls a unit of {@link com.whim.colony.domain.Resources food}
 * from the shared stockpile and restores the colonist's hunger need over a small
 * number of ticks. If no food is available the colonist gnaws on nothing and the
 * action finishes without benefit (the storyteller will notice the starvation).
 */
public final class ConsumeAction implements Action {

    /** Food units eaten per meal. */
    private static final int FOOD_COST = 1;
    /** Hunger points restored per eating tick. */
    private static final double RESTORE_PER_TICK = 25.0;
    /** How many ticks a meal takes. */
    private static final int DURATION_TICKS = 4;

    private int ticksEaten = 0;
    private boolean paidForMeal = false;

    @Override
    public void perform(ColonyState state, Colonist c) {
        if (isFinished()) {
            return;
        }
        // Charge the stockpile once, on the first bite.
        if (!paidForMeal) {
            paidForMeal = true;
            if (!state.getResources().consumeFood(FOOD_COST)) {
                // Nothing to eat — abandon the meal immediately.
                ticksEaten = DURATION_TICKS;
                return;
            }
        }
        Needs needs = c.getNeeds();
        needs.setHunger(needs.getHunger() + RESTORE_PER_TICK);
        ticksEaten++;
    }

    @Override
    public boolean isFinished() {
        return ticksEaten >= DURATION_TICKS;
    }
}
