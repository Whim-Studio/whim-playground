package com.whim.stars.model.ship;

/**
 * The kind of a ship component, which determines the hull slots it may occupy.
 * A GENERAL slot accepts any category; every other slot accepts only its own.
 */
public enum ComponentCategory {
    ENGINE("Engine"),
    WEAPON("Weapon"),
    ARMOR("Armor"),
    SHIELD("Shield"),
    SCANNER("Scanner"),
    ELECTRONICS("Electronics"),
    MECHANICAL("Mechanical"),
    BOMB("Bomb"),
    MINING_ROBOT("Mining Robot"),
    MINE_LAYER("Mine Layer"),
    ORBITAL("Orbital"),
    GENERAL("General Purpose");

    private final String label;

    ComponentCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
