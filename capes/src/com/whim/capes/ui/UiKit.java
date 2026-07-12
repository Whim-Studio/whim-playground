package com.whim.capes.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Programmatic {@link Graphics2D} glyphs for the game's tokens and dice — the
 * "geometric placeholder" art the constraints call for (no copyrighted comic
 * art). Views call these to draw pips, tokens and index-card frames
 * consistently. Kept in the UI layer and free of model dependencies so it is
 * purely presentational.
 */
public final class UiKit {
    private UiKit() {}

    public static void antialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /** Draws a rounded index-card frame at (x,y) with the given size and edge colour. */
    public static void indexCard(Graphics2D g, int x, int y, int w, int h, Color fill, Color edge) {
        g.setColor(fill);
        g.fillRoundRect(x, y, w, h, 14, 14);
        g.setColor(edge);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, 14, 14);
    }

    /** Draws a six-sided die face showing {@code value} pips, side coloured by {@code accent}. */
    public static void die(Graphics2D g, int x, int y, int size, int value, Color accent) {
        antialias(g);
        g.setColor(Palette.PANEL);
        g.fillRoundRect(x, y, size, size, size / 4, size / 4);
        g.setColor(accent);
        g.setStroke(new BasicStroke(2.2f));
        g.drawRoundRect(x, y, size, size, size / 4, size / 4);

        int v = Math.max(1, Math.min(6, value));
        int r = Math.max(2, size / 12);
        int cx = x + size / 2, cy = y + size / 2;
        int off = size / 4;
        g.setColor(Palette.INK);
        // pip layout by value
        boolean[] p = pips(v);
        int[][] pos = {
            {x + off, y + off}, {cx, y + off}, {x + size - off, y + off},
            {x + off, cy},      {cx, cy},      {x + size - off, cy},
            {x + off, y + size - off}, {cx, y + size - off}, {x + size - off, y + size - off}
        };
        for (int i = 0; i < 9; i++) {
            if (p[i]) g.fillOval(pos[i][0] - r, pos[i][1] - r, r * 2, r * 2);
        }
    }

    private static boolean[] pips(int v) {
        // indices: 0..8 in a 3x3 grid, row-major
        boolean[] p = new boolean[9];
        switch (v) {
            case 1: p[4] = true; break;
            case 2: p[0] = p[8] = true; break;
            case 3: p[0] = p[4] = p[8] = true; break;
            case 4: p[0] = p[2] = p[6] = p[8] = true; break;
            case 5: p[0] = p[2] = p[4] = p[6] = p[8] = true; break;
            case 6: p[0] = p[2] = p[3] = p[5] = p[6] = p[8] = true; break;
            default: p[4] = true;
        }
        return p;
    }

    /** Draws a round resource token (Debt / Story / Inspiration) with a single-letter glyph. */
    public static void token(Graphics2D g, int x, int y, int size, Color fill, String glyph) {
        antialias(g);
        g.setColor(fill);
        g.fillOval(x, y, size, size);
        g.setColor(Palette.INK);
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(x, y, size, size);
        if (glyph != null && !glyph.isEmpty()) {
            g.setColor(Color.WHITE);
            g.setFont(Palette.DIE.deriveFont((float) size * 0.55f));
            int tw = g.getFontMetrics().stringWidth(glyph);
            int th = g.getFontMetrics().getAscent();
            g.drawString(glyph, x + (size - tw) / 2, y + (size + th) / 2 - 2);
        }
    }
}
