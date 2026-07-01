package com.whim.colony.engine;

import com.whim.colony.ColonyState;
import com.whim.colony.api.Event;

/**
 * A supply pod plummets from orbit, adding food, steel and wood to the colony's
 * stockpile. The colony's small good-news event, sized by severity.
 */
public final class ResourceDropEvent implements Event {

    private final int food;
    private final int steel;
    private final int wood;

    public ResourceDropEvent(int food, int steel, int wood) {
        this.food = Math.max(0, food);
        this.steel = Math.max(0, steel);
        this.wood = Math.max(0, wood);
    }

    @Override
    public void apply(ColonyState state) {
        state.getResources().addFood(food);
        state.getResources().addSteel(steel);
        state.getResources().addWood(wood);
        state.addMessage(describe());
    }

    @Override
    public String describe() {
        return "Supply pod! +" + food + " food, +" + steel + " steel, +" + wood + " wood.";
    }
}
