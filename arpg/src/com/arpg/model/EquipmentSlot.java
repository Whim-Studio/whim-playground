package com.arpg.model;

/**
 * The wearable slots on a {@link Character}. Each {@link Equipment} declares the
 * single slot it occupies.
 */
public enum EquipmentSlot {
    WEAPON("Weapon"),
    OFFHAND("Off-hand"),
    HELM("Helm"),
    CHEST("Chest"),
    GLOVES("Gloves"),
    BOOTS("Boots"),
    RING("Ring"),
    AMULET("Amulet");

    private final String displayName;

    EquipmentSlot(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
