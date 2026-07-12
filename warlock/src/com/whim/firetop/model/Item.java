package com.whim.firetop.model;

import java.io.Serializable;

/**
 * A carryable object: weapon, potion, treasure, key or provision. Items are
 * immutable value objects.
 */
public final class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final ItemType type;
    private final String description;
    /** Effect magnitude, e.g. STAMINA restored by a potion, or gold value of treasure. */
    private final int magnitude;

    /**
     * Creates an item.
     *
     * @param name        display name
     * @param type        item category
     * @param description original flavor text
     * @param magnitude   effect magnitude (heal amount, gold value, etc.)
     */
    public Item(String name, ItemType type, String description, int magnitude) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.magnitude = magnitude;
    }

    public String getName() { return name; }
    public ItemType getType() { return type; }
    public String getDescription() { return description; }
    public int getMagnitude() { return magnitude; }

    @Override
    public String toString() {
        return name + (magnitude != 0 ? " (" + magnitude + ")" : "");
    }
}
