package com.whim.civ.engine;

import com.whim.civ.domain.City;
import com.whim.civ.domain.Civilization;
import com.whim.civ.domain.EconomyView;
import com.whim.civ.domain.GameState;

/**
 * {@link EconomyView} adapter backed by {@link EconomyEngine}, so the UI (or Main) can display
 * engine-computed economy numbers through the domain interface without importing the engine.
 */
public final class EngineEconomyView implements EconomyView {

    private final EconomyEngine economy;

    public EngineEconomyView() {
        this(new EconomyEngine());
    }

    public EngineEconomyView(EconomyEngine economy) {
        this.economy = economy;
    }

    public int food(GameState s, City c) {
        return economy.computeFood(s, c);
    }

    public int shields(GameState s, City c) {
        return economy.computeShields(s, c);
    }

    public int trade(GameState s, City c) {
        return economy.computeTrade(s, c);
    }

    public int[] tradeSplit(Civilization civ, int totalTrade) {
        return economy.splitTrade(civ, totalTrade);
    }
}
