package com.whim.necromunda.model;

/**
 * Wearable armour. The {@code saveValue} is the minimum D6 needed to negate a
 * wound (lower is better); {@code 7} means "no save possible". A weapon's save
 * modifier worsens this at resolution time.
 */
public enum Armour {
    NONE("Unarmoured", 7, 0),
    FLAK("Flak", 6, 10),
    MESH("Mesh", 5, 15),
    CARAPACE("Carapace", 4, 40);

    private final String label;
    private final int saveValue;
    private final int cost;

    Armour(String label, int saveValue, int cost) {
        this.label = label;
        this.saveValue = saveValue;
        this.cost = cost;
    }

    public String label() { return label; }

    /** Minimum unmodified D6 needed to save; 7 = no save. */
    public int saveValue() { return saveValue; }

    public int cost() { return cost; }
}
