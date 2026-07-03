package com.whim.cardwoven.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.whim.cardwoven.api.Enums.Faction;
import com.whim.cardwoven.api.Enums.ResourceType;
import com.whim.cardwoven.api.Enums.VictoryType;
import com.whim.cardwoven.api.Views.CardView;
import com.whim.cardwoven.api.Views.PlayerView;

/**
 * A single player's owning state: faction profile, resource purse, deck,
 * discard pile, hand, and victory progress. Implements the read-only
 * {@link PlayerView} and exposes concrete accessors the engine mutates.
 */
public final class PlayerState implements PlayerView {

    private final int index;
    private final Faction faction;
    private final FactionProfile profile;
    private final String name;
    private final boolean human;
    private final Resources resources;
    private final Deck deck;
    private final DiscardPile discard;
    private final List<Card> hand = new ArrayList<Card>();
    private final Map<VictoryType, Double> victoryProgress =
            new EnumMap<VictoryType, Double>(VictoryType.class);

    public PlayerState(int index, Faction faction, String name, boolean human,
                       Deck deck, DiscardPile discard) {
        this.index = index;
        this.faction = faction;
        this.profile = FactionProfile.of(faction);
        this.name = name;
        this.human = human;
        this.deck = deck;
        this.discard = discard;
        this.resources =
                new Resources(profile.startingGold(), profile.startingCommand());
    }

    // --- concrete accessors (engine) ---
    public Resources resources() { return resources; }
    public FactionProfile profile() { return profile; }
    public Deck deck() { return deck; }
    public DiscardPile discard() { return discard; }

    /** Concrete hand list (engine). Live reference. */
    public List<Card> handCards() { return hand; }

    public void addToHand(Card c) {
        if (c != null) {
            hand.add(c);
        }
    }

    /**
     * Draw one card into the hand and return it. If the deck is empty, the
     * discard pile is shuffled back into the deck first. Returns null only if
     * both deck and discard are empty.
     */
    public Card drawOne() {
        if (deck.isEmpty()) {
            reshuffleDiscardIntoDeck();
        }
        Card c = deck.draw();
        if (c != null) {
            hand.add(c);
        }
        return c;
    }

    /** Move the whole discard pile into the deck and shuffle. */
    public void reshuffleDiscardIntoDeck() {
        List<Card> recovered = discard.takeAll();
        for (int i = 0; i < recovered.size(); i++) {
            deck.addBottom(recovered.get(i));
        }
        deck.shuffle();
    }

    /** Discard a specific card from hand into the discard pile. */
    public boolean discardFromHand(Card c) {
        if (c != null && hand.remove(c)) {
            discard.add(c);
            return true;
        }
        return false;
    }

    /** Remove and return a card from hand by id, or null if absent. */
    public Card removeFromHand(int cardId) {
        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.get(i);
            if (c.id() == cardId) {
                hand.remove(i);
                return c;
            }
        }
        return null;
    }

    /** Find a hand card by id without removing it, or null. */
    public Card handCard(int cardId) {
        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.get(i);
            if (c.id() == cardId) {
                return c;
            }
        }
        return null;
    }

    public void setVictoryProgress(VictoryType type, double progress) {
        double clamped = progress < 0 ? 0 : (progress > 1 ? 1 : progress);
        victoryProgress.put(type, Double.valueOf(clamped));
    }

    // --- PlayerView ---
    public int index() { return index; }
    public Faction faction() { return faction; }
    public String name() { return name; }
    public boolean isHuman() { return human; }

    public int resource(ResourceType type) { return resources.get(type); }

    public int deckCount() { return deck.size(); }
    public int discardCount() { return discard.size(); }
    public int handSize() { return hand.size(); }

    public List<CardView> hand() {
        return Collections.<CardView>unmodifiableList(
                new ArrayList<CardView>(hand));
    }

    public double victoryProgress(VictoryType type) {
        Double v = victoryProgress.get(type);
        return v == null ? 0.0 : v.doubleValue();
    }

    public List<VictoryType> pursuableVictories() {
        return profile.pursuedVictories();
    }

    @Override
    public String toString() {
        return "PlayerState#" + index + "(" + name + "," + faction
                + ",hand=" + hand.size() + ",deck=" + deck.size() + ")";
    }
}
