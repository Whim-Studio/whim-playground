package com.whim.settlers.buildings;

import java.awt.Color;

/** Groups buildings in the build menu and for economy bookkeeping. */
public enum BuildingCategory {
    HEADQUARTERS("HQ",       new Color(0xF2E9C9)),
    WOOD        ("Wood",     new Color(0xC8A15A)),
    STONE       ("Stone",    new Color(0xBFBFBF)),
    FOOD        ("Food",     new Color(0xE0A85A)),
    MINE        ("Mines",    new Color(0x8C7B6B)),
    METAL       ("Metal",    new Color(0xB08D57)),
    TOOLS       ("Tools",    new Color(0x9AA0A6)),
    MILITARY    ("Military", new Color(0xC85A5A)),
    SHIPPING    ("Shipping", new Color(0x5A8FC8));

    private final String label;
    private final Color color;

    BuildingCategory(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    public String label() { return label; }
    public Color color()   { return color; }
}
