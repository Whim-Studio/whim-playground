package com.whim.populous.ui;

import java.awt.Color;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.TerrainType;

/**
 * Central palette for the Populous UI. All colours are procedural constants —
 * no image assets. Maps a {@link TerrainType} (plus a small elevation nudge) to
 * a colour ramp, and allegiance to entity colours (Good = blue, Evil = red).
 *
 * The ramp runs, roughly by rising elevation:
 *   deep blue WATER -> lighter SHALLOW -> sandy SAND -> greens (GRASS/HILL) ->
 *   grey ROCK/MOUNTAIN -> orange/black LAVA, with murky SWAMP off to the side.
 */
public final class UiColors {

    private UiColors() { }

    // ---- Terrain base colours ------------------------------------------------
    public static final Color WATER_DEEP    = new Color(18, 38, 92);
    public static final Color WATER         = new Color(28, 60, 140);
    public static final Color SHALLOW       = new Color(58, 120, 190);
    public static final Color SAND          = new Color(214, 196, 132);
    public static final Color GRASS         = new Color(96, 158, 70);
    public static final Color GRASS_DARK    = new Color(66, 122, 52);
    public static final Color HILL          = new Color(120, 150, 72);
    public static final Color MOUNTAIN      = new Color(140, 132, 118);
    public static final Color ROCK          = new Color(110, 104, 98);
    public static final Color ROCK_HIGH     = new Color(176, 172, 168);
    public static final Color LAVA          = new Color(224, 96, 24);
    public static final Color LAVA_HOT      = new Color(255, 196, 60);
    public static final Color SWAMP         = new Color(74, 78, 54);

    // ---- Entity / faction colours -------------------------------------------
    public static final Color GOOD          = new Color(74, 150, 255);
    public static final Color GOOD_DARK     = new Color(30, 84, 190);
    public static final Color EVIL          = new Color(224, 64, 56);
    public static final Color EVIL_DARK     = new Color(150, 26, 22);
    public static final Color NEUTRAL       = new Color(200, 200, 200);

    // ---- HUD chrome ----------------------------------------------------------
    public static final Color HUD_BG        = new Color(24, 24, 30);
    public static final Color HUD_PANEL     = new Color(38, 38, 48);
    public static final Color HUD_BORDER    = new Color(70, 70, 86);
    public static final Color HUD_TEXT      = new Color(224, 224, 232);
    public static final Color HUD_TEXT_DIM  = new Color(150, 150, 162);
    public static final Color MANA          = new Color(120, 90, 220);
    public static final Color MANA_BG       = new Color(48, 40, 70);
    public static final Color HIGHLIGHT     = new Color(255, 214, 90);

    /** Base colour for a terrain type before elevation shading. */
    public static Color terrainBase(TerrainType t) {
        switch (t) {
            case WATER:    return WATER;
            case SHALLOW:  return SHALLOW;
            case SAND:     return SAND;
            case GRASS:    return GRASS;
            case HILL:     return HILL;
            case MOUNTAIN: return MOUNTAIN;
            case ROCK:     return ROCK;
            case LAVA:     return LAVA;
            case SWAMP:    return SWAMP;
            default:       return NEUTRAL;
        }
    }

    /**
     * Terrain colour shaded by elevation so height reads through the colour ramp
     * even in a strict top-down view. {@code norm} is a -1..+1 hint of how high
     * this tile is relative to its band (below-sea deepens blue; high land
     * brightens toward rock/snow).
     */
    public static Color terrainShaded(TerrainType t, double norm) {
        Color base = terrainBase(t);
        if (t == TerrainType.WATER || t == TerrainType.SHALLOW) {
            // Deeper water -> darker. norm<0 means deeper.
            return blend(base, WATER_DEEP, clamp(-norm, 0.0, 1.0) * 0.7);
        }
        if (t == TerrainType.LAVA) {
            return blend(base, LAVA_HOT, clamp(norm, 0.0, 1.0));
        }
        if (t == TerrainType.ROCK || t == TerrainType.MOUNTAIN) {
            return blend(base, ROCK_HIGH, clamp(norm, 0.0, 1.0) * 0.8);
        }
        // Grass / hill / sand: lighten as it rises, darken in hollows.
        double f = clamp(norm, -1.0, 1.0);
        return f >= 0 ? lighten(base, f * 0.28) : blend(base, GRASS_DARK, -f * 0.35);
    }

    public static Color allegianceColor(Allegiance a) {
        switch (a) {
            case GOOD: return GOOD;
            case EVIL: return EVIL;
            default:   return NEUTRAL;
        }
    }

    public static Color allegianceDark(Allegiance a) {
        switch (a) {
            case GOOD: return GOOD_DARK;
            case EVIL: return EVIL_DARK;
            default:   return NEUTRAL.darker();
        }
    }

    // ---- small colour math ---------------------------------------------------

    public static Color blend(Color a, Color b, double t) {
        double f = clamp(t, 0.0, 1.0);
        int r = (int) Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * f);
        int g = (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * f);
        int bl = (int) Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * f);
        return new Color(clampInt(r), clampInt(g), clampInt(bl));
    }

    public static Color lighten(Color c, double f) {
        return blend(c, Color.WHITE, f);
    }

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), clampInt(alpha));
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static int clampInt(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
