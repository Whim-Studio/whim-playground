package com.whim.cardwoven.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

import com.whim.cardwoven.api.Enums.AttachmentType;
import com.whim.cardwoven.api.Enums.BuildingType;

/**
 * Stateless procedural-graphics helpers. Every building, attachment glyph and
 * raider marker is drawn as {@link Graphics2D} shapes — no images anywhere.
 * Shared by {@link MapPanel} and {@link HandPanel} so a Temple looks the same
 * on the board and on the card that places it.
 */
public final class Renderer {
    private Renderer() {}

    public static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /**
     * Draw a building icon centered in the box (cx,cy) with radius r. Each
     * BuildingType has a distinct silhouette.
     */
    public static void building(Graphics2D g, BuildingType type, double cx, double cy, double r) {
        Color fill = UiColors.building(type);
        Color edge = UiColors.mix(fill, Color.BLACK, 0.45);
        g.setStroke(new BasicStroke((float) Math.max(1.2, r * 0.10)));
        if (type == BuildingType.CITY) {
            // Three battlemented towers.
            double w = r * 1.7, h = r * 1.5;
            double x = cx - w / 2, y = cy - h / 2 + r * 0.15;
            g.setColor(fill);
            g.fillRect((int) x, (int) y, (int) w, (int) h);
            g.setColor(edge);
            g.drawRect((int) x, (int) y, (int) w, (int) h);
            // crenellations
            double bw = w / 5.0;
            for (int i = 0; i < 5; i += 2) {
                g.setColor(fill);
                g.fillRect((int) (x + i * bw), (int) (y - r * 0.35), (int) bw, (int) (r * 0.4));
                g.setColor(edge);
                g.drawRect((int) (x + i * bw), (int) (y - r * 0.35), (int) bw, (int) (r * 0.4));
            }
            g.setColor(edge);
            g.drawLine((int) (cx - w / 2), (int) (cy + r * 0.2), (int) (cx + w / 2), (int) (cy + r * 0.2));
        } else if (type == BuildingType.FARM) {
            // House with pitched roof + field furrows.
            double w = r * 1.7, h = r * 1.1;
            double x = cx - w / 2, y = cy - h / 2 + r * 0.3;
            g.setColor(fill);
            g.fillRect((int) x, (int) y, (int) w, (int) h);
            Polygon roof = new Polygon();
            roof.addPoint((int) (x - r * 0.15), (int) y);
            roof.addPoint((int) (cx), (int) (y - r * 0.7));
            roof.addPoint((int) (x + w + r * 0.15), (int) y);
            g.setColor(UiColors.mix(fill, Color.BLACK, 0.25));
            g.fillPolygon(roof);
            g.setColor(edge);
            g.drawPolygon(roof);
            g.drawRect((int) x, (int) y, (int) w, (int) h);
        } else if (type == BuildingType.TEMPLE) {
            // Columned portico with pediment.
            double w = r * 1.8, h = r * 1.1;
            double x = cx - w / 2, y = cy - h / 2 + r * 0.35;
            Polygon ped = new Polygon();
            ped.addPoint((int) (x - r * 0.1), (int) y);
            ped.addPoint((int) cx, (int) (y - r * 0.75));
            ped.addPoint((int) (x + w + r * 0.1), (int) y);
            g.setColor(fill);
            g.fillPolygon(ped);
            g.setColor(edge);
            g.drawPolygon(ped);
            for (int i = 0; i <= 3; i++) {
                double px = x + i * (w / 3.0);
                g.setColor(fill);
                g.fillRect((int) (px - r * 0.09), (int) y, (int) (r * 0.18), (int) h);
                g.setColor(edge);
                g.drawRect((int) (px - r * 0.09), (int) y, (int) (r * 0.18), (int) h);
            }
        } else if (type == BuildingType.PORT) {
            // Dock + sailboat.
            g.setColor(edge);
            g.fillRect((int) (cx - r), (int) (cy + r * 0.5), (int) (r * 2), (int) (r * 0.3));
            Polygon hull = new Polygon();
            hull.addPoint((int) (cx - r * 0.8), (int) (cy + r * 0.4));
            hull.addPoint((int) (cx + r * 0.8), (int) (cy + r * 0.4));
            hull.addPoint((int) (cx + r * 0.5), (int) (cy + r * 0.9));
            hull.addPoint((int) (cx - r * 0.5), (int) (cy + r * 0.9));
            g.setColor(fill);
            g.fillPolygon(hull);
            g.setColor(edge);
            g.drawPolygon(hull);
            Polygon sail = new Polygon();
            sail.addPoint((int) cx, (int) (cy - r * 0.9));
            sail.addPoint((int) cx, (int) (cy + r * 0.3));
            sail.addPoint((int) (cx + r * 0.7), (int) (cy + r * 0.3));
            g.setColor(UiColors.mix(fill, Color.WHITE, 0.4));
            g.fillPolygon(sail);
            g.setColor(edge);
            g.drawLine((int) cx, (int) (cy - r * 0.9), (int) cx, (int) (cy + r * 0.4));
        }
    }

    /**
     * Small attachment glyph. WORKER = pick/diamond, IDOL = radiant circle,
     * WITCH = pointed hat/star.
     */
    public static void attachment(Graphics2D g, AttachmentType type, double cx, double cy, double r) {
        Color fill = UiColors.attachment(type);
        Color edge = UiColors.mix(fill, Color.BLACK, 0.5);
        g.setStroke(new BasicStroke((float) Math.max(1.0, r * 0.18)));
        if (type == AttachmentType.WORKER) {
            Polygon d = new Polygon();
            d.addPoint((int) cx, (int) (cy - r));
            d.addPoint((int) (cx + r), (int) cy);
            d.addPoint((int) cx, (int) (cy + r));
            d.addPoint((int) (cx - r), (int) cy);
            g.setColor(fill);
            g.fillPolygon(d);
            g.setColor(edge);
            g.drawPolygon(d);
        } else if (type == AttachmentType.IDOL) {
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * i / 4.0;
                g.setColor(edge);
                g.drawLine((int) cx, (int) cy,
                        (int) (cx + Math.cos(a) * r * 1.4), (int) (cy + Math.sin(a) * r * 1.4));
            }
            g.setColor(fill);
            g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            g.setColor(edge);
            g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        } else { // WITCH
            Polygon star = star(cx, cy, r * 1.3, r * 0.55, 5, -Math.PI / 2);
            g.setColor(fill);
            g.fillPolygon(star);
            g.setColor(edge);
            g.drawPolygon(star);
        }
    }

    /** Crossed-swords raider marker. */
    public static void raider(Graphics2D g, double cx, double cy, double r, int strength) {
        g.setStroke(new BasicStroke((float) Math.max(1.4, r * 0.18)));
        g.setColor(UiColors.RAIDER);
        g.drawLine((int) (cx - r), (int) (cy - r), (int) (cx + r), (int) (cy + r));
        g.drawLine((int) (cx + r), (int) (cy - r), (int) (cx - r), (int) (cy + r));
        g.setColor(UiColors.mix(UiColors.RAIDER, Color.BLACK, 0.4));
        g.drawOval((int) (cx - r * 1.3), (int) (cy - r * 1.3), (int) (r * 2.6), (int) (r * 2.6));
        if (strength > 0) {
            g.setFont(g.getFont().deriveFont(Font.BOLD, (float) Math.max(9, r * 1.1)));
            String s = Integer.toString(strength);
            int w = g.getFontMetrics().stringWidth(s);
            g.setColor(Color.WHITE);
            g.drawString(s, (int) (cx - w / 2.0), (int) (cy + r * 2.4));
        }
    }

    public static Polygon star(double cx, double cy, double outer, double inner,
                               int points, double rot) {
        Polygon p = new Polygon();
        for (int i = 0; i < points * 2; i++) {
            double rad = (i % 2 == 0) ? outer : inner;
            double a = rot + Math.PI * i / points;
            p.addPoint((int) Math.round(cx + Math.cos(a) * rad),
                       (int) Math.round(cy + Math.sin(a) * rad));
        }
        return p;
    }

    /** Draw a rounded progress bar with label; value 0..1. */
    public static void progressBar(Graphics2D g, int x, int y, int w, int h,
                                   double value, Color fill, String label, Color textColor) {
        double v = Math.max(0.0, Math.min(1.0, value));
        int arc = h;
        g.setColor(UiColors.PANEL_BG_2);
        g.fillRoundRect(x, y, w, h, arc, arc);
        if (v > 0) {
            int fw = (int) Math.round((w - 2) * v);
            g.setColor(fill);
            g.fillRoundRect(x + 1, y + 1, Math.max(h - 2, fw), h - 2, arc, arc);
        }
        g.setColor(UiColors.withAlpha(Color.BLACK, 60));
        g.drawRoundRect(x, y, w, h, arc, arc);
        if (label != null) {
            g.setColor(textColor);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 11f));
            g.drawString(label, x + 6, y + h - (h - 11) / 2 - 1);
        }
    }
}
