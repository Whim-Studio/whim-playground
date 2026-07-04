package com.whim.swd6.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;

/**
 * A small original color / stroke / font theme for the UI: deep-space blues with
 * amber accents. Intentionally generic — no trademarked marks, logos or palettes.
 *
 * Owned by Task 3 (ui). Pure constants + tiny helpers.
 */
public final class Palette {

    private Palette() {
    }

    // ----- core space palette -----
    public static final Color SPACE_DEEP   = new Color(0x0A0E1A); // near-black blue
    public static final Color SPACE_PANEL  = new Color(0x121A2E); // panel fill
    public static final Color SPACE_RAISED = new Color(0x1B2542); // raised card
    public static final Color GRID_LINE    = new Color(0x24314F); // hairline grid
    public static final Color STAR_DIM     = new Color(0x33406A);

    // ----- accents -----
    public static final Color AMBER        = new Color(0xF2B138); // primary accent
    public static final Color AMBER_DIM    = new Color(0xB07E1E);
    public static final Color CYAN         = new Color(0x4FD2E0); // secondary accent
    public static final Color TEAL         = new Color(0x2E8B99);

    // ----- semantic -----
    public static final Color OK           = new Color(0x5FD08A); // success green
    public static final Color WARN         = new Color(0xE8A13A);
    public static final Color DANGER       = new Color(0xE0563E); // failure / danger red
    public static final Color FORCE        = new Color(0x8E7BFF); // force-purple

    // ----- text -----
    public static final Color TEXT         = new Color(0xE6ECF7);
    public static final Color TEXT_DIM     = new Color(0x9AA6C0);
    public static final Color TEXT_FAINT   = new Color(0x63709A);

    // ----- strokes -----
    public static final Stroke HAIRLINE = new BasicStroke(1f);
    public static final Stroke FRAME    = new BasicStroke(2f);
    public static final Stroke THICK    = new BasicStroke(3f);

    // ----- fonts -----
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 22);
    public static final Font HEAD  = new Font("SansSerif", Font.BOLD, 15);
    public static final Font BODY  = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font SMALL = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font MONO  = new Font("Monospaced", Font.BOLD, 14);
    public static final Font DICE  = new Font("Monospaced", Font.BOLD, 16);

    /** Blend two colors by t in [0,1]. */
    public static Color blend(Color a, Color b, float t) {
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        int r = Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t);
        return new Color(r, g, bl);
    }

    /** A translucent copy of a color. */
    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }
}
