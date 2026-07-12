package com.whim.firetop.ui;

import java.awt.Color;
import java.awt.Font;

/** Central palette and fonts so every screen reads as one system. */
public final class Theme {

    private Theme() { }

    public static final Color BG_DARK = new Color(24, 20, 28);
    public static final Color BG_PANEL = new Color(38, 32, 44);
    public static final Color STONE = new Color(70, 64, 78);
    public static final Color STONE_LIGHT = new Color(120, 110, 128);
    public static final Color PARCHMENT = new Color(232, 222, 200);
    public static final Color GOLD = new Color(214, 176, 92);
    public static final Color BLOOD = new Color(168, 58, 52);
    public static final Color EMERALD = new Color(96, 158, 106);
    public static final Color ROYAL = new Color(96, 120, 190);
    public static final Color REACHABLE = new Color(96, 158, 106);

    /** Distinct token colours for up to four adventurers. */
    public static final Color[] PLAYER_COLORS = {
            new Color(214, 176, 92),   // gold
            new Color(96, 120, 190),   // blue
            new Color(168, 58, 52),    // red
            new Color(96, 158, 106)    // green
    };

    public static final Font TITLE = new Font("Serif", Font.BOLD, 30);
    public static final Font HEADING = new Font("Serif", Font.BOLD, 18);
    public static final Font BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font BODY_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font MONO = new Font("Monospaced", Font.PLAIN, 12);
}
