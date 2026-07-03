package com.whim.ttr.api;

/**
 * The eight train-car colors plus the multi-colored LOCOMOTIVE (wild) card.
 * There are 12 cards of each of the 8 colors and 14 LOCOMOTIVE cards = 110 total.
 *
 * <p>Route colors on the board are represented with this same enum; a route
 * whose {@code color} is {@code null} is a GRAY route claimable with any single
 * matching set of one color (see {@code domain.Route}).</p>
 */
public enum CardColor {
    PURPLE, BLUE, ORANGE, WHITE, GREEN, YELLOW, BLACK, RED, LOCOMOTIVE;

    /** True for the wild card, which substitutes for any color. */
    public boolean isLocomotive() {
        return this == LOCOMOTIVE;
    }

    /** The eight non-wild colors, in stable board/deck order. */
    public static CardColor[] trainColors() {
        return new CardColor[] { PURPLE, BLUE, ORANGE, WHITE, GREEN, YELLOW, BLACK, RED };
    }
}
