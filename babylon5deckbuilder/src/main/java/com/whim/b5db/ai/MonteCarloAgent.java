package com.whim.b5db.ai;

import com.whim.b5db.engine.GameEngine;
import com.whim.b5db.engine.GameState;
import com.whim.b5db.engine.PlayerState;
import com.whim.b5db.engine.Rng;
import com.whim.b5db.model.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * Monte-Carlo playout agent. For the first purchase of the turn it evaluates
 * each affordable card (and "buy nothing") by cloning the game, applying that
 * purchase, and rolling the rest of the game out under a random policy several
 * times; it keeps the option with the best average PRESTIGE margin. Remaining
 * INFLUENCE is then spent by a NORMAL heuristic. This is the strongest bundled
 * AI and the reference opponent for the simulation harness.
 */
public final class MonteCarloAgent implements Agent {

    private final int rollouts;
    private final long baseSeed;
    private final HeuristicAgent topUp = new HeuristicAgent(HeuristicAgent.Difficulty.NORMAL);

    public MonteCarloAgent(int rollouts, long baseSeed) {
        this.rollouts = Math.max(1, rollouts);
        this.baseSeed = baseSeed;
    }

    @Override
    public List<Card> chooseAcquisitions(GameState state, PlayerState me, GameEngine engine) {
        int myIndex = state.players().indexOf(me);

        List<Card> candidates = new ArrayList<>();
        for (Card c : engine.affordable(state, me)) {
            if (!candidates.contains(c)) {
                candidates.add(c);
            }
        }

        Card best = null;
        double bestEv = evaluate(state, engine, myIndex, null); // baseline: buy nothing
        for (Card cand : candidates) {
            double ev = evaluate(state, engine, myIndex, cand);
            if (ev > bestEv) {
                bestEv = ev;
                best = cand;
            }
        }

        List<Card> plan = new ArrayList<>();
        if (best != null) {
            plan.add(best);
        }
        // Spend whatever remains with the heuristic, using a shallow copy so we
        // do not disturb the live state while planning.
        plan.addAll(topUpPlan(state, me, engine, best));
        return plan;
    }

    private double evaluate(GameState state, GameEngine engine, int myIndex, Card candidate) {
        double sum = 0.0;
        for (int r = 0; r < rollouts; r++) {
            long seed = baseSeed * 1_000_003L + (candidate == null ? 7 : candidate.id().hashCode()) + r;
            Rng rng = new Rng(seed);
            GameState sim = state.copy(rng);
            PlayerState simMe = sim.players().get(myIndex);
            if (candidate != null) {
                engine.purchase(sim, simMe, candidate);
            }
            engine.concludeTurn(sim);

            List<Agent> rollAgents = new ArrayList<>();
            for (int p = 0; p < sim.players().size(); p++) {
                rollAgents.add(new RandomAgent(seed + 31L * p));
            }
            engine.run(sim, rollAgents);
            sum += simMe.prestige() - bestOther(sim, myIndex);
        }
        return sum / rollouts;
    }

    private int bestOther(GameState sim, int myIndex) {
        int best = 0;
        for (int i = 0; i < sim.players().size(); i++) {
            if (i == myIndex) {
                continue;
            }
            best = Math.max(best, sim.players().get(i).prestige());
        }
        return best;
    }

    /** Heuristic top-up for leftover budget, computed on a clone. */
    private List<Card> topUpPlan(GameState state, PlayerState me, GameEngine engine, Card alreadyChosen) {
        GameState sim = state.copy(new Rng(baseSeed + 99L));
        int myIndex = state.players().indexOf(me);
        PlayerState simMe = sim.players().get(myIndex);
        if (alreadyChosen != null) {
            engine.purchase(sim, simMe, alreadyChosen);
        }
        return topUp.chooseAcquisitions(sim, simMe, engine);
    }

    @Override
    public String label() {
        return "MonteCarlo(" + rollouts + ")";
    }
}
