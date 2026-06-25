package com.whim.tarot.domain;

/** A card paired with the orientation it was drawn in. */
public interface DrawnCard {
    Card getCard();
    Orientation getOrientation();
    boolean isReversed();
    String getActiveMeaning();    // upright or reversed meaning per orientation
}
