package com.arpg.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

/**
 * A custom Graphics2D-drawn horizontal stat bar (HP / resource / XP).
 * Draws a rounded track, a filled portion and centered "cur / max" text.
 */
public class StatBar extends JComponent {

    private long current;
    private long max;
    private Color fill;
    private String label;

    public StatBar(Color fill) {
        this.fill = fill;
        this.max = 1;
        this.current = 0;
        setPreferredSize(new Dimension(180, 18));
        setForeground(UiTheme.FG_TEXT);
    }

    public void setValues(long current, long max) {
        this.current = current;
        this.max = max;
        repaint();
    }

    public void setLabel(String label) {
        this.label = label;
        repaint();
    }

    public void setFill(Color fill) {
        this.fill = fill;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int arc = Math.min(h, 12);

        g2.setColor(UiTheme.BG_SLOT);
        g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

        double ratio = max <= 0 ? 0 : Math.max(0.0, Math.min(1.0, (double) current / (double) max));
        int fillW = (int) Math.round((w - 2) * ratio);
        if (fillW > 0) {
            g2.setColor(fill);
            g2.fillRoundRect(1, 1, Math.max(arc, fillW), h - 3, arc, arc);
        }

        g2.setColor(new Color(0, 0, 0, 90));
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

        String text = (label == null ? "" : label + "  ") + current + " / " + max;
        g2.setFont(UiTheme.BODY_BOLD);
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(text)) / 2;
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(new Color(0, 0, 0, 140));
        g2.drawString(text, tx + 1, ty + 1);
        g2.setColor(UiTheme.FG_TEXT);
        g2.drawString(text, tx, ty);

        g2.dispose();
    }
}
