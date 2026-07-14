package com.whim.b5db.ai;

import com.whim.b5db.engine.GameEngine;
import com.whim.b5db.engine.GameState;
import com.whim.b5db.engine.PlayerState;
import com.whim.b5db.model.Card;
import com.whim.b5db.model.Effect;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based agent implementing the EASY / NORMAL / HARD difficulty tiers.
 * Greedily buys the highest-scoring affordable card until INFLUENCE runs out.
 * Higher tiers value prestige, economy, and same-faction Ally synergy more
 * sharply; EASY plays loosely and undervalues long-term engine building.
 */
public final class HeuristicAgent implements Agent {

    public enum Difficulty {EASY, NORMAL, HARD}

    private final Difficulty difficulty;

    public HeuristicAgent(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    @Override
    public List<Card> chooseAcquisitions(GameState state, PlayerState me, GameEngine engine) {
        List<Card> plan = new ArrayList<>();
        int budget = me.influence();
        List<Card> takenRim = new ArrayList<>();
        while (true) {
            Card best = null;
            double bestScore = bestScoreThreshold();
            for (Card c : engine.affordable(state, me)) {
                if (c.cost() > budget) {
                    continue;
                }
                if (state.market().rim().contains(c) && takenRim.contains(c)) {
                    continue;
                }
                double score = score(c, me);
                if (score > bestScore) {
                    bestScore = score;
                    best = c;
                }
            }
            if (best == null) {
                break;
            }
            plan.add(best);
            budget -= best.cost();
            if (state.market().rim().contains(best)) {
                takenRim.add(best);
            }
        }
        return plan;
    }

    /** EASY skips marginal buys; higher tiers snap up anything positive. */
    private double bestScoreThreshold() {
        return difficulty == Difficulty.EASY ? 1.0 : 0.0;
    }

    /** Score a candidate purchase. Higher is better. */
    private double score(Card c, PlayerState me) {
        double factionWeight = difficulty == Difficulty.HARD ? 3.0
                : difficulty == Difficulty.NORMAL ? 1.5 : 0.5;
        double prestigeWeight = difficulty == Difficulty.HARD ? 6.0
                : difficulty == Difficulty.NORMAL ? 5.0 : 3.0;

        double s = c.prestige() * prestigeWeight;
        s += c.totalAttributes();
        s += economyValue(c) * 2.0;
        if (c.faction() == me.faction()) {
            s += factionWeight;
        }
        s -= c.cost() * 0.5; // mild efficiency preference
        return s;
    }

    private int economyValue(Card c) {
        int v = 0;
        for (Effect e : c.effects()) {
            if (e.type() == Effect.Type.GAIN_INFLUENCE) {
                v += e.amount();
            }
        }
        return v;
    }

    @Override
    public String label() {
        return "Heuristic-" + difficulty;
    }
}
