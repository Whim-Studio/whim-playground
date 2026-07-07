package com.whim.albion.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/** Small shared drawing helpers for the UI package. */
final class UiUtil {

    private UiUtil() {}

    static final Color PANEL_BG = new Color(28, 26, 34);
    static final Color PANEL_EDGE = new Color(90, 82, 66);
    static final Color INK = new Color(224, 216, 190);
    static final Color LP_COLOR = new Color(200, 70, 70);
    static final Color SP_COLOR = new Color(70, 120, 210);
    static final Font UI_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    static final Font UI_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    static final Font TITLE_FONT = new Font(Font.SERIF, Font.BOLD, 40);

    /** Draw a labelled value bar (LP/SP/XP). */
    static void bar(Graphics2D g, int x, int y, int w, int h, int cur, int max, Color fill, String label) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(x, y, w, h);
        int fw = max <= 0 ? 0 : (int) ((long) w * Math.max(0, Math.min(cur, max)) / max);
        g.setColor(fill);
        g.fillRect(x, y, fw, h);
        g.setColor(PANEL_EDGE);
        g.drawRect(x, y, w, h);
        if (label != null) {
            g.setColor(INK);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g.drawString(label + " " + cur + "/" + max, x + 3, y + h - 2);
        }
    }
}
