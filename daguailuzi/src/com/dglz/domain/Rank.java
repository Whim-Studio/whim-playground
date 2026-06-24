package com.dglz.domain;

/** Card rank, ascending strength. Note 2 is high; jokers are above all natural ranks. */
public enum Rank {
    THREE(3, "3"), FOUR(4, "4"), FIVE(5, "5"), SIX(6, "6"), SEVEN(7, "7"), EIGHT(8, "8"),
    NINE(9, "9"), TEN(10, "10"), JACK(11, "J"), QUEEN(12, "Q"), KING(13, "K"), ACE(14, "A"),
    TWO(15, "2"), SMALL_JOKER(16, "小怪"), BIG_JOKER(17, "大怪");

    private final int order;
    private final String label;

    Rank(int order, String label) {
        this.order = order;
        this.label = label;
    }

    public int order() {
        return order;
    }

    public String label() {
        return label;
    }

    public boolean isJoker() {
        return this == SMALL_JOKER || this == BIG_JOKER;
    }

    /** Natural ranks THREE..TWO (no jokers), ascending, for straight building. */
    public static Rank[] naturalAscending() {
        return new Rank[] {
            THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE, TWO
        };
    }
}
