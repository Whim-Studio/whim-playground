package com.whim.b5db.ai;

import com.whim.b5db.engine.GameEngine;
import com.whim.b5db.engine.GameState;
import com.whim.b5db.engine.PlayerState;
import com.whim.b5db.model.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Baseline agent: spends INFLUENCE on random affordable cards. Useful as the
 * rollout policy for {@link MonteCarloAgent} and as a control in balance runs.
 */
public final class RandomAgent implements Agent {

    private final Random random;

    public RandomAgent(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public List<Card> chooseAcquisitions(GameState state, PlayerState me, GameEngine engine) {
        List<Card> plan = new ArrayList<>();
        int budget = me.influence();
        List<Card> bought = new ArrayList<>();
        while (true) {
            List<Card> options = new ArrayList<>();
            for (Card c : engine.affordable(state, me)) {
                if (c.cost() <= budget && !bought.contains(c)) {
                    options.add(c);
                }
            }
            if (options.isEmpty()) {
                break;
            }
            Card pick = options.get(random.nextInt(options.size()));
            plan.add(pick);
            budget -= pick.cost();
            if (!pick.type().permanent() && pick.cost() == 0) {
                break; // avoid infinite loop on free non-unique picks
            }
            if (isUniqueRim(state, pick)) {
                bought.add(pick);
            }
        }
        return plan;
    }

    private boolean isUniqueRim(GameState state, Card c) {
        return state.market().rim().contains(c);
    }

    @Override
    public String label() {
        return "Random";
    }
}
