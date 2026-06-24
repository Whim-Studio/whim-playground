package com.dglz.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Full triple deck: 3 decks x (52 ranked + 2 jokers) = 162 cards. */
public final class Deck {
    private final List<Card> cards;

    public Deck() {
        cards = new ArrayList<Card>(162);
        Suit[] rankedSuits = { Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES };
        Rank[] naturals = Rank.naturalAscending(); // 13 natural ranks
        for (int deckId = 0; deckId < 3; deckId++) {
            for (Suit suit : rankedSuits) {
                for (Rank rank : naturals) {
                    cards.add(new Card(rank, suit, deckId));
                }
            }
            cards.add(new Card(Rank.SMALL_JOKER, Suit.JOKER, deckId));
            cards.add(new Card(Rank.BIG_JOKER, Suit.JOKER, deckId));
        }
    }

    /** The 162 cards. */
    public List<Card> cards() {
        return cards;
    }

    public void shuffle(Random rng) {
        Collections.shuffle(cards, rng);
    }

    /** Deal perPlayer cards to each of players; returns players hands. Called with (6, 27). */
    public List<List<Card>> deal(int players, int perPlayer) {
        List<List<Card>> hands = new ArrayList<List<Card>>(players);
        for (int p = 0; p < players; p++) {
            hands.add(new ArrayList<Card>(perPlayer));
        }
        int idx = 0;
        for (int round = 0; round < perPlayer; round++) {
            for (int p = 0; p < players; p++) {
                hands.get(p).add(cards.get(idx++));
            }
        }
        return hands;
    }
}
