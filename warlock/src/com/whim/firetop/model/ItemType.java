package com.whim.firetop.model;

/** Category of an {@link Item}, determining how it may be used. */
public enum ItemType {
    /** Improves combat; kept in inventory. */
    WEAPON,
    /** Consumable that restores an attribute when used. */
    POTION,
    /** Valuable for score; no direct combat use. */
    TREASURE,
    /** Opens the way deeper into the mountain. */
    KEY,
    /** Edible; restores STAMINA outside combat. */
    PROVISION
}
