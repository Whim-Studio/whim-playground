package com.whim.warroom.domain;

import java.awt.Color;

/**
 * Terrain type of a single tile. Carries its own base fill color, a movement
 * speed multiplier, a defensive cover bonus, and passability.
 */
public enum Biome {
    GRASSLAND(new Color(112, 148, 84), 1.00, 0.00, true),
    FOREST(new Color(54, 92, 54), 0.65, 0.25, true),
    HILLS(new Color(140, 128, 88), 0.70, 0.15, true),
    DESERT(new Color(196, 178, 120), 0.85, 0.00, true),
    SNOW(new Color(226, 230, 236), 0.60, 0.00, true),
    URBAN(new Color(128, 128, 134), 0.80, 0.25, true),
    WATER(new Color(58, 96, 148), 0.25, 0.00, false);

    private final Color color;
    private final double moveCostMul;
    private final double coverBonus;
    private final boolean passable;

    Biome(Color color, double moveCostMul, double coverBonus, boolean passable) {
        this.color = color;
        this.moveCostMul = moveCostMul;
        this.coverBonus = coverBonus;
        this.passable = passable;
    }

    public Color color() {
        return color;
    }

    /** Movement speed multiplier applied to units standing on the tile. */
    public double moveCostMul() {
        return moveCostMul;
    }

    /** Additive defense fraction granted to a unit defending on the tile. */
    public double coverBonus() {
        return coverBonus;
    }

    public boolean passable() {
        return passable;
    }
}
