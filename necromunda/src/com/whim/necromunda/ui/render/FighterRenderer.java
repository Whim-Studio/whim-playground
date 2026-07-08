package com.whim.necromunda.ui.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Ellipse2D;

import com.whim.necromunda.model.Fighter;
import com.whim.necromunda.model.FighterStatus;
import com.whim.necromunda.model.House;

/**
 * Draws a fighter token as a Java2D geometric placeholder. Shape encodes role,
 * fill encodes House colour, a name initial is overlaid, an elevation badge is
 * shown for raised fighters, and a yellow ring marks the current selection.
 *
 * <ul>
 *   <li>Leader   → star</li>
 *   <li>Champion → square</li>
 *   <li>Ganger   → circle</li>
 *   <li>Juve     → triangle</li>
 * </ul>
 */
public final class FighterRenderer {

    private static final Color SELECT_RING = new Color(0xFF, 0xE0, 0x40);
    private static final Color BADGE_BG     = new Color(0x00, 0x00, 0x00, 0xB0);

    private FighterRenderer() {
    }

    public static void paint(Graphics2D g, Fighter f, House house,
                             int px, int py, int size, boolean selected) {
        int pad = Math.max(2, size / 6);
        int x = px + pad;
        int y = py + pad;
        int d = size - 2 * pad;
        int cx = px + size / 2;
        int cy = py + size / 2;

        Color fill = house.color();
        if (!f.status().inPlay()) {
            fill = fill.darker().darker();
        } else if (f.status() == FighterStatus.DOWN
                || f.status() == FighterStatus.PINNED) {
            fill = fill.darker();
        }

        if (selected) {
            g.setColor(SELECT_RING);
            g.fillOval(px + 1, py + 1, size - 2, size - 2);
        }

        g.setColor(fill);
        switch (f.type()) {
            case LEADER:
                fillStar(g, cx, cy, d / 2, d / 4);
                break;
            case CHAMPION:
                g.fillRect(x, y, d, d);
                break;
            case GANGER:
                g.fill(new Ellipse2D.Double(x, y, d, d));
                break;
            case JUVE:
                fillTriangle(g, cx, y, d);
                break;
            default:
                g.fill(new Ellipse2D.Double(x, y, d, d));
        }

        // Name initial.
        String initial = f.name().isEmpty() ? "?" : f.name().substring(0, 1).toUpperCase();
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, size / 2)));
        int tw = g.getFontMetrics().stringWidth(initial);
        int th = g.getFontMetrics().getAscent();
        g.drawString(initial, cx - tw / 2, cy + th / 2 - 1);

        // Elevation badge.
        // (position z is provided by the caller through the board, but the
        // fighter itself does not know it; the caller draws the badge — see below)
    }

    /** Draw a small "^z" elevation badge in the token's corner. */
    public static void paintElevationBadge(Graphics2D g, int px, int py, int size, int level) {
        if (level <= 0) {
            return;
        }
        String txt = "^" + level;
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, size / 3)));
        int tw = g.getFontMetrics().stringWidth(txt);
        int bw = tw + 4;
        int bh = g.getFontMetrics().getHeight();
        g.setColor(BADGE_BG);
        g.fillRoundRect(px + size - bw - 1, py + 1, bw, bh, 4, 4);
        g.setColor(new Color(0xFF, 0xE0, 0x40));
        g.drawString(txt, px + size - bw + 1, py + 1 + g.getFontMetrics().getAscent());
    }

    private static void fillStar(Graphics2D g, int cx, int cy, int outer, int inner) {
        Polygon star = new Polygon();
        for (int i = 0; i < 10; i++) {
            double r = (i % 2 == 0) ? outer : inner;
            double a = Math.PI / 2 + i * Math.PI / 5;
            star.addPoint((int) Math.round(cx + r * Math.cos(a)),
                          (int) Math.round(cy - r * Math.sin(a)));
        }
        g.fillPolygon(star);
    }

    private static void fillTriangle(Graphics2D g, int cx, int top, int d) {
        Polygon tri = new Polygon();
        tri.addPoint(cx, top);
        tri.addPoint(cx - d / 2, top + d);
        tri.addPoint(cx + d / 2, top + d);
        g.fillPolygon(tri);
    }
}
