package com.whim.tarot.domain;

/** Immutable concrete {@link DrawnCard}. */
public final class DefaultDrawnCard implements DrawnCard {
    private final Card card;
    private final Orientation orientation;

    public DefaultDrawnCard(Card card, Orientation orientation) {
        if (card == null || orientation == null) {
            throw new IllegalArgumentException("card and orientation must not be null");
        }
        this.card = card;
        this.orientation = orientation;
    }

    public Card getCard() { return card; }

    public Orientation getOrientation() { return orientation; }

    public boolean isReversed() { return orientation == Orientation.REVERSED; }

    public String getActiveMeaning() {
        return isReversed() ? card.getReversedMeaning() : card.getUprightMeaning();
    }

    @Override
    public String toString() {
        return card.getName() + " [" + orientation + "]";
    }
}
