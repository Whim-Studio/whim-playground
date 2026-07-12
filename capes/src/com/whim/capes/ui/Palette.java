package com.whim.capes.ui;

import java.awt.Color;
import java.awt.Font;

/**
 * Central colour and font palette so every view reads as one comic-book-styled
 * system. All art in this app is programmatically drawn (no copyrighted
 * material); the palette anchors that look.
 */
public final class Palette {
    private Palette() {}

    public static final Color INK        = new Color(0x1A1A22);
    public static final Color PAPER       = new Color(0xF5F1E6);
    public static final Color PANEL       = new Color(0xFFFFFF);
    public static final Color PANEL_EDGE  = new Color(0x2A2A33);

    public static final Color HERO_BLUE   = new Color(0x2C6BB3);
    public static final Color VILLAIN_RED = new Color(0xB33A3A);
    public static final Color GOLD        = new Color(0xE0A93C); // Story Tokens / Inspirations accent
    public static final Color DEBT        = new Color(0x6E4E9E); // Debt accent
    public static final Color CONTROL     = new Color(0x2E7D4F); // controlling side highlight
    public static final Color MUTED       = new Color(0x7A7568);

    public static final Font TITLE   = new Font("SansSerif", Font.BOLD, 22);
    public static final Font HEADING = new Font("SansSerif", Font.BOLD, 15);
    public static final Font BODY    = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font MONO    = new Font("Monospaced", Font.PLAIN, 12);
    public static final Font DIE     = new Font("SansSerif", Font.BOLD, 16);
}
