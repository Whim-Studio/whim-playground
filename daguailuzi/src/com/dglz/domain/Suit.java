package com.dglz.domain;

/** Card suit. Jokers use Suit.JOKER. */
public enum Suit {
    CLUBS("♣"), DIAMONDS("♦"), HEARTS("♥"), SPADES("♠"), JOKER("★");

    private final String symbol;

    Suit(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
