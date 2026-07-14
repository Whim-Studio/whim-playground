package com.whim.b5db.sim;

import com.whim.b5db.ai.Agent;
import com.whim.b5db.engine.GameEngine;
import com.whim.b5db.engine.GameResult;
import com.whim.b5db.engine.GameState;
import com.whim.b5db.engine.Seat;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless Monte-Carlo simulation harness. Runs many deterministic games and
 * collects {@link GameResult}s for the balance report. Each game {@code i} uses
 * seed {@code baseSeed + i}, so a whole batch is reproducible.
 */
public final class Simulator {

    private final GameEngine engine;
    private final List<Seat> seats;
    private final AgentFactory factory;

    public Simulator(GameEngine engine, List<Seat> seats, AgentFactory factory) {
        this.engine = engine;
        this.seats = seats;
        this.factory = factory;
    }

    /** Run {@code games} deterministic games and return their results. */
    public List<GameResult> run(int games, long baseSeed) {
        List<GameResult> results = new ArrayList<>(games);
        for (int i = 0; i < games; i++) {
            long seed = baseSeed + i;
            GameState state = engine.createGame(seats, seed);
            List<Agent> agents = new ArrayList<>();
            for (int s = 0; s < seats.size(); s++) {
                agents.add(factory.create(s, seats.get(s).faction, seed));
            }
            results.add(engine.run(state, agents));
        }
        return results;
    }
}
