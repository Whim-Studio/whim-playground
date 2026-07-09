package com.whim.samurai.render;

import java.awt.Color;

/**
 * A small, internally-consistent woodblock-print (ukiyo-e) inspired palette so
 * every screen reads as one game: aged washi paper, sumi ink, cinnabar seals,
 * indigo and moss. No copyrighted assets are used — see README.
 */
public final class Palette {
    private Palette() { }

    public static final Color PAPER      = new Color(226, 214, 186); // washi paper
    public static final Color PAPER_DK   = new Color(205, 190, 158);
    public static final Color INK        = new Color(30, 26, 22);     // sumi ink
    public static final Color INK_SOFT   = new Color(72, 62, 52);
    public static final Color PANEL      = new Color(238, 228, 202);
    public static final Color PANEL_LINE = new Color(120, 96, 62);
    public static final Color CINNABAR   = new Color(176, 58, 46);    // seal red / blood
    public static final Color CINNABAR_DK= new Color(120, 38, 30);
    public static final Color GOLD       = new Color(180, 140, 60);   // honor
    public static final Color INDIGO     = new Color(46, 66, 104);    // power / rival
    public static final Color MOSS       = new Color(96, 112, 66);    // land / rice
    public static final Color JADE       = new Color(70, 120, 96);
    public static final Color DIM        = new Color(120, 104, 82);
    public static final Color GOOD       = new Color(70, 120, 60);
    public static final Color DANGER     = new Color(176, 58, 46);

    // Clan accent colours for the strategic map.
    public static final Color[] CLAN = {
        new Color(176, 58, 46),   // cinnabar
        new Color(46, 66, 104),   // indigo
        new Color(96, 112, 66),   // moss
        new Color(120, 80, 140),  // murasaki purple
        new Color(60, 110, 120),  // teal
        new Color(150, 110, 50),  // ochre
        new Color(110, 60, 50),   // brown
        new Color(80, 90, 100),   // slate
    };
}
