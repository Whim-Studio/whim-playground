package com.whim.starcommand.render;

import java.awt.Color;

/** A small, internally-consistent colour palette so every screen reads as one game. */
public final class Palette {
    private Palette() { }

    public static final Color SPACE      = new Color(6, 8, 18);
    public static final Color PANEL      = new Color(14, 20, 38);
    public static final Color PANEL_LINE = new Color(40, 70, 120);
    public static final Color TEXT       = new Color(200, 220, 255);
    public static final Color TEXT_DIM   = new Color(120, 140, 180);
    public static final Color ACCENT     = new Color(90, 200, 255);   // cyan HUD
    public static final Color ACCENT_2   = new Color(255, 180, 60);   // amber
    public static final Color DANGER     = new Color(255, 90, 90);
    public static final Color GOOD       = new Color(90, 230, 140);
    public static final Color CORE       = new Color(60, 120, 220);
    public static final Color ALPHA      = new Color(210, 90, 60);
    public static final Color BETA       = new Color(150, 90, 200);
}
