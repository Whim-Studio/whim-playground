package com.whim.cardwoven.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A draw deck: a dynamic, ordered stack of cards. The "top" (next to be drawn)
 * is index 0. Shuffling is deterministic given the seeded {@link Random} shared
 * with the owning game.
 */
public final class Deck {

    private final List<Card> cards = new ArrayList<Card>();
    private final Random random;

    public Deck(Random random) {
        this.random = random;
    }

    public Deck(Random random, List<Card> initial) {
        this.random = random;
        if (initial != null) {
            cards.addAll(initial);
        }
    }

    public int size() { return cards.size(); }

    public boolean isEmpty() { return cards.isEmpty(); }

    /** Put a card on top (drawn next). */
    public void addTop(Card c) {
        if (c != null) {
            cards.add(0, c);
        }
    }

    /** Put a card on the bottom (drawn last). */
    public void addBottom(Card c) {
        if (c != null) {
            cards.add(c);
        }
    }

    /** Deterministic shuffle using the shared seeded Random. */
    public void shuffle() {
        Collections.shuffle(cards, random);
    }

    /** Draw the top card, or null if the deck is empty. */
    public Card draw() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.remove(0);
    }

    /** Snapshot of the remaining cards, top-first. Read-only. */
    public List<Card> cards() {
        return Collections.unmodifiableList(new ArrayList<Card>(cards));
    }

    @Override
    public String toString() {
        return "Deck(" + cards.size() + ")";
    }
}
