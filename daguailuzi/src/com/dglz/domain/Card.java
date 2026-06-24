package com.dglz.domain;

/** Immutable playing card. */
public final class Card {
    private final Rank rank;
    private final Suit suit;
    private final int deckId;

    public Card(Rank rank, Suit suit, int deckId) {
        this.rank = rank;
        this.suit = suit;
        this.deckId = deckId;
    }

    public Rank rank() {
        return rank;
    }

    public Suit suit() {
        return suit;
    }

    public int deckId() {
        return deckId;
    }

    public boolean isWildcard() {
        return rank.isJoker();
    }

    public boolean isBigJoker() {
        return rank == Rank.BIG_JOKER;
    }

    public boolean isSmallJoker() {
        return rank == Rank.SMALL_JOKER;
    }

    /** e.g. "♠K", or "小怪"/"大怪" for jokers. */
    public String shortName() {
        if (rank.isJoker()) {
            return rank.label();
        }
        return suit.symbol() + rank.label();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Card)) {
            return false;
        }
        Card other = (Card) o;
        return deckId == other.deckId && rank == other.rank && suit == other.suit;
    }

    @Override
    public int hashCode() {
        int result = rank.hashCode();
        result = 31 * result + suit.hashCode();
        result = 31 * result + deckId;
        return result;
    }

    @Override
    public String toString() {
        return shortName() + "#" + deckId;
    }
}
