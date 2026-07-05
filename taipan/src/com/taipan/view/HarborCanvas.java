package com.taipan.view;

import com.taipan.model.GameState;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Simple placeholder art drawn entirely with Graphics2D — no external image
 * assets. Renders a stylised harbour scene: sky, sea, a lorcha whose sails
 * darken as hull damage rises, and a gun count.
 */
public class HarborCanvas extends JComponent {

    private GameState state;

    public HarborCanvas() {
        setPreferredSize(new Dimension(240, 160));
    }

    public void setState(GameState state) {
        this.state = state;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        // Sky and sea.
        g.setColor(new Color(0xBFD8E6));
        g.fillRect(0, 0, w, h);
        int seaY = (int) (h * 0.62);
        g.setColor(new Color(0x2E6E8E));
        g.fillRect(0, seaY, w, h - seaY);

        // Sun.
        g.setColor(new Color(0xF2D06B));
        g.fillOval(w - 46, 14, 30, 30);

        // Ship position.
        int cx = w / 2;
        int hullY = seaY - 6;

        // Hull.
        g.setColor(new Color(0x5A3A1E));
        int[] hx = {cx - 44, cx + 44, cx + 30, cx - 30};
        int[] hy = {hullY, hullY, hullY + 22, hullY + 22};
        g.fillPolygon(hx, hy, 4);

        // Masts and sails; darken with damage.
        int damage = (state == null) ? 0 : state.getShip().getDamage();
        int sailShade = (int) (230 - 1.4 * damage);
        sailShade = Math.max(60, Math.min(255, sailShade));
        Color sail = new Color(sailShade, sailShade, Math.max(40, sailShade - 30));

        g.setColor(new Color(0x3A2410));
        g.fillRect(cx - 22, hullY - 46, 3, 46);
        g.fillRect(cx + 20, hullY - 40, 3, 40);

        g.setColor(sail);
        int[] s1x = {cx - 20, cx - 20, cx + 2};
        int[] s1y = {hullY - 46, hullY - 4, hullY - 4};
        g.fillPolygon(s1x, s1y, 3);
        int[] s2x = {cx + 22, cx + 22, cx + 4};
        int[] s2y = {hullY - 40, hullY - 4, hullY - 4};
        g.fillPolygon(s2x, s2y, 3);

        // Gun ports as little dots along the hull.
        int guns = (state == null) ? 0 : state.getShip().getGuns();
        g.setColor(Color.BLACK);
        for (int i = 0; i < Math.min(guns, 8); i++) {
            g.fillOval(cx - 34 + i * 9, hullY + 8, 4, 4);
        }

        g.dispose();
    }
}
