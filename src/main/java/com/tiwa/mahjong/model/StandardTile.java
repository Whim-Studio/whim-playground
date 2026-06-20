package com.tiwa.mahjong.model;

import com.tiwa.mahjong.api.Suit;
import com.tiwa.mahjong.api.Tile;

/**
 * Immutable concrete {@link Tile}. Equality is by ({@link #getSuit()}, {@link #getRank()}),
 * so the four physical copies of a tile are equal to one another (per the api contract).
 */
public final class StandardTile implements Tile {

    private final Suit suit;
    private final int rank;

    public StandardTile(Suit suit, int rank) {
        if (suit == null) {
            throw new IllegalArgumentException("suit must not be null");
        }
        validateRank(suit, rank);
        this.suit = suit;
        this.rank = rank;
    }

    private static void validateRank(Suit suit, int rank) {
        int max;
        if (suit.isSuited()) {
            max = 9;
        } else if (suit == Suit.WIND) {
            max = 4;
        } else if (suit == Suit.DRAGON) {
            max = 3;
        } else {
            // FLOWER / SEASON
            max = 4;
        }
        if (rank < 1 || rank > max) {
            throw new IllegalArgumentException("rank " + rank + " out of range 1.." + max + " for suit " + suit);
        }
    }

    @Override
    public Suit getSuit() {
        return suit;
    }

    @Override
    public int getRank() {
        return rank;
    }

    @Override
    public boolean isHonor() {
        return suit.isHonor();
    }

    @Override
    public boolean isSuited() {
        return suit.isSuited();
    }

    @Override
    public boolean isBonus() {
        return suit.isBonus();
    }

    @Override
    public boolean isTerminal() {
        return suit.isSuited() && (rank == 1 || rank == 9);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tile)) {
            return false;
        }
        Tile other = (Tile) o;
        return rank == other.getRank() && suit == other.getSuit();
    }

    @Override
    public int hashCode() {
        return 31 * suit.hashCode() + rank;
    }

    @Override
    public String toString() {
        return suit + "-" + rank;
    }
}
