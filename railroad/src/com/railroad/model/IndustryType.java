package com.railroad.model;

import java.awt.Color;

/**
 * The kinds of {@link Industry} placed on the map in Phase 2. Each type declares
 * the single cargo it consumes (or {@code null} for a raw extractor) and the
 * single cargo it produces, which together define the one production chain the
 * ticket asks for:
 *
 * <pre>
 *   COAL_MINE:  consumes nothing -> produces COAL
 *   STEEL_MILL: consumes COAL    -> produces STEEL
 * </pre>
 *
 * Coal mined at a {@code COAL_MINE} is hauled to a {@code STEEL_MILL}, which
 * turns it into steel to be hauled onward to towns that demand it.
 */
public enum IndustryType {

    COAL_MINE("Coal Mine", null, CargoType.COAL, new Color(70, 60, 55)),
    STEEL_MILL("Steel Mill", CargoType.COAL, CargoType.STEEL, new Color(150, 90, 70));

    private final String label;
    private final CargoType consumes; // null for a raw extractor
    private final CargoType produces;
    private final Color color;

    IndustryType(String label, CargoType consumes, CargoType produces, Color color) {
        this.label = label;
        this.consumes = consumes;
        this.produces = produces;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    /** The cargo this industry turns into its product, or null if it extracts raw. */
    public CargoType getConsumes() {
        return consumes;
    }

    /** The cargo this industry makes available for pickup. */
    public CargoType getProduces() {
        return produces;
    }

    public Color getColor() {
        return color;
    }
}
