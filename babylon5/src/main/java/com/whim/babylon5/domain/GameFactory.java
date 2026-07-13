package com.whim.babylon5.domain;

import com.whim.babylon5.data.CardDatabase;
import com.whim.babylon5.data.DeckStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the standard single-player game: one human (index 0) plus exactly
 * three AI opponents (indices 1..3). Deterministic for a given seed.
 *
 * <p>Per the contract and rulebook setup: each faction starts with Influence
 * Rating and pool of 4, its Ambassador placed in the Inner Circle, a deck built
 * from {@link CardDatabase}, shuffled with the game's seeded RNG, and an opening
 * hand of {@link #OPENING_HAND_SIZE} cards drawn.</p>
 */
public final class GameFactory {

    public static final int OPENING_HAND_SIZE = 6;

    /** The four Premiere player races, human first. */
    private static final FactionId[] STANDARD_FACTIONS = {
            FactionId.HUMAN, FactionId.MINBARI, FactionId.NARN, FactionId.CENTAURI
    };

    private static final String[] STANDARD_NAMES = {
            "You (Earth Alliance)", "Minbari Federation", "Narn Regime", "Centauri Republic"
    };

    private GameFactory() { }

    public static GameState newStandardGame(long seed) {
        List<PlayerState> players = new ArrayList<PlayerState>();
        for (int i = 0; i < STANDARD_FACTIONS.length; i++) {
            players.add(new PlayerState(STANDARD_NAMES[i], STANDARD_FACTIONS[i], i == 0));
        }

        GameState state = new GameState(players, seed);

        for (PlayerState p : players) {
            seedPlayer(p, state);
        }

        // Initiative order (rulebook Ready step 3): lowest Influence Rating acts
        // first. With all ratings equal at start, index 0 (the human) begins.
        state.setActiveIndex(0);
        state.setPhase(Phase.READY);
        return state;
    }

    /**
     * Build one player's deck from the card database, place the Ambassador in
     * the Inner Circle, shuffle the rest into the draw deck (deterministically
     * via the game RNG), and draw the opening hand.
     */
    private static void seedPlayer(PlayerState p, GameState state) {
        p.setInfluenceRating(4);
        p.setInfluencePool(4);
        p.setPower(4);

        List<Card> pool = CardDatabase.forFaction(p.getFaction());

        // The starting Ambassador always comes from the faction pool (never drawn),
        // so the game is playable regardless of any custom deck.
        Card ambassador = null;
        for (Card def : pool) {
            if (def.getType() == CardType.AMBASSADOR) {
                ambassador = CardDatabase.copyOf(def);
                break;
            }
        }

        List<Card> deckCards = DeckStore.hasDeck(p.getFaction())
                ? buildCustomDeck(p.getFaction())
                : buildDefaultDeck(pool);

        // Place the Ambassador in the Inner Circle (rulebook: the Ambassador is
        // always a member of the faction's Inner Circle).
        if (ambassador != null) {
            p.zone(ZoneType.INNER_CIRCLE).add(ambassador);
        }

        Zone drawDeck = p.zone(ZoneType.DRAW_DECK);
        for (Card c : deckCards) {
            drawDeck.add(c);
        }
        drawDeck.shuffle(state.getRng());

        // Draw the opening hand.
        Zone hand = p.zone(ZoneType.HAND);
        for (int i = 0; i < OPENING_HAND_SIZE; i++) {
            Card drawn = drawDeck.draw();
            if (drawn == null) break;
            hand.add(drawn);
        }
    }

    /** Default deck: one in-play copy of every non-Ambassador card in the faction pool. */
    private static List<Card> buildDefaultDeck(List<Card> pool) {
        List<Card> deck = new ArrayList<Card>();
        for (Card def : pool) {
            if (def.getType() != CardType.AMBASSADOR) {
                deck.add(CardDatabase.copyOf(def));
            }
        }
        return deck;
    }

    /**
     * Custom deck: expand the faction's saved id-&gt;count list into in-play copies.
     * Ambassadors are skipped (the starting Ambassador is placed separately) and
     * unknown ids are ignored, so a stale deck can never break game creation.
     */
    private static List<Card> buildCustomDeck(FactionId faction) {
        List<Card> deck = new ArrayList<Card>();
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>(DeckStore.deckFor(faction));
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            Card def = CardDatabase.byId(e.getKey());
            if (def == null || def.getType() == CardType.AMBASSADOR) {
                continue;
            }
            int n = e.getValue() == null ? 0 : e.getValue();
            for (int i = 0; i < n; i++) {
                deck.add(CardDatabase.copyOf(def));
            }
        }
        return deck;
    }
}
