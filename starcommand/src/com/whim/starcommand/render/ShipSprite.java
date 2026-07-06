package com.whim.starcommand.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;

/** Procedurally-drawn ship silhouettes — original vector art, no ripped sprites. */
public final class ShipSprite {
    private ShipSprite() { }

    /** Draw a ship pointing right, centred at (cx,cy), scaled to size px. */
    public static void draw(Graphics2D g, int cx, int cy, int size, Color body, boolean disabled) {
        Polygon hull = new Polygon();
        hull.addPoint(cx + size, cy);
        hull.addPoint(cx - size / 2, cy - size / 2);
        hull.addPoint(cx - size / 3, cy);
        hull.addPoint(cx - size / 2, cy + size / 2);
        g.setColor(disabled ? body.darker().darker() : body);
        g.fillPolygon(hull);
        g.setColor(Palette.PANEL_LINE);
        g.setStroke(new BasicStroke(1.5f));
        g.drawPolygon(hull);
        // engine glow
        if (!disabled) {
            g.setColor(Palette.ACCENT);
            g.fillOval(cx - size / 2 - 4, cy - 3, 6, 6);
        } else {
            g.setColor(Palette.DANGER);
            g.drawLine(cx - size / 3, cy - size / 3, cx + size / 3, cy + size / 3);
        }
    }
}
