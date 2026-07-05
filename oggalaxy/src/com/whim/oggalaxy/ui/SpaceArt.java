package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.Ids;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.util.Random;

/**
 * All procedural Java2D artwork: starfield backgrounds, seeded procedural planets,
 * distinct vector glyphs per ship / defense type, and small resource icons. NOTHING
 * here loads an image — every pixel is drawn from shapes and gradients so the build
 * stays zero-asset. All methods are static and stateless (deterministic given a seed).
 */
public final class SpaceArt {

    private SpaceArt() {
    }

    public static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    // ------------------------------------------------------------------
    // Starfield
    // ------------------------------------------------------------------

    /** Fills the rectangle with a deep space gradient and a seeded scatter of stars. */
    public static void starfield(Graphics2D g, int w, int h, long seed) {
        hints(g);
        java.awt.Paint old = g.getPaint();
        RadialGradientPaint bg = new RadialGradientPaint(
                new Point(w / 3, h / 3), Math.max(w, h),
                new float[]{0f, 0.6f, 1f},
                new Color[]{Palette.BG_SPACE, Palette.BG_DEEP, new Color(4, 5, 12)});
        g.setPaint(bg);
        g.fillRect(0, 0, w, h);
        g.setPaint(old);

        Random rnd = new Random(seed * 0x9E3779B97F4A7C15L + 1);
        int count = Math.max(40, (w * h) / 2600);
        for (int i = 0; i < count; i++) {
            int x = rnd.nextInt(Math.max(1, w));
            int y = rnd.nextInt(Math.max(1, h));
            float b = 0.25f + rnd.nextFloat() * 0.75f;
            int size = rnd.nextInt(100) < 88 ? 1 : 2;
            int v = (int) (b * 255);
            Color star = new Color(v, v, Math.min(255, v + 20), (int) (b * 220));
            g.setColor(star);
            g.fillRect(x, y, size, size);
            if (size == 2 && rnd.nextInt(6) == 0) {
                g.setColor(Palette.alpha(Palette.ACCENT, 90));
                g.drawLine(x - 2, y, x + 3, y);
                g.drawLine(x, y - 2, x, y + 3);
            }
        }
        // a faint nebula smear
        Random n = new Random(seed * 31 + 7);
        for (int i = 0; i < 3; i++) {
            int cx = n.nextInt(Math.max(1, w));
            int cy = n.nextInt(Math.max(1, h));
            int r = 60 + n.nextInt(120);
            Color tint = i % 2 == 0 ? Palette.ACCENT2 : Palette.ACCENT;
            RadialGradientPaint neb = new RadialGradientPaint(
                    new Point(cx, cy), r, new float[]{0f, 1f},
                    new Color[]{Palette.alpha(tint, 26), Palette.alpha(tint, 0)});
            g.setPaint(neb);
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
        }
        g.setPaint(old);
    }

    // ------------------------------------------------------------------
    // Procedural planet (seeded by coordinates)
    // ------------------------------------------------------------------

    /**
     * Draws a procedural planet centred at (cx,cy). The seed (typically a hash of the
     * planet's coordinates) fully determines its palette, banding and lighting so the
     * same world always looks the same.
     */
    public static void planet(Graphics2D g, int cx, int cy, int radius, long seed) {
        hints(g);
        Random rnd = new Random(seed);
        java.awt.Paint old = g.getPaint();
        Shape oldClip = g.getClip();

        // base hue picked from seed
        float hue = rnd.nextFloat();
        Color base = Color.getHSBColor(hue, 0.45f + rnd.nextFloat() * 0.3f, 0.55f + rnd.nextFloat() * 0.25f);
        Color dark = Palette.mix(base, Color.BLACK, 0.55);
        Color light = Palette.mix(base, Color.WHITE, 0.35);

        // atmosphere glow
        Color atmo = Color.getHSBColor((hue + 0.5f) % 1f, 0.5f, 0.9f);
        RadialGradientPaint glow = new RadialGradientPaint(
                new Point(cx, cy), (int) (radius * 1.35f), new float[]{0.72f, 0.86f, 1f},
                new Color[]{Palette.alpha(atmo, 0), Palette.alpha(atmo, 70), Palette.alpha(atmo, 0)});
        g.setPaint(glow);
        g.fillOval(cx - (int) (radius * 1.35f), cy - (int) (radius * 1.35f),
                (int) (radius * 2.7f), (int) (radius * 2.7f));

        Ellipse2D disc = new Ellipse2D.Double(cx - radius, cy - radius, radius * 2.0, radius * 2.0);
        g.setClip(disc);

        // sphere base shading (light from upper-left)
        RadialGradientPaint sphere = new RadialGradientPaint(
                new Point(cx - radius / 3, cy - radius / 3), (int) (radius * 1.6f),
                new float[]{0f, 0.6f, 1f},
                new Color[]{light, base, dark});
        g.setPaint(sphere);
        g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // latitude bands / continents
        int bands = 4 + rnd.nextInt(6);
        for (int i = 0; i < bands; i++) {
            float t = rnd.nextFloat();
            Color c = Palette.mix(dark, light, t);
            g.setColor(Palette.alpha(c, 70 + rnd.nextInt(80)));
            int by = cy - radius + (int) (rnd.nextFloat() * radius * 2);
            int bh = 4 + rnd.nextInt(radius / 2 + 4);
            g.fillOval(cx - radius - 6, by, radius * 2 + 12, bh);
        }
        // speckle craters / storms
        int spots = 6 + rnd.nextInt(10);
        for (int i = 0; i < spots; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double rr = rnd.nextDouble() * radius * 0.8;
            int sx = cx + (int) (Math.cos(ang) * rr);
            int sy = cy + (int) (Math.sin(ang) * rr);
            int sz = 2 + rnd.nextInt(Math.max(3, radius / 6));
            g.setColor(Palette.alpha(rnd.nextBoolean() ? dark : light, 60 + rnd.nextInt(70)));
            g.fillOval(sx - sz / 2, sy - sz / 2, sz, sz);
        }

        // terminator shadow (night side, lower-right)
        RadialGradientPaint night = new RadialGradientPaint(
                new Point(cx + radius / 2, cy + radius / 2), (int) (radius * 1.8f),
                new float[]{0f, 1f},
                new Color[]{Palette.alpha(Color.BLACK, 210), Palette.alpha(Color.BLACK, 0)});
        g.setPaint(night);
        g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

        g.setClip(oldClip);

        // rim highlight
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(Palette.alpha(light, 120));
        g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // occasional ring
        if (rnd.nextInt(4) == 0) {
            g.setStroke(new BasicStroke(2f));
            g.setColor(Palette.alpha(Palette.mix(base, Color.WHITE, 0.5), 120));
            int rw = (int) (radius * 2.6f);
            int rh = (int) (radius * 0.7f);
            g.drawOval(cx - rw / 2, cy - rh / 2, rw, rh);
        }
        g.setPaint(old);
    }

    /** Draw a small moon disc. */
    public static void moon(Graphics2D g, int cx, int cy, int radius, long seed) {
        hints(g);
        Random rnd = new Random(seed ^ 0x5DEECE66DL);
        Shape oldClip = g.getClip();
        Ellipse2D disc = new Ellipse2D.Double(cx - radius, cy - radius, radius * 2.0, radius * 2.0);
        g.setClip(disc);
        RadialGradientPaint sphere = new RadialGradientPaint(
                new Point(cx - radius / 3, cy - radius / 3), Math.max(1, (int) (radius * 1.6f)),
                new float[]{0f, 1f},
                new Color[]{new Color(200, 205, 215), new Color(70, 74, 84)});
        g.setPaint(sphere);
        g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        for (int i = 0; i < 5 + rnd.nextInt(6); i++) {
            int sx = cx - radius + rnd.nextInt(radius * 2);
            int sy = cy - radius + rnd.nextInt(radius * 2);
            int sz = 1 + rnd.nextInt(Math.max(2, radius / 4));
            g.setColor(new Color(50, 52, 60, 120));
            g.fillOval(sx, sy, sz, sz);
        }
        g.setClip(oldClip);
        g.setColor(new Color(150, 155, 165, 120));
        g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
    }

    // ------------------------------------------------------------------
    // Ship / defense glyphs
    // ------------------------------------------------------------------

    /** Draw a distinct vector glyph for a ship type, fitting a size×size box at (x,y). */
    public static void shipGlyph(Graphics2D g, Ids.ShipType type, int x, int y, int size, Color c) {
        hints(g);
        java.awt.Stroke os = g.getStroke();
        g.setStroke(new BasicStroke(Math.max(1f, size / 12f)));
        double s = size;
        double cx = x + s / 2, cy = y + s / 2;
        g.setColor(c);
        switch (type) {
            case SMALL_CARGO:
                fillRoundBox(g, x + size / 4, y + size / 3, size / 2, size / 3, c);
                break;
            case LARGE_CARGO:
                fillRoundBox(g, x + size / 6, y + size / 4, size * 2 / 3, size / 2, c);
                g.setColor(Palette.mix(c, Color.BLACK, 0.4));
                g.drawLine(x + size / 6, (int) cy, x + size / 6 + size * 2 / 3, (int) cy);
                break;
            case LIGHT_FIGHTER:
                tri(g, cx, y + size * 0.2, x + size * 0.25, y + size * 0.8, x + size * 0.75, y + size * 0.8, c, true);
                break;
            case HEAVY_FIGHTER:
                tri(g, cx, y + size * 0.15, x + size * 0.2, y + size * 0.85, x + size * 0.8, y + size * 0.85, c, true);
                g.setColor(Palette.mix(c, Color.WHITE, 0.5));
                g.drawLine((int) cx, y + size / 3, (int) cx, y + size * 3 / 4);
                break;
            case CRUISER:
                diamond(g, cx, cy, s * 0.4, s * 0.28, c, true);
                break;
            case BATTLESHIP:
                fillRoundBox(g, x + size / 6, (int) (y + size * 0.35), size * 2 / 3, size / 3, c);
                tri(g, x + size * 0.83, cy, x + size * 0.62, y + size * 0.3, x + size * 0.62, y + size * 0.7, c, true);
                break;
            case BATTLECRUISER:
                diamond(g, cx, cy, s * 0.46, s * 0.22, c, true);
                g.setColor(Palette.mix(c, Color.WHITE, 0.6));
                g.drawLine(x + 2, (int) cy, x + size - 2, (int) cy);
                break;
            case BOMBER:
                fillRoundBox(g, x + size / 5, y + size / 3, size * 3 / 5, size / 3, c);
                g.setColor(c);
                g.fillOval((int) (cx - s * 0.12), (int) (y + size * 0.62), (int) (s * 0.24), (int) (s * 0.24));
                break;
            case DESTROYER:
                hexagon(g, cx, cy, s * 0.42, c, true);
                break;
            case REAPER:
                // scythe: arc + blade
                g.setColor(c);
                g.fillOval((int) (cx - s * 0.35), (int) (cy - s * 0.35), (int) (s * 0.7), (int) (s * 0.7));
                g.setColor(Palette.mix(c, Color.BLACK, 0.5));
                g.drawArc((int) (cx - s * 0.28), (int) (cy - s * 0.28), (int) (s * 0.56), (int) (s * 0.56), 30, 200);
                break;
            case PATHFINDER:
                tri(g, cx, y + size * 0.18, x + size * 0.22, y + size * 0.82, x + size * 0.78, y + size * 0.82, c, false);
                g.setColor(c);
                g.drawOval((int) (cx - s * 0.14), (int) (cy - s * 0.14), (int) (s * 0.28), (int) (s * 0.28));
                break;
            case LEVIATHAN:
                star(g, cx, cy, s * 0.46, s * 0.2, 6, c);
                break;
            case DEATHSTAR:
                g.setColor(c);
                g.fillOval((int) (cx - s * 0.42), (int) (cy - s * 0.42), (int) (s * 0.84), (int) (s * 0.84));
                g.setColor(Palette.mix(c, Color.BLACK, 0.55));
                g.fillOval((int) (cx - s * 0.16), (int) (cy - s * 0.22), (int) (s * 0.2), (int) (s * 0.16));
                g.drawLine((int) (cx - s * 0.42), (int) cy, (int) (cx + s * 0.42), (int) cy);
                break;
            case RECYCLER:
                fillRoundBox(g, x + size / 5, y + size / 3, size * 3 / 5, size / 3, c);
                g.setColor(Palette.mix(c, Color.WHITE, 0.5));
                g.drawArc(x + size / 4, y + size / 5, size / 2, size / 2, 0, 300);
                break;
            case ESPIONAGE_PROBE:
                g.setColor(c);
                g.fillOval((int) (cx - s * 0.16), (int) (cy - s * 0.16), (int) (s * 0.32), (int) (s * 0.32));
                g.drawLine((int) cx, (int) (cy - s * 0.16), (int) cx, (int) (y + size * 0.1));
                g.drawLine((int) cx, (int) (cy + s * 0.16), (int) cx, (int) (y + size * 0.9));
                break;
            case SOLAR_SATELLITE:
                g.setColor(c);
                g.fillOval((int) (cx - s * 0.1), (int) (cy - s * 0.1), (int) (s * 0.2), (int) (s * 0.2));
                fillRoundBox(g, x + size / 8, (int) (cy - s * 0.08), size / 5, (int) (s * 0.16), c);
                fillRoundBox(g, (int) (cx + s * 0.22), (int) (cy - s * 0.08), size / 5, (int) (s * 0.16), c);
                break;
            case COLONY_SHIP:
                fillRoundBox(g, x + size / 4, y + size / 5, size / 2, size * 3 / 5, c);
                g.setColor(Palette.mix(c, Color.WHITE, 0.5));
                g.fillOval((int) (cx - s * 0.1), (int) (cy - s * 0.05), (int) (s * 0.2), (int) (s * 0.2));
                break;
            default:
                g.fillOval(x + size / 4, y + size / 4, size / 2, size / 2);
        }
        g.setStroke(os);
    }

    /** Draw a distinct vector glyph for a defense type. */
    public static void defenseGlyph(Graphics2D g, Ids.DefenseType type, int x, int y, int size, Color c) {
        hints(g);
        java.awt.Stroke os = g.getStroke();
        g.setStroke(new BasicStroke(Math.max(1f, size / 11f)));
        double s = size;
        double cx = x + s / 2, cy = y + s / 2;
        g.setColor(c);
        switch (type) {
            case ROCKET_LAUNCHER:
                fillRoundBox(g, (int) (cx - s * 0.12), y + size / 5, (int) (s * 0.24), size * 3 / 5, c);
                tri(g, cx, y + size * 0.1, cx - s * 0.12, y + size * 0.28, cx + s * 0.12, y + size * 0.28, c, true);
                break;
            case LIGHT_LASER:
                g.drawLine((int) cx, (int) (y + size * 0.15), (int) cx, (int) (y + size * 0.85));
                g.setColor(Palette.mix(c, Color.WHITE, 0.6));
                g.fillOval((int) (cx - s * 0.08), (int) (y + size * 0.1), (int) (s * 0.16), (int) (s * 0.16));
                break;
            case HEAVY_LASER:
                g.setStroke(new BasicStroke(Math.max(2f, size / 7f)));
                g.drawLine((int) cx, (int) (y + size * 0.12), (int) cx, (int) (y + size * 0.88));
                g.setColor(Palette.mix(c, Color.WHITE, 0.6));
                g.fillOval((int) (cx - s * 0.12), (int) (y + size * 0.08), (int) (s * 0.24), (int) (s * 0.24));
                break;
            case ION_CANNON:
                g.drawOval((int) (cx - s * 0.35), (int) (cy - s * 0.35), (int) (s * 0.7), (int) (s * 0.7));
                g.drawOval((int) (cx - s * 0.18), (int) (cy - s * 0.18), (int) (s * 0.36), (int) (s * 0.36));
                break;
            case GAUSS_CANNON:
                fillRoundBox(g, x + size / 5, (int) (cy - s * 0.1), size * 3 / 5, (int) (s * 0.2), c);
                g.setColor(c);
                g.drawLine((int) (cx + s * 0.3), (int) cy, (int) (x + size * 0.95), (int) cy);
                break;
            case PLASMA_TURRET:
                hexagon(g, cx, cy, s * 0.36, c, true);
                g.setColor(Palette.mix(c, Color.WHITE, 0.7));
                g.fillOval((int) (cx - s * 0.1), (int) (cy - s * 0.1), (int) (s * 0.2), (int) (s * 0.2));
                break;
            case SMALL_SHIELD_DOME:
                g.setColor(Palette.alpha(c, 90));
                g.fillArc((int) (cx - s * 0.4), (int) (cy - s * 0.25), (int) (s * 0.8), (int) (s * 0.8), 0, 180);
                g.setColor(c);
                g.drawArc((int) (cx - s * 0.4), (int) (cy - s * 0.25), (int) (s * 0.8), (int) (s * 0.8), 0, 180);
                break;
            case LARGE_SHIELD_DOME:
                g.setColor(Palette.alpha(c, 110));
                g.fillArc((int) (cx - s * 0.46), (int) (cy - s * 0.3), (int) (s * 0.92), (int) (s * 0.92), 0, 180);
                g.setColor(c);
                g.drawArc((int) (cx - s * 0.46), (int) (cy - s * 0.3), (int) (s * 0.92), (int) (s * 0.92), 0, 180);
                g.drawArc((int) (cx - s * 0.3), (int) (cy - s * 0.2), (int) (s * 0.6), (int) (s * 0.6), 0, 180);
                break;
            default:
                g.fillRect(x + size / 4, y + size / 4, size / 2, size / 2);
        }
        g.setStroke(os);
    }

    /** Small filled icon for a resource, in a size×size box. */
    public static void resourceIcon(Graphics2D g, Ids.ResourceType type, int x, int y, int size) {
        hints(g);
        Color c = Palette.resourceColor(type);
        double s = size;
        double cx = x + s / 2, cy = y + s / 2;
        g.setColor(c);
        switch (type) {
            case METAL:
                fillRoundBox(g, x + size / 6, y + size / 3, size * 2 / 3, size / 2, c);
                g.setColor(Palette.mix(c, Color.WHITE, 0.5));
                g.drawLine(x + size / 6, (int) cy, x + size / 6 + size * 2 / 3, (int) cy);
                break;
            case CRYSTAL:
                diamond(g, cx, cy, s * 0.32, s * 0.42, c, true);
                g.setColor(Palette.mix(c, Color.WHITE, 0.6));
                g.drawLine((int) cx, (int) (cy - s * 0.42), (int) cx, (int) (cy + s * 0.42));
                break;
            case DEUTERIUM:
                g.fillOval((int) (cx - s * 0.26), (int) (cy - s * 0.3), (int) (s * 0.52), (int) (s * 0.6));
                g.setColor(Palette.mix(c, Color.WHITE, 0.6));
                g.fillOval((int) (cx - s * 0.05), (int) (cy - s * 0.15), (int) (s * 0.14), (int) (s * 0.18));
                break;
            case ENERGY:
                bolt(g, x, y, size, c);
                break;
            case DARK_MATTER:
                star(g, cx, cy, s * 0.42, s * 0.16, 5, c);
                break;
            default:
                g.fillOval(x + size / 4, y + size / 4, size / 2, size / 2);
        }
    }

    // ------------------------------------------------------------------
    // primitives
    // ------------------------------------------------------------------

    private static void fillRoundBox(Graphics2D g, int x, int y, int w, int h, Color c) {
        g.setColor(c);
        g.fillRoundRect(x, y, w, h, Math.max(3, h / 3), Math.max(3, h / 3));
    }

    private static void tri(Graphics2D g, double ax, double ay, double bx, double by,
                            double dx, double dy, Color c, boolean fill) {
        GeneralPath p = new GeneralPath();
        p.moveTo(ax, ay);
        p.lineTo(bx, by);
        p.lineTo(dx, dy);
        p.closePath();
        g.setColor(c);
        if (fill) g.fill(p); else g.draw(p);
    }

    private static void diamond(Graphics2D g, double cx, double cy, double rx, double ry, Color c, boolean fill) {
        GeneralPath p = new GeneralPath();
        p.moveTo(cx, cy - ry);
        p.lineTo(cx + rx, cy);
        p.lineTo(cx, cy + ry);
        p.lineTo(cx - rx, cy);
        p.closePath();
        g.setColor(c);
        if (fill) g.fill(p); else g.draw(p);
    }

    private static void hexagon(Graphics2D g, double cx, double cy, double r, Color c, boolean fill) {
        GeneralPath p = new GeneralPath();
        for (int i = 0; i < 6; i++) {
            double a = Math.PI / 6 + i * Math.PI / 3;
            double px = cx + Math.cos(a) * r;
            double py = cy + Math.sin(a) * r;
            if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
        }
        p.closePath();
        g.setColor(c);
        if (fill) g.fill(p); else g.draw(p);
    }

    private static void star(Graphics2D g, double cx, double cy, double rOuter, double rInner, int points, Color c) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < points * 2; i++) {
            double r = (i % 2 == 0) ? rOuter : rInner;
            double a = -Math.PI / 2 + i * Math.PI / points;
            double px = cx + Math.cos(a) * r;
            double py = cy + Math.sin(a) * r;
            if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
        }
        p.closePath();
        g.setColor(c);
        g.fill(p);
    }

    private static void bolt(Graphics2D g, int x, int y, int size, Color c) {
        double s = size;
        GeneralPath p = new GeneralPath();
        p.moveTo(x + s * 0.58, y + s * 0.1);
        p.lineTo(x + s * 0.3, y + s * 0.55);
        p.lineTo(x + s * 0.5, y + s * 0.55);
        p.lineTo(x + s * 0.42, y + s * 0.9);
        p.lineTo(x + s * 0.72, y + s * 0.42);
        p.lineTo(x + s * 0.5, y + s * 0.42);
        p.closePath();
        g.setColor(c);
        g.fill(p);
    }

    /** A stable seed for a planet given its coordinates. */
    public static long coordSeed(int g, int s, int p) {
        return ((long) g * 73856093L) ^ ((long) s * 19349663L) ^ ((long) p * 83492791L);
    }
}
