package com.whim.kenshi.ui;

import com.whim.kenshi.api.Enums;

import java.awt.Color;

/**
 * Central colour constants for the whole UI: faction colours, terrain tints and
 * HUD chrome. Kept in one place so the {@link Renderer}, {@link Hud} and
 * {@link BodyChart} agree on a single palette. Everything here is drawn — no
 * external assets are used anywhere in the demake.
 */
public final class Palette {

    private Palette() {}

    // ---- HUD chrome ------------------------------------------------------
    public static final Color HUD_BG          = new Color(24, 22, 20);
    public static final Color HUD_PANEL       = new Color(34, 31, 28);
    public static final Color HUD_PANEL_LIGHT = new Color(46, 42, 38);
    public static final Color HUD_BORDER      = new Color(70, 62, 54);
    public static final Color HUD_TEXT        = new Color(222, 214, 200);
    public static final Color HUD_TEXT_DIM    = new Color(150, 142, 130);
    public static final Color HUD_ACCENT      = new Color(214, 176, 94);

    // ---- Selection / order feedback -------------------------------------
    public static final Color SELECT_RING     = new Color(120, 230, 140);
    public static final Color SELECT_RECT     = new Color(120, 230, 140, 60);
    public static final Color SELECT_RECT_EDGE = new Color(150, 240, 160, 200);
    public static final Color ORDER_MOVE      = new Color(120, 230, 140);
    public static final Color ORDER_ATTACK    = new Color(230, 90, 80);
    public static final Color ORDER_INTERACT  = new Color(120, 180, 240);

    // ---- Health grading (good -> bad) -----------------------------------
    public static final Color HP_GOOD = new Color(96, 200, 104);
    public static final Color HP_WARN = new Color(224, 196, 72);
    public static final Color HP_BAD  = new Color(214, 92, 72);
    public static final Color HP_OFF  = new Color(70, 66, 62); // disabled part
    public static final Color BLOOD   = new Color(196, 58, 52);
    public static final Color HUNGER  = new Color(196, 150, 70);
    public static final Color BLEED   = new Color(230, 70, 60);

    // ---- Terrain tints ---------------------------------------------------
    public static Color terrain(Enums.Terrain t) {
        switch (t) {
            case SAND:  return new Color(196, 172, 120);
            case SCRUB: return new Color(150, 152, 96);
            case GREEN: return new Color(96, 138, 78);
            case ROCK:  return new Color(122, 116, 110);
            case ASH:   return new Color(78, 72, 70);
            case WATER: return new Color(58, 96, 138);
            case TOWN:  return new Color(140, 116, 86);
            default:    return new Color(120, 120, 120);
        }
    }

    // ---- Faction colours -------------------------------------------------
    public static Color faction(Enums.FactionId f) {
        switch (f) {
            case PLAYER:         return new Color(88, 168, 220);
            case HOLY_NATION:    return new Color(226, 220, 206);
            case SHEK:           return new Color(198, 82, 68);
            case DUST_BANDITS:   return new Color(176, 120, 66);
            case HUNGRY_BANDITS: return new Color(150, 138, 92);
            case TRADE_GUILD:    return new Color(96, 176, 150);
            case DRIFTERS:       return new Color(150, 146, 140);
            default:             return new Color(180, 180, 180);
        }
    }

    /** A darker outline colour derived from a base fill. */
    public static Color darker(Color c, double f) {
        int r = (int) Math.max(0, c.getRed() * f);
        int g = (int) Math.max(0, c.getGreen() * f);
        int b = (int) Math.max(0, c.getBlue() * f);
        return new Color(r, g, b);
    }

    /** Colour-grade a 0..1 health fraction from red (bad) through amber to green. */
    public static Color grade(double frac) {
        if (frac <= 0.0) return HP_OFF;
        if (frac >= 1.0) return HP_GOOD;
        // interpolate bad -> warn -> good
        if (frac < 0.5) {
            return lerp(HP_BAD, HP_WARN, frac / 0.5);
        }
        return lerp(HP_WARN, HP_GOOD, (frac - 0.5) / 0.5);
    }

    public static Color lerp(Color a, Color b, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        int r = (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue()  - a.getBlue())  * t);
        return new Color(r, g, bl);
    }
}
