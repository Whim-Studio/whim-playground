package com.whim.warroom.ui;

import java.awt.Color;
import java.awt.Font;

/**
 * Shared colors and fonts for the War Room UI. Everything is procedural; no
 * asset loading. A dark "command console" palette keeps the battlefield the
 * visual focus while chrome recedes.
 */
public final class ThemeUI {
    private ThemeUI() {}

    // Chrome / panels
    public static final Color BG_DEEP    = new Color(18, 22, 28);
    public static final Color BG_PANEL   = new Color(28, 34, 42);
    public static final Color BG_PANEL_2 = new Color(38, 46, 56);
    public static final Color BORDER     = new Color(64, 78, 92);
    public static final Color ACCENT     = new Color(90, 200, 190);
    public static final Color ACCENT_DIM = new Color(60, 130, 124);
    public static final Color WARN       = new Color(230, 170, 60);
    public static final Color DANGER     = new Color(220, 80, 70);

    // Text
    public static final Color TEXT       = new Color(214, 224, 232);
    public static final Color TEXT_DIM   = new Color(140, 156, 168);
    public static final Color TEXT_MUTED = new Color(96, 110, 122);

    // Battlefield overlays
    public static final Color GRID_LINE  = new Color(0, 0, 0, 40);
    public static final Color SELECT     = new Color(255, 240, 120);
    public static final Color ROUTE_LINE = new Color(240, 236, 180, 200);
    public static final Color BLAST_CORE = new Color(255, 210, 120);
    public static final Color BLAST_EDGE = new Color(255, 90, 40);

    // Fonts
    public static final Font UI       = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font UI_BOLD  = new Font("SansSerif", Font.BOLD, 12);
    public static final Font UI_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font TITLE    = new Font("SansSerif", Font.BOLD, 15);
    public static final Font MONO     = new Font("Monospaced", Font.PLAIN, 12);
    public static final Font HUD      = new Font("Monospaced", Font.BOLD, 13);

    /** Blend two colors: t=0 → a, t=1 → b. */
    public static Color mix(Color a, Color b, double t) {
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return new Color(
            (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t),
            (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
    }

    /** Same color with a new alpha (0..255). */
    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a < 0 ? 0 : (a > 255 ? 255 : a));
    }
}
