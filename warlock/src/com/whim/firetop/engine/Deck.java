package com.whim.firetop.engine;

import com.whim.firetop.model.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A shuffled draw pile of {@link Card}s with a discard pile. Draw removes from
 * the top; when the draw pile empties, the discards are reshuffled back in. The
 * shuffle uses a seeded {@link Random} for reproducibility.
 */
public final class Deck {

    private final List<Card> drawPile;
    private final List<Card> discardPile = new ArrayList<Card>();
    private final Random random;

    /**
     * @param cards the cards to load (copied)
     * @param seed  shuffle seed (share the game seed for reproducibility)
     */
    public Deck(List<Card> cards, long seed) {
        this.drawPile = new ArrayList<Card>(cards);
        this.random = new Random(seed);
        Collections.shuffle(drawPile, random);
    }

    /**
     * Draws the top card, reshuffling the discard pile when the draw pile is
     * empty. Returns {@code null} only if there are no cards anywhere.
     */
    public Card draw() {
        if (drawPile.isEmpty()) {
            if (discardPile.isEmpty()) {
                return null;
            }
            drawPile.addAll(discardPile);
            discardPile.clear();
            Collections.shuffle(drawPile, random);
        }
        return drawPile.remove(drawPile.size() - 1);
    }

    /** Places a card on the discard pile. */
    public void discard(Card card) {
        if (card != null) {
            discardPile.add(card);
        }
    }

    public int drawCount() { return drawPile.size(); }
    public int discardCount() { return discardPile.size(); }

    /** All remaining cards (draw + discard) — used to persist deck state. */
    public List<Card> remaining() {
        List<Card> all = new ArrayList<Card>(drawPile);
        all.addAll(discardPile);
        return all;
    }
}
