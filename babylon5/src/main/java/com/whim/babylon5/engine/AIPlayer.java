package com.whim.babylon5.engine;

import java.util.ArrayList;
import java.util.List;

import com.whim.babylon5.domain.Card;
import com.whim.babylon5.domain.CardType;
import com.whim.babylon5.domain.ConflictType;
import com.whim.babylon5.domain.GameState;
import com.whim.babylon5.domain.PlayerState;
import com.whim.babylon5.domain.ZoneType;

/**
 * Single-player opponent brain with three distinct skill tiers.
 *
 * <p>{@link AiDifficulty#EASY} is greedy/random (cheap, shallow). {@link AiDifficulty#MEDIUM}
 * scores candidates with a value-per-influence heuristic. {@link AiDifficulty#HARD} adds a
 * one-ply look-ahead: it simulates each candidate decision against a static board evaluation
 * and picks the move that maximises projected standing.
 *
 * <p>The class is stateless apart from its difficulty, so a single instance is reusable and
 * thread-safe; all game context arrives via {@link GameState} parameters. It performs no
 * mutation &mdash; it only <i>recommends</i> moves; {@link GameEngine} validates and applies them.
 */
public final class AIPlayer {

    private final AiDifficulty difficulty;

    public AIPlayer(AiDifficulty difficulty) {
        this.difficulty = difficulty == null ? AiDifficulty.EASY : difficulty;
    }

    public AiDifficulty getDifficulty() {
        return difficulty;
    }

    // ------------------------------------------------------------------ sponsoring (required)

    /**
     * The required, fully-implemented demonstrated decision: pick the best CHARACTER in hand to
     * sponsor this turn, or null to sponsor nothing.
     *
     * <p>Common gate (all tiers): the candidate must be a CHARACTER the faction can actually
     * afford after loyalty doubling, and the faction must have spare influence. The tiers then
     * diverge in how they choose among the affordable candidates.
     */
    public Card chooseCharacterToSponsor(GameState state, int playerIndex) {
        PlayerState me = state.getPlayers().get(playerIndex);
        List<Card> affordable = affordableCharacters(me);
        if (affordable.isEmpty()) {
            return null;
        }
        switch (difficulty) {
            case EASY:
                // Greedy/random: grab a random affordable character.
                return affordable.get(state.getRng().nextInt(affordable.size()));
            case MEDIUM:
                // Heuristic: best total ability per influence spent.
                return bestByValuePerInfluence(me, affordable);
            case HARD:
            default:
                // Look-ahead: maximise a board-standing evaluation after the sponsor.
                return bestByLookahead(state, playerIndex, affordable);
        }
    }

    private List<Card> affordableCharacters(PlayerState me) {
        List<Card> out = new ArrayList<Card>();
        int pool = me.getInfluencePool();
        for (Card c : me.zone(ZoneType.HAND).getCards()) {
            if (c.getType() != CardType.CHARACTER) {
                continue;
            }
            if (cost(me, c) <= pool) {
                out.add(c);
            }
        }
        return out;
    }

    private Card bestByValuePerInfluence(PlayerState me, List<Card> affordable) {
        Card best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Card c : affordable) {
            int spent = Math.max(1, cost(me, c));
            double score = (double) totalAbility(c) / spent;
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return best;
    }

    /**
     * One-ply look-ahead: for each affordable candidate, evaluate the resulting board standing
     * (added board strength, weighted by raw ability, minus influence committed) and keep the best.
     */
    private Card bestByLookahead(GameState state, int playerIndex, List<Card> affordable) {
        PlayerState me = state.getPlayers().get(playerIndex);
        Card best = null;
        double bestEval = Double.NEGATIVE_INFINITY;
        for (Card c : affordable) {
            double eval = evaluateSponsor(state, playerIndex, c);
            if (eval > bestEval) {
                bestEval = eval;
                best = c;
            }
        }
        // Decline if even the best candidate looks worse than banking the influence early.
        if (best != null && evaluateSponsor(state, playerIndex, best) < 0 && me.getInfluenceRating() < 9) {
            return null;
        }
        return best;
    }

    /** Static evaluation of sponsoring {@code c}: board strength gained minus the influence cost. */
    private double evaluateSponsor(GameState state, int playerIndex, Card c) {
        PlayerState me = state.getPlayers().get(playerIndex);
        double abilityValue = totalAbility(c);
        double versatility = nonZeroAbilityCount(c); // flexible characters cover more conflict types
        double spent = cost(me, c);
        return abilityValue + 0.5 * versatility - 0.6 * spent;
    }

    // ------------------------------------------------------------------ declaring conflicts

    /**
     * Decide whether to initiate a conflict this turn and, if so, of which type and against whom.
     * Returns null to decline (a valid, common choice early in the game per the rulebook tip).
     */
    public Conflict chooseConflict(GameState state, int playerIndex) {
        PlayerState me = state.getPlayers().get(playerIndex);
        ConflictType bestType = strongestType(me);
        int myStrength = readyStrength(me, bestType);
        if (myStrength <= 0) {
            return null; // nothing to fight with
        }

        if (difficulty == AiDifficulty.EASY) {
            // Random/greedy: ~half the time pick a random valid target and swing.
            if (state.getRng().nextBoolean()) {
                return null;
            }
            int target = randomOpponent(state, playerIndex);
            return target < 0 ? null : new Conflict(playerIndex, bestType, target);
        }

        // MEDIUM/HARD: target the opponent we can most likely beat in our strongest discipline.
        int target = -1;
        int bestDefenseGap = Integer.MIN_VALUE;
        for (int i = 0; i < state.getPlayers().size(); i++) {
            if (i == playerIndex) {
                continue;
            }
            int defense = readyStrength(state.getPlayers().get(i), bestType);
            int gap = myStrength - defense;
            if (gap > bestDefenseGap) {
                bestDefenseGap = gap;
                target = i;
            }
        }
        if (target < 0) {
            return null;
        }
        // HARD only commits when it projects a strict win (support must exceed opposition);
        // MEDIUM is willing to contest an even matchup.
        if (difficulty == AiDifficulty.HARD && bestDefenseGap <= 0) {
            return null;
        }
        if (difficulty == AiDifficulty.MEDIUM && bestDefenseGap < 0) {
            return null;
        }
        return new Conflict(playerIndex, bestType, target);
    }

    // ------------------------------------------------------------------ committing cards

    /**
     * Decide whether to rotate {@code c} into a pending conflict (as support if {@code asSupport},
     * else as opposition). EASY commits anything that can help; MEDIUM/HARD require a meaningful
     * contribution so they keep blockers in reserve.
     */
    public boolean willCommit(Card c, Conflict pending, boolean asSupport) {
        if (pending == null || c == null || !c.isReady()) {
            return false;
        }
        int ability = Math.max(0, c.support(pending.getType()) - c.getDamage());
        if (ability <= 0) {
            return false; // cannot contribute to this conflict type
        }
        switch (difficulty) {
            case EASY:
                return true; // throw everyone in
            case MEDIUM:
                return ability >= 1;
            case HARD:
            default:
                // Spend the strong cards; hold near-zero contributors back as reserve.
                return ability >= 2 || asSupport;
        }
    }

    // ------------------------------------------------------------------ helpers

    private int cost(PlayerState me, Card c) {
        int cost = c.getCost();
        if (c.getFaction() != me.getFaction()) {
            cost *= 2; // loyalty doubling for a different-race character
        }
        return cost;
    }

    private int totalAbility(Card c) {
        return c.getDiplomacy() + c.getIntrigue() + c.getPsi() + c.getMilitary();
    }

    private int nonZeroAbilityCount(Card c) {
        int n = 0;
        if (c.getDiplomacy() > 0) n++;
        if (c.getIntrigue() > 0) n++;
        if (c.getPsi() > 0) n++;
        if (c.getMilitary() > 0) n++;
        return n;
    }

    /** The conflict type in which this faction's ready cards are collectively strongest. */
    private ConflictType strongestType(PlayerState me) {
        ConflictType best = ConflictType.DIPLOMACY;
        int bestVal = -1;
        for (ConflictType t : ConflictType.values()) {
            int v = readyStrength(me, t);
            if (v > bestVal) {
                bestVal = v;
                best = t;
            }
        }
        return best;
    }

    /** Sum of ready, un-damaged-out ability for the given conflict type across a faction's cards. */
    private int readyStrength(PlayerState p, ConflictType t) {
        int total = 0;
        for (ZoneType zt : new ZoneType[] { ZoneType.INNER_CIRCLE, ZoneType.SUPPORTING }) {
            for (Card c : p.zone(zt).getCards()) {
                if (c.isReady()) {
                    total += Math.max(0, c.support(t) - c.getDamage());
                }
            }
        }
        return total;
    }

    private int randomOpponent(GameState state, int playerIndex) {
        int n = state.getPlayers().size();
        if (n <= 1) {
            return -1;
        }
        int offset = 1 + state.getRng().nextInt(n - 1);
        return (playerIndex + offset) % n;
    }
}
