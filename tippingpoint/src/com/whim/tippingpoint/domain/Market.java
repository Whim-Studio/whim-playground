package com.whim.tippingpoint.domain;

import java.util.ArrayList;
import java.util.List;

/** The 3x4 face-up Central Market of development cards, drawn from a deck. */
public final class Market {
    private final DevelopmentCard[][] slots;
    private final List<DevelopmentCard> deck;

    public Market(List<DevelopmentCard> deck) {
        this.deck = new ArrayList<DevelopmentCard>(deck);
        this.slots = new DevelopmentCard[Rules.MARKET_ROWS][Rules.MARKET_COLS];
        refill();
    }

    public int rows() { return Rules.MARKET_ROWS; }
    public int cols() { return Rules.MARKET_COLS; }

    public DevelopmentCard get(int row, int col) { return slots[row][col]; }

    public DevelopmentCard take(int row, int col) {
        DevelopmentCard c = slots[row][col];
        slots[row][col] = null;
        return c;
    }

    /** Fill every null slot from the remaining deck (while cards remain). */
    public void refill() {
        for (int r = 0; r < Rules.MARKET_ROWS; r++) {
            for (int c = 0; c < Rules.MARKET_COLS; c++) {
                if (slots[r][c] == null && !deck.isEmpty()) {
                    slots[r][c] = deck.remove(0);
                }
            }
        }
    }

    public boolean deckEmpty() { return deck.isEmpty(); }
}
