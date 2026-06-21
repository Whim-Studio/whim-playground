package com.whim.civ.engine;

import com.whim.civ.domain.Building;
import com.whim.civ.domain.City;
import com.whim.civ.domain.Civilization;
import com.whim.civ.domain.EconomyView;
import com.whim.civ.domain.EngineServices;
import com.whim.civ.domain.GameState;

import java.util.Random;

/**
 * Top-level engine wiring the sub-engines together and implementing the domain
 * {@link EngineServices} bridge the turn loop calls into.
 */
public final class GameEngine implements EngineServices {

    private final EconomyEngine economy;
    private final ResearchEngine research;
    private final CombatResolver combat;
    private final AIController ai;

    public GameEngine() {
        this(new Random());
    }

    /** Seedable constructor for deterministic AI/combat behaviour in tests. */
    public GameEngine(Random rng) {
        this.economy = new EconomyEngine();
        this.research = new ResearchEngine();
        this.combat = new CombatResolver(rng);
        this.ai = new AIController(rng);
    }

    // --- EngineServices -----------------------------------------------------

    /** Food growth, gold upkeep/income and the civil-disorder flag for every city. */
    public void runUpkeep(GameState state, Civilization civ) {
        if (!civ.isAlive()) {
            return;
        }
        for (City c : state.citiesOf(civ.getId())) {
            boolean disorder = economy.computeDisorder(state, c);
            c.setInDisorder(disorder);

            // A city in disorder produces no growth.
            if (!disorder) {
                economy.grow(state, c);
            }

            int upkeep = 0;
            for (Building b : c.getBuildings()) {
                upkeep += b.getUpkeep();
            }

            int trade = economy.computeTrade(state, c);
            int tax = economy.splitTrade(civ, trade)[0];
            civ.setTreasury(civ.getTreasury() + tax - upkeep);
        }
    }

    /** Convert accumulated shields into the queued unit or building (skipped in disorder). */
    public void runProduction(GameState state, Civilization civ) {
        if (!civ.isAlive()) {
            return;
        }
        for (City c : state.citiesOf(civ.getId())) {
            if (!c.isInDisorder()) {
                economy.produce(state, c);
            }
        }
    }

    /** Sum science across the civ's cities and advance research. */
    public void runResearch(GameState state, Civilization civ) {
        if (!civ.isAlive()) {
            return;
        }
        int science = 0;
        for (City c : state.citiesOf(civ.getId())) {
            int trade = economy.computeTrade(state, c);
            science += economy.splitTrade(civ, trade)[1];
        }
        research.advance(state, civ, science);
    }

    /** Run the rival AI for non-human civilizations. */
    public void runAI(GameState state, Civilization civ) {
        if (civ.isHuman() || !civ.isAlive()) {
            return;
        }
        ai.takeTurn(state, civ);
    }

    // --- Accessors for Main / UI -------------------------------------------

    public EconomyEngine economyEngine() {
        return economy;
    }

    public ResearchEngine researchEngine() {
        return research;
    }

    public CombatResolver combatResolver() {
        return combat;
    }

    /** An {@link EconomyView} adapter the UI/Main can use without importing the engine. */
    public EconomyView economyView() {
        return new EngineEconomyView(economy);
    }
}
