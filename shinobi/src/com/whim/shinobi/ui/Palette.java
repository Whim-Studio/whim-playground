package com.whim.shinobi.ui;

import java.awt.Color;

/**
 * Named colors for the Shinobi renderer. Everything is drawn procedurally, so this
 * is the single place that fixes the palette. UPPER-plane variants are dimmer/bluer
 * to read as background depth; LOWER-plane variants are brighter foreground.
 */
public final class Palette {
    private Palette() {}

    // --- Background / sky ---
    public static final Color SKY_TOP    = new Color(18, 12, 34);
    public static final Color SKY_BOTTOM = new Color(46, 26, 58);

    // --- Joe Musashi (player) ---
    public static final Color JOE_SUIT   = new Color(238, 238, 240); // white ninja gi
    public static final Color JOE_TRIM   = new Color(206, 34, 44);   // red scarf/trim
    public static final Color JOE_SKIN   = new Color(226, 176, 140);

    // --- Enemies ---
    public static final Color THUG_BODY  = new Color(196, 120, 40);  // orange-brown thug
    public static final Color THUG_TRIM  = new Color(120, 68, 22);
    public static final Color NINJA_BODY = new Color(56, 84, 168);   // blue ninja
    public static final Color NINJA_TRIM = new Color(28, 40, 96);
    public static final Color NINJA_BLOCK = new Color(150, 200, 255); // deflect glow

    // --- Hostage ---
    public static final Color HOSTAGE_BODY  = new Color(210, 196, 120);
    public static final Color HOSTAGE_ROPE  = new Color(150, 110, 60);
    public static final Color HOSTAGE_FREED = new Color(120, 220, 140);

    // --- Projectiles ---
    public static final Color SHURIKEN = new Color(220, 220, 230);
    public static final Color KNIFE    = new Color(200, 220, 255);
    public static final Color GUN_SHOT = new Color(255, 230, 120);
    public static final Color ENEMY_SHOT = new Color(255, 120, 90);

    // --- Terrain per plane ---
    public static final Color GROUND_LOWER = new Color(70, 54, 40);
    public static final Color GROUND_LOWER_TOP = new Color(104, 80, 56);
    public static final Color GROUND_UPPER = new Color(52, 58, 88);   // bluer/dimmer for depth
    public static final Color GROUND_UPPER_TOP = new Color(78, 88, 128);

    // --- HUD ---
    public static final Color HUD_BG    = new Color(8, 8, 14);
    public static final Color HUD_TEXT  = new Color(240, 240, 245);
    public static final Color HUD_LABEL = new Color(230, 190, 70);
    public static final Color HUD_RED   = new Color(220, 60, 60);
    public static final Color HUD_NINJUTSU = new Color(150, 200, 255);

    public static final Color FLASH    = new Color(255, 255, 255);
    public static final Color OVERLAY  = new Color(0, 0, 0, 150);

    /** Desaturate/dim a color toward the UPPER-plane background feel. */
    public static Color depth(Color c) {
        int r = (c.getRed()   * 6 + 40 * 4) / 10;
        int g = (c.getGreen() * 6 + 46 * 4) / 10;
        int b = (c.getBlue()  * 6 + 80 * 4) / 10;
        return new Color(clamp(r), clamp(g), clamp(b));
    }

    private static int clamp(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }
}
