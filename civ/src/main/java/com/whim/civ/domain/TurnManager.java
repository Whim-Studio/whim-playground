package com.whim.civ.domain;

import java.util.List;

/**
 * Drives one civilization's turn through its phases and advances the year each full round
 * of all civs (Civ1-style).
 *
 * <p>Year progression breakpoints (per round, see {@link #advanceYear()}):
 * <pre>
 *   year &lt; -1000 : +50;  -1000..-1 : +25;  1..1000 : +20;  1000..1500 : +10;
 *   1500..1750 : +5;  1750..1850 : +2;  year &gt;= 1850 : +1.
 * </pre>
 * There is no year 0: if applying an increment lands exactly on 0, the year becomes 1
 * instead (the documented "-1 -&gt; +1" skip).
 */
public final class TurnManager {
    private final GameState state;
    private final EngineServices services;
    private TurnPhase phase = TurnPhase.UPKEEP;

    public TurnManager(GameState state, EngineServices services) {
        this.state = state;
        this.services = services;
    }

    public TurnPhase getPhase() { return phase; }

    /** UPKEEP for the active civ. */
    public void beginTurn() {
        Civilization civ = activeCiv();
        phase = TurnPhase.UPKEEP;
        services.runUpkeep(state, civ);
        if (!civ.isHuman()) {
            services.runAI(state, civ);
        }
        phase = TurnPhase.MOVEMENT;
    }

    /** Runs remaining phases, then advances to the next active civ (and the year on wrap). */
    public void endTurn() {
        Civilization civ = activeCiv();

        phase = TurnPhase.PRODUCTION;
        services.runProduction(state, civ);

        phase = TurnPhase.RESEARCH;
        services.runResearch(state, civ);

        phase = TurnPhase.END;

        List<Civilization> civs = state.getCivilizations();
        int next = (state.getActiveCivIndex() + 1) % civs.size();
        state.setActiveCivIndex(next);
        if (next == 0) {
            advanceYear();
            state.setTurnNumber(state.getTurnNumber() + 1);
        }
        phase = TurnPhase.UPKEEP;
    }

    /** Applies the year breakpoints above, skipping year 0. */
    public void advanceYear() {
        int year = state.getYear();
        int inc;
        if (year < -1000) {
            inc = 50;
        } else if (year < 0) {        // -1000 .. -1
            inc = 25;
        } else if (year < 1000) {     // 1 .. 1000 (year 0 never occurs)
            inc = 20;
        } else if (year < 1500) {     // 1000 .. 1500
            inc = 10;
        } else if (year < 1750) {     // 1500 .. 1750
            inc = 5;
        } else if (year < 1850) {     // 1750 .. 1850
            inc = 2;
        } else {                      // 1850+
            inc = 1;
        }
        int newYear = year + inc;
        if (newYear == 0) {
            newYear = 1;              // no year 0
        }
        state.setYear(newYear);
    }

    public boolean isHumanTurn() { return activeCiv().isHuman(); }

    private Civilization activeCiv() {
        return state.getCivilizations().get(state.getActiveCivIndex());
    }
}
