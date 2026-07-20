package com.heroquest.model;

/** Decorative / searchable dungeon furniture occupying a single square. */
public enum Furniture {
    TABLE("Table"),
    CHEST("Treasure Chest"),
    THRONE("Throne"),
    BOOKCASE("Bookcase"),
    WEAPON_RACK("Weapon Rack"),
    FIREPLACE("Fireplace"),
    TOMB("Tomb");

    private final String label;

    Furniture(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Chests can be searched for treasure; other furniture is flavour. */
    public boolean isSearchable() {
        return this == CHEST || this == TOMB;
    }
}
