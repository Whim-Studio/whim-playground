package com.whim.babylon5.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * An ordered collection of cards belonging to one player's {@link ZoneType}.
 * For {@code DRAW_DECK}, index 0 is the top of the deck (the next card drawn).
 */
public final class Zone {

    private final ZoneType type;
    private final List<Card> cards = new ArrayList<Card>();

    public Zone(ZoneType type) {
        this.type = type;
    }

    public ZoneType getType() { return type; }

    /** Live, ordered list (mutating it mutates the zone). Index 0 == top for DRAW_DECK. */
    public List<Card> getCards() { return cards; }

    public void add(Card c) {
        if (c != null) cards.add(c);
    }

    public boolean remove(Card c) {
        return cards.remove(c);
    }

    public int size() { return cards.size(); }

    public boolean isEmpty() { return cards.isEmpty(); }

    /** Deterministic shuffle using the supplied seeded RNG. */
    public void shuffle(Random rng) {
        Collections.shuffle(cards, rng);
    }

    /** Remove and return the top card (index 0), or {@code null} if empty. */
    public Card draw() {
        if (cards.isEmpty()) return null;
        return cards.remove(0);
    }
}
