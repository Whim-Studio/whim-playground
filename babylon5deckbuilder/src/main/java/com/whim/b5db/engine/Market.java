package com.whim.b5db.engine;

import com.whim.b5db.model.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * The hybrid market from the GDD: a dynamic, faction-seeded center-row
 * ("THE_RIM", up to {@code RIM_SLOTS} face-up cards refilled from a shared
 * deck) plus a small static supply of always-available piles
 * ("THE_CENTRAL_CORRIDOR").
 */
public final class Market {

    public static final int RIM_SLOTS = 6;

    private final List<Card> rimDeck = new ArrayList<>();
    private final List<Card> rim = new ArrayList<>();
    private final List<Card> corridor;

    public Market(List<Card> rimSeed, List<Card> corridor, Rng rng) {
        this.rimDeck.addAll(rimSeed);
        rng.shuffle(this.rimDeck);
        this.corridor = new ArrayList<>(corridor);
        refill();
    }

    /** Deep copy for Monte-Carlo rollouts (no reshuffle). */
    public Market(Market other) {
        this.rimDeck.addAll(other.rimDeck);
        this.rim.addAll(other.rim);
        this.corridor = new ArrayList<>(other.corridor);
    }

    /** Fill empty RIM slots from the top of the RIM deck. */
    public void refill() {
        while (rim.size() < RIM_SLOTS && !rimDeck.isEmpty()) {
            rim.add(rimDeck.remove(rimDeck.size() - 1));
        }
    }

    public List<Card> rim() {
        return rim;
    }

    public List<Card> corridor() {
        return corridor;
    }

    public int rimDeckSize() {
        return rimDeck.size();
    }

    /** True once both the RIM deck and every RIM slot are empty. */
    public boolean exhausted() {
        return rimDeck.isEmpty() && rim.isEmpty();
    }

    /** Remove a bought card from the RIM and immediately refill the slot. */
    public void buyFromRim(Card card) {
        rim.remove(card);
        refill();
    }

    /** SCUTTLE: banish a RIM card to the bottom of the RIM deck and refill. */
    public void scuttle(Card card) {
        if (rim.remove(card)) {
            rimDeck.add(0, card);
            refill();
        }
    }
}
