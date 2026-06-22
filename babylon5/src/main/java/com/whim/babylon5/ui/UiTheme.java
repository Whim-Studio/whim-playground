package com.whim.babylon5.ui;

import java.awt.Color;
import java.awt.Font;

import com.whim.babylon5.domain.ConflictType;
import com.whim.babylon5.domain.FactionId;

/**
 * Central palette + fonts for the Babylon 5 virtual tabletop. Kept tiny and
 * dependency-free so every panel reads from one source of truth.
 */
final class UiTheme {

    private UiTheme() { }

    static final Color SPACE      = new Color(0x0B1020);
    static final Color PANEL      = new Color(0x141B30);
    static final Color PANEL_HI   = new Color(0x1E2A45);
    static final Color INK        = new Color(0xE8ECF8);
    static final Color INK_DIM    = new Color(0x9AA6C4);
    static final Color ACCENT     = new Color(0x4F8CFF);
    static final Color GOLD       = new Color(0xE8C15A);
    static final Color DANGER     = new Color(0xE0556B);
    static final Color OK         = new Color(0x57C98B);

    static final Font H1   = new Font("SansSerif", Font.BOLD, 20);
    static final Font H2   = new Font("SansSerif", Font.BOLD, 14);
    static final Font BODY = new Font("SansSerif", Font.PLAIN, 12);
    static final Font MONO = new Font("Monospaced", Font.PLAIN, 12);

    static Color factionColor(FactionId f) {
        if (f == null) {
            return INK_DIM;
        }
        switch (f) {
            case HUMAN:     return new Color(0x5B8DEF);
            case MINBARI:   return new Color(0x7FC7D9);
            case NARN:      return new Color(0xE0772F);
            case CENTAURI:  return new Color(0xC65BD3);
            case VORLON:    return new Color(0xE8E07A);
            case SHADOW:    return new Color(0x9B3B4E);
            case PSI_CORPS: return new Color(0x7A7FE0);
            case NONALIGNED:
            default:        return new Color(0x8A93AD);
        }
    }

    static Color conflictColor(ConflictType t) {
        if (t == null) {
            return INK_DIM;
        }
        switch (t) {
            case DIPLOMACY: return new Color(0x57C98B);
            case INTRIGUE:  return new Color(0xC65BD3);
            case PSI:       return new Color(0x7A7FE0);
            case MILITARY:  return new Color(0xE0556B);
            default:        return INK_DIM;
        }
    }
}
