package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.Ids;

import java.awt.Color;
import java.awt.Font;

/**
 * Central dark-space colour and font palette for the whole UI. Everything the UI
 * draws pulls its colours from here so the theme stays coherent. Pure constants —
 * no Swing state, safe to touch from any thread (values are immutable).
 */
public final class Palette {

    private Palette() {
    }

    // --- backgrounds ---
    public static final Color BG_DEEP = new Color(7, 9, 18);
    public static final Color BG_SPACE = new Color(11, 14, 26);
    public static final Color BG_PANEL = new Color(18, 22, 38);
    public static final Color BG_PANEL_HI = new Color(28, 34, 54);
    public static final Color BG_CELL = new Color(23, 28, 46);
    public static final Color BORDER = new Color(48, 60, 92);
    public static final Color BORDER_HI = new Color(74, 96, 150);

    // --- text ---
    public static final Color TEXT = new Color(216, 226, 242);
    public static final Color TEXT_DIM = new Color(140, 152, 178);
    public static final Color TEXT_FAINT = new Color(96, 106, 132);

    // --- accents / status ---
    public static final Color ACCENT = new Color(84, 184, 255);
    public static final Color ACCENT2 = new Color(158, 118, 255);
    public static final Color OK = new Color(96, 214, 132);
    public static final Color WARN = new Color(244, 192, 74);
    public static final Color BAD = new Color(242, 104, 92);

    // --- resources ---
    public static final Color METAL = new Color(184, 188, 198);
    public static final Color CRYSTAL = new Color(122, 202, 255);
    public static final Color DEUTERIUM = new Color(112, 232, 208);
    public static final Color ENERGY = new Color(250, 220, 96);
    public static final Color DARK_MATTER = new Color(196, 132, 255);

    // --- fonts ---
    public static final Font FONT = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 12);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 16);
    public static final Font FONT_BIG = new Font("SansSerif", Font.BOLD, 22);
    public static final Font FONT_MONO = new Font("Monospaced", Font.PLAIN, 12);

    public static Color resourceColor(Ids.ResourceType type) {
        switch (type) {
            case METAL: return METAL;
            case CRYSTAL: return CRYSTAL;
            case DEUTERIUM: return DEUTERIUM;
            case ENERGY: return ENERGY;
            case DARK_MATTER: return DARK_MATTER;
            default: return TEXT;
        }
    }

    /** Colour used to tag a log/event line by its category. */
    public static Color logColor(Ids.LogCategory cat) {
        if (cat == null) return TEXT_DIM;
        switch (cat) {
            case ECONOMY: return METAL;
            case RESEARCH: return CRYSTAL;
            case CONSTRUCTION: return new Color(150, 200, 120);
            case FLEET: return ACCENT;
            case COMBAT: return BAD;
            case EXPEDITION: return ACCENT2;
            case ESPIONAGE: return new Color(120, 220, 190);
            case AI: return WARN;
            case SYSTEM: return TEXT_DIM;
            default: return TEXT_DIM;
        }
    }

    /** Blend two colours by t in [0,1]. */
    public static Color mix(Color a, Color b, double t) {
        double u = 1 - t;
        return new Color(
                clamp((int) (a.getRed() * u + b.getRed() * t)),
                clamp((int) (a.getGreen() * u + b.getGreen() * t)),
                clamp((int) (a.getBlue() * u + b.getBlue() * t)));
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), clamp(a));
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
