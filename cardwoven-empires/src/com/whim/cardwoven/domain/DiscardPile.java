package com.whim.cardwoven.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A discard pile. Cards played, discarded at end of turn, or purged land here.
 * When a {@link Deck} empties, the engine (or {@link PlayerState#drawOne()})
 * moves the whole pile back into the deck and reshuffles.
 */
public final class DiscardPile {

    private final List<Card> cards = new ArrayList<Card>();

    public int size() { return cards.size(); }

    public boolean isEmpty() { return cards.isEmpty(); }

    public void add(Card c) {
        if (c != null) {
            cards.add(c);
        }
    }

    /** Remove and return all cards, emptying the pile. */
    public List<Card> takeAll() {
        List<Card> out = new ArrayList<Card>(cards);
        cards.clear();
        return out;
    }

    /** Snapshot of the pile. Read-only. */
    public List<Card> cards() {
        return Collections.unmodifiableList(new ArrayList<Card>(cards));
    }

    @Override
    public String toString() {
        return "DiscardPile(" + cards.size() + ")";
    }
}
