package com.whim.monopoly.data;

import com.whim.monopoly.domain.Card;
import com.whim.monopoly.domain.CardAction;
import com.whim.monopoly.domain.DefaultCard;
import com.whim.monopoly.domain.Deck;

import java.util.ArrayList;
import java.util.List;

/**
 * The 16 standard Chance and 16 standard Community Chest cards (US edition).
 * Get-Out-of-Jail-Free appears in both decks.
 */
public final class Cards {
    private Cards() { }

    private static Card c(Deck deck, String text, CardAction action,
                          int amount, int amount2, int targetIndex) {
        return new DefaultCard(deck, text, action, amount, amount2, targetIndex);
    }

    /** The 16 standard Chance cards. */
    public static List<Card> chance() {
        Deck d = Deck.CHANCE;
        List<Card> cards = new ArrayList<Card>(16);
        cards.add(c(d, "Advance to GO. (Collect $200)", CardAction.MOVE_TO_SPACE, 0, 0, 0));
        cards.add(c(d, "Advance to Illinois Avenue. If you pass Go, collect $200.", CardAction.MOVE_TO_SPACE, 0, 0, 24));
        cards.add(c(d, "Advance to St. Charles Place. If you pass Go, collect $200.", CardAction.MOVE_TO_SPACE, 0, 0, 11));
        cards.add(c(d, "Advance token to nearest Utility. If unowned, you may buy it from the Bank. If owned, throw dice and pay owner ten times the amount thrown.", CardAction.NEAREST_UTILITY, 0, 0, -1));
        cards.add(c(d, "Advance to the nearest Railroad. Pay owner twice the rental to which they are otherwise entitled. If unowned, you may buy it from the Bank.", CardAction.NEAREST_RAILROAD, 0, 0, -1));
        cards.add(c(d, "Advance to the nearest Railroad. Pay owner twice the rental to which they are otherwise entitled. If unowned, you may buy it from the Bank.", CardAction.NEAREST_RAILROAD, 0, 0, -1));
        cards.add(c(d, "Bank pays you dividend of $50.", CardAction.COLLECT, 50, 0, -1));
        cards.add(c(d, "Get Out of Jail Free. This card may be kept until needed or sold.", CardAction.GET_OUT_OF_JAIL_FREE, 0, 0, -1));
        cards.add(c(d, "Go Back 3 Spaces.", CardAction.MOVE_BACK, 3, 0, -1));
        cards.add(c(d, "Go to Jail. Go directly to Jail. Do not pass Go, do not collect $200.", CardAction.GO_TO_JAIL, 0, 0, -1));
        cards.add(c(d, "Make general repairs on all your property: For each house pay $25, for each hotel pay $100.", CardAction.STREET_REPAIRS, 25, 100, -1));
        cards.add(c(d, "Speeding fine $15.", CardAction.PAY, 15, 0, -1));
        cards.add(c(d, "Take a trip to Reading Railroad. If you pass Go, collect $200.", CardAction.MOVE_TO_SPACE, 0, 0, 5));
        cards.add(c(d, "Advance to Boardwalk.", CardAction.MOVE_TO_SPACE, 0, 0, 39));
        cards.add(c(d, "You have been elected Chairman of the Board. Pay each player $50.", CardAction.PAY_EACH, 50, 0, -1));
        cards.add(c(d, "Your building loan matures. Collect $150.", CardAction.COLLECT, 150, 0, -1));
        return cards;
    }

    /** The 16 standard Community Chest cards. */
    public static List<Card> communityChest() {
        Deck d = Deck.COMMUNITY_CHEST;
        List<Card> cards = new ArrayList<Card>(16);
        cards.add(c(d, "Advance to GO. (Collect $200)", CardAction.MOVE_TO_SPACE, 0, 0, 0));
        cards.add(c(d, "Bank error in your favor. Collect $200.", CardAction.COLLECT, 200, 0, -1));
        cards.add(c(d, "Doctor's fee. Pay $50.", CardAction.PAY, 50, 0, -1));
        cards.add(c(d, "From sale of stock you get $50.", CardAction.COLLECT, 50, 0, -1));
        cards.add(c(d, "Get Out of Jail Free. This card may be kept until needed or sold.", CardAction.GET_OUT_OF_JAIL_FREE, 0, 0, -1));
        cards.add(c(d, "Go to Jail. Go directly to Jail. Do not pass Go, do not collect $200.", CardAction.GO_TO_JAIL, 0, 0, -1));
        cards.add(c(d, "Holiday fund matures. Receive $100.", CardAction.COLLECT, 100, 0, -1));
        cards.add(c(d, "Income tax refund. Collect $20.", CardAction.COLLECT, 20, 0, -1));
        cards.add(c(d, "It is your birthday. Collect $10 from every player.", CardAction.COLLECT_FROM_EACH, 10, 0, -1));
        cards.add(c(d, "Life insurance matures. Collect $100.", CardAction.COLLECT, 100, 0, -1));
        cards.add(c(d, "Pay hospital fees of $100.", CardAction.PAY, 100, 0, -1));
        cards.add(c(d, "Pay school fees of $50.", CardAction.PAY, 50, 0, -1));
        cards.add(c(d, "Receive $25 consultancy fee.", CardAction.COLLECT, 25, 0, -1));
        cards.add(c(d, "You are assessed for street repairs: $40 per house, $115 per hotel.", CardAction.STREET_REPAIRS, 40, 115, -1));
        cards.add(c(d, "You have won second prize in a beauty contest. Collect $10.", CardAction.COLLECT, 10, 0, -1));
        cards.add(c(d, "You inherit $100.", CardAction.COLLECT, 100, 0, -1));
        return cards;
    }
}
