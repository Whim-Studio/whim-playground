package com.arpg.model;

/**
 * Item quality tiers. Each tier carries a drop {@code weight} (higher = more
 * common) used by {@link LootTable} rolls, plus a stat multiplier hint and a
 * display colour (packed RGB) that the UI may use. Pure data — the actual roll
 * lives in the engine.
 */
public enum Rarity {
    COMMON("Common", 100, 1.0, 0xB8B8B8),
    UNCOMMON("Uncommon", 55, 1.35, 0x4CC24C),
    RARE("Rare", 26, 1.8, 0x3D7DFF),
    EPIC("Epic", 9, 2.4, 0xB44CE0),
    LEGENDARY("Legendary", 2, 3.4, 0xF0A020);

    private final String displayName;
    private final int weight;
    private final double statMultiplier;
    private final int colorRgb;

    Rarity(String displayName, int weight, double statMultiplier, int colorRgb) {
        this.displayName = displayName;
        this.weight = weight;
        this.statMultiplier = statMultiplier;
        this.colorRgb = colorRgb;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getWeight() {
        return weight;
    }

    public double getStatMultiplier() {
        return statMultiplier;
    }

    public int getColorRgb() {
        return colorRgb;
    }
}
