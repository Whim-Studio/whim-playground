package com.arpg.engine;

import com.arpg.model.Item;

/** Outcome of an {@link EconomyEngine} reforge gamble. */
public final class ReforgeResult {
    public enum Outcome { SHATTERED, DIMINISHED, UNCHANGED, IMPROVED, MASTERWORK }

    private final boolean success;
    private final Outcome outcome;
    private final Item resultItem;   // null when the item shattered / on failure
    private final int cost;
    private final String message;

    ReforgeResult(boolean success, Outcome outcome, Item resultItem, int cost, String message) {
        this.success = success;
        this.outcome = outcome;
        this.resultItem = resultItem;
        this.cost = cost;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public Outcome getOutcome() { return outcome; }
    public Item getResultItem() { return resultItem; }
    public int getCost() { return cost; }
    public String getMessage() { return message; }
}
