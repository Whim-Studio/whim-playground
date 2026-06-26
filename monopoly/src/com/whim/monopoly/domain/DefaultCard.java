package com.whim.monopoly.domain;

public class DefaultCard implements Card {
    private final Deck deck;
    private final String text;
    private final CardAction action;
    private final int amount;
    private final int amount2;
    private final int targetIndex;

    public DefaultCard(Deck deck, String text, CardAction action,
                       int amount, int amount2, int targetIndex) {
        this.deck = deck;
        this.text = text;
        this.action = action;
        this.amount = amount;
        this.amount2 = amount2;
        this.targetIndex = targetIndex;
    }

    public Deck getDeck() { return deck; }
    public String getText() { return text; }
    public CardAction getAction() { return action; }
    public int getAmount() { return amount; }
    public int getAmount2() { return amount2; }
    public int getTargetIndex() { return targetIndex; }
}
