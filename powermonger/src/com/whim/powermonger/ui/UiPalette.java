package com.whim.powermonger.ui;

import java.awt.Color;

import com.whim.powermonger.api.Enums.Allegiance;
import com.whim.powermonger.api.Enums.Job;
import com.whim.powermonger.api.Enums.TerrainType;

/**
 * Named colours for terrain, factions and UI chrome. Earthy topographic palette
 * in the spirit of Powermonger's map. Pure {@code java.awt.Color} constants and a
 * couple of helpers; no assets.
 */
public final class UiPalette {
    private UiPalette() {}

    // --- UI chrome -------------------------------------------------------
    public static final Color PANEL_BG      = new Color(38, 32, 26);
    public static final Color PANEL_BG_DARK = new Color(24, 20, 16);
    public static final Color PANEL_EDGE    = new Color(96, 82, 60);
    public static final Color PARCHMENT     = new Color(214, 196, 158);
    public static final Color INK           = new Color(30, 24, 16);
    public static final Color TEXT_LIGHT    = new Color(226, 214, 188);
    public static final Color TEXT_DIM      = new Color(150, 138, 116);
    public static final Color HILIGHT       = new Color(240, 208, 120);
    public static final Color SHADOW        = new Color(0, 0, 0, 90);

    // --- Factions --------------------------------------------------------
    public static final Color PLAYER   = new Color(60, 110, 200);
    public static final Color ENEMY    = new Color(190, 56, 44);
    public static final Color NEUTRAL  = new Color(180, 168, 120);

    // --- Terrain base colours -------------------------------------------
    public static final Color DEEP_WATER    = new Color(28, 52, 96);
    public static final Color SHALLOW_WATER = new Color(58, 104, 150);
    public static final Color BEACH         = new Color(206, 186, 130);
    public static final Color GRASS         = new Color(96, 132, 62);
    public static final Color FOREST        = new Color(52, 92, 48);
    public static final Color HILL          = new Color(126, 112, 72);
    public static final Color MOUNTAIN      = new Color(120, 108, 100);
    public static final Color TOWN          = new Color(150, 118, 78);
    public static final Color SNOW          = new Color(238, 240, 246);

    public static Color terrain(TerrainType t) {
        if (t == null) return GRASS;
        switch (t) {
            case DEEP_WATER:    return DEEP_WATER;
            case SHALLOW_WATER: return SHALLOW_WATER;
            case BEACH:         return BEACH;
            case GRASS:         return GRASS;
            case FOREST:        return FOREST;
            case HILL:          return HILL;
            case MOUNTAIN:      return MOUNTAIN;
            case TOWN:          return TOWN;
            default:            return GRASS;
        }
    }

    public static Color faction(Allegiance a) {
        if (a == null) return NEUTRAL;
        switch (a) {
            case PLAYER: return PLAYER;
            case ENEMY:  return ENEMY;
            default:     return NEUTRAL;
        }
    }

    public static Color job(Job j) {
        if (j == null) return NEUTRAL;
        switch (j) {
            case FARMING:  return new Color(212, 180, 84);
            case FISHING:  return new Color(96, 168, 196);
            case HERDING:  return new Color(180, 140, 96);
            case CRAFTING: return new Color(196, 120, 72);
            default:       return new Color(170, 160, 140);
        }
    }

    /** Lighten a colour toward white by {@code f} in [0,1]. */
    public static Color lighten(Color c, double f) {
        f = clamp01(f);
        int r = (int) Math.round(c.getRed()   + (255 - c.getRed())   * f);
        int g = (int) Math.round(c.getGreen() + (255 - c.getGreen()) * f);
        int b = (int) Math.round(c.getBlue()  + (255 - c.getBlue())  * f);
        return new Color(r, g, b);
    }

    /** Darken a colour toward black by {@code f} in [0,1]. */
    public static Color darken(Color c, double f) {
        f = clamp01(f);
        int r = (int) Math.round(c.getRed()   * (1 - f));
        int g = (int) Math.round(c.getGreen() * (1 - f));
        int b = (int) Math.round(c.getBlue()  * (1 - f));
        return new Color(r, g, b);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
