package com.whim.b5db;

import com.whim.b5db.ai.HeuristicAgent;
import com.whim.b5db.app.Catalog;
import com.whim.b5db.engine.GameConfig;
import com.whim.b5db.engine.GameEngine;
import com.whim.b5db.engine.GameResult;
import com.whim.b5db.engine.Seat;
import com.whim.b5db.model.Faction;
import com.whim.b5db.sim.AgentFactory;
import com.whim.b5db.sim.BalanceReport;
import com.whim.b5db.sim.Simulator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Integration test: run 100 deterministic headless games end-to-end. */
public class SimulationIT {

    @Test
    public void runsOneHundredGames() {
        Catalog catalog = Catalog.load(null); // bundled cards only
        assertTrue("catalogue should load bundled cards", catalog.cards().size() >= 20);

        GameEngine engine = new GameEngine(catalog.cards(), new GameConfig(40));
        List<Seat> seats = new ArrayList<>();
        Faction[] pool = Faction.playable();
        for (int i = 0; i < 4; i++) {
            seats.add(new Seat(pool[i].display(), pool[i], true));
        }
        AgentFactory factory = (seatIndex, faction, gameSeed) ->
                new HeuristicAgent(HeuristicAgent.Difficulty.HARD);

        List<GameResult> results = new Simulator(engine, seats, factory).run(100, 2026L);
        assertEquals(100, results.size());
        for (GameResult r : results) {
            assertNotNull(r.winnerFaction());
            assertTrue("game should terminate within the turn cap", r.turns() <= 200);
        }

        BalanceReport report = new BalanceReport(results);
        assertEquals(100, report.games());
        // Determinism: identical batch reproduces the same first winner.
        List<GameResult> again = new Simulator(engine, seats, factory).run(100, 2026L);
        assertEquals(results.get(0).winnerFaction(), again.get(0).winnerFaction());
    }
}
