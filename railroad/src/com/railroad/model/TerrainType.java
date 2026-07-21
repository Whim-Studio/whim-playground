package com.railroad.model;

import java.awt.Color;

/**
 * The four terrain classes used in Phase 1. Each carries the cost of laying a
 * single track segment onto a tile of this type and a distinct display colour.
 *
 * <p>Costs are intentionally spread far apart so the terrain-dependent build
 * cost is obvious to the player: open ground is cheap, hills are pricier,
 * mountains are expensive, and water requires a costly bridge.
 */
public enum TerrainType {

    WATER("Water", 800, false, new Color(46, 90, 156)),
    CLEAR("Clear", 50, true, new Color(120, 170, 90)),
    HILLS("Hills", 150, true, new Color(160, 150, 80)),
    MOUNTAINS("Mountains", 400, false, new Color(120, 110, 100));

    private final String label;
    private final int segmentCost;
    private final boolean buildable; // whether a town may be founded here
    private final Color color;

    TerrainType(String label, int segmentCost, boolean buildable, Color color) {
        this.label = label;
        this.segmentCost = segmentCost;
        this.buildable = buildable;
        this.color = color;
    }

    /** Cost, in company cash, to lay one track segment entering a tile of this type. */
    public int getSegmentCost() {
        return segmentCost;
    }

    /** Human-readable name shown in the legend. */
    public String getLabel() {
        return label;
    }

    /** Whether a town may be placed on this terrain (open, flat land only). */
    public boolean isTownBuildable() {
        return buildable;
    }

    /** Display colour for this terrain. */
    public Color getColor() {
        return color;
    }
}
