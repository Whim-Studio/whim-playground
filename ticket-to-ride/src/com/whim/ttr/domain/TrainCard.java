package com.whim.ttr.domain;

import com.whim.ttr.api.CardColor;

/**
 * A trivial wrapper around a single train-car {@link CardColor}. The deck and
 * hands mostly deal in {@code CardColor} directly; this type exists for the
 * rare spot that wants a card object rather than a bare enum.
 */
public final class TrainCard {

    private final CardColor color;

    public TrainCard(CardColor color) {
        this.color = color;
    }

    public CardColor color() { return color; }

    @Override public String toString() { return String.valueOf(color); }
}
