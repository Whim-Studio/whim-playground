package com.whim.scg.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Small shared Java2D drawing helpers so every screen looks consistent. */
public final class UiKit {
    private UiKit() {}

    public static final Font H1   = new Font("SansSerif", Font.BOLD, 30);
    public static final Font H2   = new Font("SansSerif", Font.BOLD, 18);
    public static final Font BODY = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font MONO = new Font("Monospaced", Font.PLAIN, 13);

    public static void antialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    public static void panel(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(Palette.BG_PANEL);
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(Palette.GRID);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x, y, w, h, 10, 10);
    }

    public static void text(Graphics2D g, String s, int x, int y, Font f, Color c) {
        g.setFont(f); g.setColor(c); g.drawString(s, x, y);
    }

    public static void textCenter(Graphics2D g, String s, int cx, int y, Font f, Color c) {
        g.setFont(f); g.setColor(c);
        int w = g.getFontMetrics().stringWidth(s);
        g.drawString(s, cx - w / 2, y);
    }

    /** A labelled horizontal bar (hull/shield/charge/happiness). */
    public static void bar(Graphics2D g, int x, int y, int w, int h,
                           double frac, Color fill, Color track) {
        frac = Math.max(0, Math.min(1, frac));
        g.setColor(track); g.fillRoundRect(x, y, w, h, h, h);
        g.setColor(fill);  g.fillRoundRect(x, y, (int) (w * frac), h, h, h);
    }

    public static void button(Graphics2D g, String label, int x, int y, int w, int h, boolean hot) {
        g.setColor(hot ? Palette.ACCENT : Palette.BG_PANEL);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(Palette.GRID);
        g.drawRoundRect(x, y, w, h, 8, 8);
        textCenter(g, label, x + w / 2, y + h / 2 + 5, BODY, hot ? Palette.BG : Palette.INK);
    }
}
