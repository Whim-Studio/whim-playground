package com.whim.babylon5.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A named, immutable card list used to seed a player. The rulebook requires a
 * minimum of 45 cards including exactly one Starting Ambassador; this prototype
 * does not enforce that minimum (the embedded starter set is smaller) but the
 * structure mirrors a real play deck.
 */
public final class Deck {

    private final String name;
    private final FactionId faction;
    private final List<Card> cards;

    public Deck(String name, FactionId faction, List<Card> cards) {
        this.name = name;
        this.faction = faction;
        this.cards = Collections.unmodifiableList(
                new ArrayList<Card>(cards == null ? new ArrayList<Card>() : cards));
    }

    public String getName() { return name; }
    public FactionId getFaction() { return faction; }
    public List<Card> getCards() { return cards; }
}
