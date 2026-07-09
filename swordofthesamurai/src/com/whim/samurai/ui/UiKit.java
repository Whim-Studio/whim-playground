package com.whim.samurai.ui;

import com.whim.samurai.render.Palette;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Consistent Swing styling helpers so every screen shares one visual language. */
public final class UiKit {
    private UiKit() { }

    public static final Font TITLE = new Font("Serif", Font.BOLD, 40);
    public static final Font HEAD  = new Font("Serif", Font.BOLD, 20);
    public static final Font BODY  = new Font("Serif", Font.PLAIN, 16);
    public static final Font MONO  = new Font("Monospaced", Font.PLAIN, 14);
    public static final Font SMALL = new Font("Serif", Font.PLAIN, 13);

    public static JButton button(String text) {
        JButton b = new JButton(text);
        b.setFont(HEAD);
        b.setForeground(Palette.INK);
        b.setBackground(Palette.PANEL);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.PANEL_LINE, 2),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)));
        b.setOpaque(true);
        return b;
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static void transparent(JComponent c) { c.setOpaque(false); }

    public static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /** Paint the shared aged-paper background used by every screen. */
    public static void paperBackground(Graphics2D g, int w, int h) {
        g.setColor(Palette.PAPER);
        g.fillRect(0, 0, w, h);
        // subtle vertical fibre streaks for a washi-paper feel
        g.setColor(Palette.PAPER_DK);
        for (int x = 0; x < w; x += 7) {
            if (((x / 7) & 3) == 0) g.drawLine(x, 0, x, h);
        }
    }
}
