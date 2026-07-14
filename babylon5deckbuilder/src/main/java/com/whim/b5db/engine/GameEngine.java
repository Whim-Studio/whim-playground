package com.whim.b5db.engine;

import com.whim.b5db.ai.Agent;
import com.whim.b5db.model.Card;
import com.whim.b5db.model.CardType;
import com.whim.b5db.model.ContestType;
import com.whim.b5db.model.Faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The rules engine. Builds games from a card catalogue and drives the strict
 * five-phase turn loop (START, STRATEGY, ACTION, ACQUISITION, CLEANUP). The
 * same instance is used by the Swing UI and the headless simulator, so both
 * observe identical, deterministic behaviour for a given seed.
 */
public final class GameEngine {

    private final List<Card> catalogue;
    private final GameConfig config;

    public GameEngine(List<Card> catalogue, GameConfig config) {
        this.catalogue = catalogue;
        this.config = config;
    }

    public GameConfig config() {
        return config;
    }

    /** Build a fresh, deterministic game for the given seats and seed. */
    public GameState createGame(List<Seat> seats, long seed) {
        Rng rng = new Rng(seed);

        List<PlayerState> players = new ArrayList<>();
        for (Seat seat : seats) {
            PlayerState p = new PlayerState(seat.name, seat.faction, seat.ai);
            p.drawDeck().addAll(BasicCards.starterDeck());
            rng.shuffle(p.drawDeck());
            p.commandRow().addAll(BasicCards.ambassadorsFor(seat.faction));
            players.add(p);
        }

        List<Card> rimSeed = buildRimSeed();
        Market market = new Market(rimSeed, BasicCards.corridorPiles(), rng);
        return new GameState(players, market, rng, config.prestigeTarget);
    }

    /** Assemble the RIM deck: {@code rimCopies} of each acquirable catalogue card. */
    private List<Card> buildRimSeed() {
        List<Card> unique = new ArrayList<>();
        for (Card c : catalogue) {
            if (c.type() == CardType.AMBASSADOR_HERO) {
                continue; // heroes start in play, never in the market
            }
            if (c.cost() <= 0) {
                continue; // free basics live in the corridor/starter, not the RIM
            }
            unique.add(c);
            if (unique.size() >= config.maxUniqueMarketCards) {
                break;
            }
        }
        List<Card> seed = new ArrayList<>();
        for (int i = 0; i < config.rimCopies; i++) {
            seed.addAll(unique);
        }
        return seed;
    }

    /** @return true once any end-game trigger has fired. */
    public boolean isOver(GameState state) {
        return state.leader().prestige() >= config.prestigeTarget
                || state.market().exhausted()
                || state.turn() >= config.maxTurns;
    }

    /** Run one full turn for the current player, then advance the seat. */
    public void playTurn(GameState state, Agent agent) {
        beginTurn(state);
        acquisitionPhase(state, state.current(), agent);
        concludeTurn(state);
    }

    /**
     * Run START, STRATEGY and ACTION for the current player, stopping before
     * ACQUISITION. The UI calls this, lets a human choose purchases, then calls
     * {@link #concludeTurn(GameState)}.
     */
    public void beginTurn(GameState state) {
        PlayerState p = state.current();
        startPhase(state, p);
        strategyPhase(state, p);
        actionPhase(state, p);
    }

    // --- phases ---

    private void startPhase(GameState state, PlayerState p) {
        p.resetPools();
        for (Card c : p.commandRow()) {
            addAttributes(p, c);
            EffectResolver.applyAll(c, state, p);
        }
    }

    private void strategyPhase(GameState state, PlayerState p) {
        p.draw(config.handSize, state.rng());
    }

    private void actionPhase(GameState state, PlayerState p) {
        // Auto-play every card in hand: economy, characters, events all contribute
        // their attributes and effects. (Permanents are never in hand.)
        List<Card> handCopy = new ArrayList<>(p.hand());
        for (Card c : handCopy) {
            p.hand().remove(c);
            p.playArea().add(c);
            addAttributes(p, c);
            EffectResolver.applyAll(c, state, p);
        }
        applyAllyBonus(p);
        EffectResolver.convertDiplomacy(p);
        resolveConflicts(state, p);
    }

    private void acquisitionPhase(GameState state, PlayerState p, Agent agent) {
        List<Card> buys = agent.chooseAcquisitions(state, p, this);
        for (Card c : buys) {
            purchase(state, p, c);
        }
    }

    private void cleanupPhase(PlayerState p) {
        p.discard().addAll(p.playArea());
        p.playArea().clear();
        p.discard().addAll(p.hand());
        p.hand().clear();
    }

    /**
     * Finish the current player's turn from within the ACQUISITION_PHASE
     * (CLEANUP + advance seat). Used by {@link com.whim.b5db.ai.MonteCarloAgent}
     * to continue a rollout after applying a candidate purchase.
     */
    public void concludeTurn(GameState state) {
        cleanupPhase(state.current());
        state.advance();
    }

    // --- mechanics shared by UI and AI ---

    /** Add a card's four attribute ratings to the owner's per-turn pools. */
    public void addAttributes(PlayerState p, Card c) {
        for (ContestType t : ContestType.values()) {
            int v = c.attribute(t);
            if (v != 0) {
                p.addPool(t, v);
            }
        }
    }

    /** Star-Realms-style Ally Bonus: +1 INFLUENCE per same-faction card beyond the first. */
    public void applyAllyBonus(PlayerState p) {
        Map<Faction, Integer> counts = new HashMap<>();
        for (Card c : p.playArea()) {
            counts.merge(c.faction(), 1, Integer::sum);
        }
        for (Card c : p.commandRow()) {
            counts.merge(c.faction(), 1, Integer::sum);
        }
        for (Map.Entry<Faction, Integer> e : counts.entrySet()) {
            if (e.getKey() == Faction.NON_ALIGNED) {
                continue;
            }
            if (e.getValue() >= 2) {
                p.addInfluence(e.getValue() - 1);
            }
        }
    }

    /** Resolve every RIM conflict the player can currently beat (vs the board). */
    public void resolveConflicts(GameState state, PlayerState p) {
        List<Card> rimCopy = new ArrayList<>(state.market().rim());
        for (Card c : rimCopy) {
            if (c.type() != CardType.CONFLICT || c.contest() == null) {
                continue;
            }
            if (p.pool(c.contest()) >= c.difficulty()) {
                p.addPrestige(c.prestige());
                EffectResolver.applyAll(c, state, p);
                state.market().buyFromRim(c);
            }
        }
    }

    /**
     * Purchase a card from the RIM or the CORRIDOR. Permanents go to the
     * COMMAND_ROW (and bank their prestige value); other cards go to the
     * DISCARD_PILE to cycle through the deck.
     *
     * @return true if the purchase succeeded.
     */
    public boolean purchase(GameState state, PlayerState p, Card c) {
        if (!p.spendInfluence(c.cost())) {
            return false;
        }
        boolean fromRim = state.market().rim().contains(c);
        if (fromRim) {
            state.market().buyFromRim(c);
        }
        if (c.permanent()) {
            p.commandRow().add(c);
        } else {
            p.discard().add(c);
        }
        if (c.prestige() != 0) {
            p.addPrestige(c.prestige());
        }
        return true;
    }

    /** Cards the player can currently afford from RIM + CORRIDOR. */
    public List<Card> affordable(GameState state, PlayerState p) {
        List<Card> options = new ArrayList<>();
        for (Card c : state.market().rim()) {
            if (c.type() != CardType.CONFLICT && c.cost() <= p.influence()) {
                options.add(c);
            }
        }
        for (Card c : state.market().corridor()) {
            if (c.cost() <= p.influence()) {
                options.add(c);
            }
        }
        return options;
    }

    /** Run a game to completion with one agent per seat. */
    public GameResult run(GameState state, List<Agent> agents) {
        while (!isOver(state)) {
            playTurn(state, agents.get(state.currentPlayerIndex()));
        }
        return GameResult.from(state, state.turn());
    }
}
