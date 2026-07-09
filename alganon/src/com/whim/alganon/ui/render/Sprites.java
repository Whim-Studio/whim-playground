package com.whim.alganon.ui.render;

import com.whim.alganon.ui.UiTheme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;

/**
 * Procedural sprite factory. Everything is drawn from java.awt primitives keyed by a string
 * {@code spriteKey} (from the views) — no image files exist. Keys are matched by prefix so
 * the engine can append variants (e.g. {@code "mob.wolf.alpha"}) and still get a wolf body.
 */
public final class Sprites {
    private Sprites() {}

    /**
     * Draw an entity sprite centered in the cell (cx,cy) with radius r. The kind is inferred
     * from the key prefix; a stable per-key hue keeps unknown keys visually distinct.
     */
    public static void draw(Graphics2D g, String spriteKey, int cx, int cy, int r) {
        String key = spriteKey == null ? "" : spriteKey.toLowerCase();
        if (key.startsWith("player")) { drawHumanoid(g, cx, cy, r, UiTheme.ACCENT, new Color(0x2A, 0x40, 0x66)); return; }
        if (key.startsWith("npc"))    { drawHumanoid(g, cx, cy, r, new Color(0xC8, 0xC0, 0x9A), new Color(0x4A, 0x44, 0x36)); markNpc(g, key, cx, cy, r); return; }
        if (key.startsWith("portal")) { drawPortal(g, cx, cy, r); return; }
        if (key.startsWith("node") || key.startsWith("gather")) { drawNode(g, key, cx, cy, r); return; }
        if (key.startsWith("mob")) {
            if (key.contains("beast") || key.contains("wolf") || key.contains("boar")) { drawBeast(g, cx, cy, r, hueFor(key)); return; }
            if (key.contains("undead") || key.contains("skeleton") || key.contains("shade")) { drawUndead(g, cx, cy, r); return; }
            drawBrute(g, cx, cy, r, hueFor(key));
            return;
        }
        // Fallback: a hued lozenge so unrecognized keys are still visible.
        g.setColor(hueFor(key));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(UiTheme.PANEL_DARK);
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
    }

    private static void drawHumanoid(Graphics2D g, int cx, int cy, int r, Color cloak, Color body) {
        int h = r; // half-ish
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(cx - r, cy + h - 3, r * 2, 6); // shadow
        g.setColor(cloak);
        Polygon p = new Polygon();
        p.addPoint(cx, cy - h);
        p.addPoint(cx - r, cy + h);
        p.addPoint(cx + r, cy + h);
        g.fillPolygon(p);
        g.setColor(body);
        g.fillOval(cx - r / 2, cy - h - r / 2, r, r); // head
        g.setColor(UiTheme.PANEL_DARK);
        g.drawOval(cx - r / 2, cy - h - r / 2, r, r);
    }

    private static void markNpc(Graphics2D g, String key, int cx, int cy, int r) {
        if (key.contains("giver") || key.contains("quest")) {
            g.setColor(UiTheme.ACCENT_HOT);
            g.setFont(g.getFont().deriveFont((float) (r * 1.6f)));
            g.drawString("!", cx - r / 4, cy - r - r / 2);
        }
    }

    private static void drawBeast(Graphics2D g, int cx, int cy, int r, Color c) {
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(cx - r, cy + r - 3, r * 2, 6);
        g.setColor(c);
        g.fillOval(cx - r, cy - r / 2, r * 2, r); // body
        g.fillOval(cx + r / 2, cy - r, r, r);       // head
        int[] ex = {cx + r + r / 3, cx + r, cx + r + r / 2};
        int[] ey = {cy - r - r / 3, cy - r / 2, cy - r / 2};
        g.fillPolygon(ex, ey, 3);                    // ear
        g.setColor(UiTheme.BAD);
        g.fillOval(cx + r, cy - r + r / 4, 3, 3);    // eye
    }

    private static void drawBrute(Graphics2D g, int cx, int cy, int r, Color c) {
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(cx - r, cy + r - 3, r * 2, 6);
        g.setColor(c);
        g.fillRoundRect(cx - r, cy - r, r * 2, r * 2, r, r);
        g.setColor(c.darker());
        g.fillRect(cx - r, cy - r / 3, r * 2, r / 3);
        g.setColor(UiTheme.BAD);
        g.fillOval(cx - r / 2, cy - r / 2, 4, 4);
        g.fillOval(cx + r / 3, cy - r / 2, 4, 4);
    }

    private static void drawUndead(Graphics2D g, int cx, int cy, int r) {
        g.setColor(new Color(0xCF, 0xD6, 0xC8));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(new Color(0x20, 0x28, 0x20));
        g.fillOval(cx - r / 2, cy - r / 3, r / 3, r / 2);
        g.fillOval(cx + r / 6, cy - r / 3, r / 3, r / 2);
        g.drawLine(cx - r / 2, cy + r / 2, cx + r / 2, cy + r / 2);
    }

    private static void drawNode(Graphics2D g, String key, int cx, int cy, int r) {
        Color c;
        if (key.contains("ore") || key.contains("vein") || key.contains("metal")) c = new Color(0x8A, 0x8F, 0x9A);
        else if (key.contains("herb") || key.contains("plant")) c = new Color(0x5A, 0x9A, 0x4A);
        else if (key.contains("wood") || key.contains("tree")) c = new Color(0x6A, 0x4A, 0x2A);
        else c = new Color(0x7A, 0x8A, 0x6A);
        g.setColor(c);
        Polygon p = new Polygon();
        p.addPoint(cx, cy - r);
        p.addPoint(cx + r, cy);
        p.addPoint(cx, cy + r);
        p.addPoint(cx - r, cy);
        g.fillPolygon(p);
        g.setColor(c.brighter());
        g.drawLine(cx, cy - r, cx - r, cy);
    }

    private static void drawPortal(Graphics2D g, int cx, int cy, int r) {
        for (int i = 3; i >= 0; i--) {
            int rr = r - i * (r / 5);
            g.setColor(UiTheme.mix(new Color(0x30, 0x1A, 0x50), UiTheme.mix(UiTheme.XP, Color.WHITE, 0.2), i / 3.0));
            g.fillOval(cx - rr, cy - rr, rr * 2, rr * 2);
        }
    }

    /** Stable pseudo-color from a key so distinct mobs read differently without config. */
    public static Color hueFor(String key) {
        int h = key == null ? 0 : key.hashCode();
        float hue = ((h & 0xFFFF) / 65535f);
        return Color.getHSBColor(hue, 0.45f, 0.55f);
    }
}
