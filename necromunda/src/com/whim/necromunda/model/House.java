package com.whim.necromunda.model;

import java.awt.Color;

/**
 * The six core gang Houses, described by mechanical archetype only (no
 * trademarked lore). House identity is data: a flavour archetype and a display
 * colour for the board tokens. Allowed fighter types / weapon access lists can
 * be layered on later without subclassing.
 *
 * <p>Note: this enum references {@link java.awt.Color} purely as a data value for
 * the view; it holds no logic. If strict model/AWT separation is desired the
 * colour can be moved to the render layer, but a plain RGB constant is harmless.
 */
public enum House {
    GOLIATH("Goliath",  "Brute / strength house — high S/T, melee-leaning, cheap big bodies.", new Color(0xC0, 0x39, 0x2B)),
    ORLOCK("Orlock",    "Balanced industrial house — all-rounder, solid mid-range weapons.",   new Color(0xD3, 0x8A, 0x1B)),
    ESCHER("Escher",    "Fast agile house — high I/M, poison & blades, good pistols.",          new Color(0x2E, 0x9E, 0x5B)),
    VAN_SAAR("Van Saar","Tech / marksman house — superior BS, high-tech energy weapons.",       new Color(0x2C, 0x7B, 0xE0)),
    CAWDOR("Cawdor",    "Zealot mob house — cheap numbers, improvised weapons, morale flavour.", new Color(0x7A, 0x51, 0x2B)),
    DELAQUE("Delaque",  "Stealth house — long-range specialists, subtle weapons, stealth.",      new Color(0x4B, 0x4E, 0x6D));

    private final String label;
    private final String archetype;
    private final Color color;

    House(String label, String archetype, Color color) {
        this.label = label;
        this.archetype = archetype;
        this.color = color;
    }

    public String label() { return label; }
    public String archetype() { return archetype; }
    public Color color() { return color; }
}
