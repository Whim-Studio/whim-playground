package com.whim.powermonger.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Polygon;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import com.whim.powermonger.api.GameController;
import com.whim.powermonger.api.Views.GameStateView;

/**
 * Balance-of-Power scale along the bottom: a horizontal bar from enemy (left,
 * red) to player (right, blue) with a sliding indicator at the current balance
 * in [-1,+1]. Also shows season, weather and a status line.
 */
public final class BalancePanel extends JPanel {

    private final GameController controller;

    public BalancePanel(GameController controller) {
        this.controller = controller;
        setBackground(UiPalette.PANEL_BG);
        setPreferredSize(new Dimension(600, 62));
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GameStateView st = controller.state();

        int pad = 16;
        int barX = pad;
        int barY = 14;
        int barW = getWidth() - pad * 2;
        int barH = 16;

        // Gradient bar enemy -> neutral -> player.
        g.setPaint(new GradientPaint(barX, 0, UiPalette.ENEMY,
                barX + barW, 0, UiPalette.PLAYER));
        g.fillRoundRect(barX, barY, barW, barH, 8, 8);
        g.setPaint(null);
        g.setColor(UiPalette.PANEL_EDGE);
        g.drawRoundRect(barX, barY, barW, barH, 8, 8);

        // Centre tick.
        int midX = barX + barW / 2;
        g.setColor(UiPalette.INK);
        g.drawLine(midX, barY - 3, midX, barY + barH + 3);

        // Indicator.
        double bop = clamp(st.balanceOfPower(), -1, 1);
        int ix = barX + (int) Math.round((bop + 1) / 2.0 * barW);
        Polygon marker = new Polygon();
        marker.addPoint(ix, barY - 6);
        marker.addPoint(ix - 6, barY - 14);
        marker.addPoint(ix + 6, barY - 14);
        g.setColor(UiPalette.HILIGHT);
        g.fillPolygon(marker);
        g.setColor(UiPalette.INK);
        g.drawPolygon(marker);
        g.setStroke(new BasicStroke(2f));
        g.setColor(UiPalette.HILIGHT);
        g.drawLine(ix, barY, ix, barY + barH);

        // Labels.
        g.setFont(getFont().deriveFont(11f));
        g.setColor(UiPalette.TEXT_LIGHT);
        g.drawString("ENEMY", barX, barY + barH + 16);
        String pl = "PLAYER";
        int plW = g.getFontMetrics().stringWidth(pl);
        g.drawString(pl, barX + barW - plW, barY + barH + 16);

        // Season / weather / status.
        String meta = st.season() + " · " + st.weather()
                + " · move x" + fmt(st.movementFactor())
                + " · BoP " + fmt(bop)
                + " · tick " + st.tickCount();
        g.setColor(UiPalette.TEXT_DIM);
        int metaW = g.getFontMetrics().stringWidth(meta);
        g.drawString(meta, (getWidth() - metaW) / 2, barY + barH + 16);

        if (st.gameOver()) {
            g.setColor(st.playerWon() ? UiPalette.PLAYER : UiPalette.ENEMY);
            g.setFont(getFont().deriveFont(java.awt.Font.BOLD, 13f));
            String msg = st.playerWon() ? "VICTORY" : "DEFEAT";
            if (st.statusMessage() != null && !st.statusMessage().isEmpty()) {
                msg = st.statusMessage();
            }
            g.drawString(msg, midX - g.getFontMetrics().stringWidth(msg) / 2, barY + barH + 30);
        }
    }

    private static String fmt(double v) {
        return String.valueOf(Math.round(v * 100) / 100.0);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
