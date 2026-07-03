package com.arpg.ui;

import java.awt.Color;
import java.awt.Font;

import com.arpg.model.Rarity;

/** Shared colors, fonts and small paint helpers for the ARPG Swing UI. */
final class UiTheme {

    private UiTheme() {
    }

    static final Color BG_DARK = new Color(0x1E1E24);
    static final Color BG_PANEL = new Color(0x2A2A33);
    static final Color BG_SLOT = new Color(0x35353F);
    static final Color FG_TEXT = new Color(0xE6E6EB);
    static final Color FG_MUTED = new Color(0x9A9AA6);
    static final Color ACCENT = new Color(0x6C8CFF);

    static final Color DAMAGE = new Color(0xE8503A);
    static final Color HEAL = new Color(0x4FBF6A);
    static final Color BUFF = new Color(0x5AA8FF);
    static final Color DEBUFF = new Color(0xB05AFF);
    static final Color LOOT = new Color(0xF2C14E);

    static final Color HP_BAR = new Color(0xC0392B);
    static final Color RESOURCE_BAR = new Color(0x2E86DE);
    static final Color XP_BAR = new Color(0xF2C14E);

    static final Font TITLE = new Font("SansSerif", Font.BOLD, 16);
    static final Font BODY = new Font("SansSerif", Font.PLAIN, 12);
    static final Font BODY_BOLD = new Font("SansSerif", Font.BOLD, 12);
    static final Font MONO = new Font("Monospaced", Font.PLAIN, 12);

    /**
     * Color for an item rarity. Resolved by {@link Rarity#name()} so it works
     * regardless of the exact enum-constant ordering, falling back to a neutral
     * gray for any rarity we don't recognize.
     */
    static Color rarityColor(Rarity rarity) {
        if (rarity == null) {
            return FG_MUTED;
        }
        String n = rarity.name().toUpperCase();
        if (n.contains("LEGEND")) {
            return new Color(0xF2792E);
        }
        if (n.contains("EPIC") || n.contains("MYTH")) {
            return new Color(0xA335EE);
        }
        if (n.contains("RARE")) {
            return new Color(0x2E86DE);
        }
        if (n.contains("UNCOMMON") || n.contains("MAGIC")) {
            return new Color(0x4FBF6A);
        }
        if (n.contains("COMMON") || n.contains("POOR") || n.contains("NORMAL")) {
            return new Color(0xC8C8D0);
        }
        return FG_MUTED;
    }
}
