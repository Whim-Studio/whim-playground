package com.whim.civ.ui;

import com.whim.civ.domain.Terrain;

import java.awt.Color;
import java.awt.Font;

/**
 * Central palette and font helpers for the Civilization UI. Terrain colours give the
 * top-down square-tiled map its at-a-glance readability; civ colours keep unit/city
 * stacks distinguishable. Pure constants — no Swing state, safe to touch off the EDT.
 */
final class UiTheme {

    private UiTheme() { }

    static final Color GRID = new Color(0, 0, 0, 60);
    static final Color SELECTION = new Color(255, 235, 59);
    static final Color GOODY = new Color(255, 215, 0);
    static final Color ROAD = new Color(120, 90, 50);
    static final Color RAILROAD = new Color(60, 60, 70);
    static final Color PANEL_BG = new Color(34, 38, 46);
    static final Color PANEL_FG = new Color(230, 232, 236);
    static final Color ACCENT = new Color(90, 160, 240);

    private static final Color[] CIV_COLORS = {
        new Color(220, 60, 60),    // 0 red
        new Color(60, 120, 220),   // 1 blue
        new Color(70, 180, 90),    // 2 green
        new Color(230, 180, 50),   // 3 yellow
        new Color(170, 90, 200),   // 4 purple
        new Color(230, 130, 50),   // 5 orange
        new Color(60, 200, 200),   // 6 cyan
        new Color(230, 120, 170)   // 7 pink
    };

    static Color civColor(int civId) {
        if (civId < 0) {
            return Color.LIGHT_GRAY;
        }
        return CIV_COLORS[civId % CIV_COLORS.length];
    }

    static Color terrainColor(Terrain t) {
        switch (t) {
            case OCEAN:     return new Color(40, 90, 160);
            case GRASSLAND: return new Color(96, 168, 72);
            case PLAINS:    return new Color(176, 184, 96);
            case FOREST:    return new Color(40, 104, 56);
            case HILLS:     return new Color(150, 134, 84);
            case MOUNTAINS: return new Color(128, 120, 112);
            case DESERT:    return new Color(224, 208, 136);
            case TUNDRA:    return new Color(180, 188, 176);
            case ARCTIC:    return new Color(238, 244, 248);
            case SWAMP:     return new Color(86, 104, 80);
            case JUNGLE:    return new Color(64, 128, 64);
            default:        return Color.GRAY;
        }
    }

    static final Font H1 = new Font("SansSerif", Font.BOLD, 16);
    static final Font H2 = new Font("SansSerif", Font.BOLD, 13);
    static final Font BODY = new Font("SansSerif", Font.PLAIN, 12);
    static final Font MONO = new Font("Monospaced", Font.PLAIN, 12);

    /** Contrasting ink for text drawn over a terrain tile. */
    static Color inkFor(Color bg) {
        double lum = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue());
        return lum > 150 ? Color.BLACK : Color.WHITE;
    }
}
