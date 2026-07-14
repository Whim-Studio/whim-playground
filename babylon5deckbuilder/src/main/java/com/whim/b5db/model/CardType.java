package com.whim.b5db.model;

/**
 * Card categories from the GDD card dictionary. {@code permanent()} reports
 * whether a card, once acquired/played, stays in the COMMAND_ROW across turns
 * rather than cycling through the deck.
 */
public enum CardType {
    ECONOMY(false),
    CHARACTER(false),
    AMBASSADOR_HERO(true),
    FLEET(true),
    GROUP(true),
    LOCATION(true),
    AGENDA(true),
    CONFLICT(false),
    EVENT(false);

    private final boolean permanent;

    CardType(boolean permanent) {
        this.permanent = permanent;
    }

    /** @return true if cards of this type persist in the COMMAND_ROW. */
    public boolean permanent() {
        return permanent;
    }
}
