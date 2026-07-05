package com.whim.oggalaxy.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * A slim themed progress bar with an overlaid centred label. Used for construction,
 * research and shipyard-queue progress everywhere in the UI.
 */
public final class Gauge extends JComponent {

    private double fraction;
    private String text = "";
    private Color fill = Palette.ACCENT;

    public Gauge() {
        setPreferredSize(new Dimension(180, 18));
    }

    public void set(double fraction, String text, Color fill) {
        this.fraction = Math.max(0, Math.min(1, fraction));
        this.text = text == null ? "" : text;
        this.fill = fill == null ? Palette.ACCENT : fill;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        SpaceArt.hints(g2);
        int w = getWidth(), h = getHeight();
        g2.setColor(Palette.BG_DEEP);
        g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
        int fw = (int) ((w - 2) * fraction);
        if (fw > 0) {
            g2.setColor(Palette.mix(fill, Palette.BG_PANEL, 0.15));
            g2.fillRoundRect(1, 1, Math.max(h, fw), h - 2, h, h);
            g2.setColor(Palette.alpha(Palette.mix(fill, Color.WHITE, 0.4), 120));
            g2.fillRoundRect(1, 1, Math.max(h, fw), (h - 2) / 2, h, h);
        }
        g2.setColor(Palette.BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
        if (!text.isEmpty()) {
            g2.setFont(Palette.FONT_SMALL);
            java.awt.FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(text)) / 2;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(new Color(0, 0, 0, 160));
            g2.drawString(text, tx + 1, ty + 1);
            g2.setColor(Palette.TEXT);
            g2.drawString(text, tx, ty);
        }
        g2.dispose();
    }
}
