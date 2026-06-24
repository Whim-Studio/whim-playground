package com.dglz.domain;

import java.util.Collections;
import java.util.List;

/** A validated play. Constructed by ComboValidator. */
public final class Combination {
    private final Road road;
    private final ComboType type;
    private final List<Card> cards;
    private final Rank primaryRank;
    private final int wildcardsUsed;

    public Combination(Road road, ComboType type, List<Card> cards, Rank primaryRank,
                       int wildcardsUsed) {
        this.road = road;
        this.type = type;
        this.cards = Collections.unmodifiableList(cards);
        this.primaryRank = primaryRank;
        this.wildcardsUsed = wildcardsUsed;
    }

    public Road road() {
        return road;
    }

    public ComboType type() {
        return type;
    }

    public List<Card> cards() {
        return cards;
    }

    public Rank primaryRank() {
        return primaryRank;
    }

    public int wildcardsUsed() {
        return wildcardsUsed;
    }

    public int size() {
        return cards.size();
    }

    /**
     * True if this combination beats other. Same road required; higher type.tier() wins,
     * else higher primaryRank.order() wins. Different road throws IllegalArgumentException.
     */
    public boolean beats(Combination other) {
        if (this.road != other.road) {
            throw new IllegalArgumentException(
                "Cannot compare combinations of different roads: " + this.road + " vs " + other.road);
        }
        int myTier = this.type.tier();
        int otherTier = other.type.tier();
        if (myTier != otherTier) {
            return myTier > otherTier;
        }
        return this.primaryRank.order() > other.primaryRank.order();
    }

    @Override
    public String toString() {
        return type + cards.toString();
    }
}
