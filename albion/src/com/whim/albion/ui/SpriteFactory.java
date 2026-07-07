package com.whim.albion.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;

import com.whim.albion.api.Enums.Direction;

/**
 * Procedural art helper. Maps string {@code spriteKey}/{@code decorKey}/{@code portraitKey}
 * values to deterministic Graphics2D drawing so the same key always renders the same
 * shape. All art is generated from shapes/gradients/colors — no image assets on disk.
 *
 * <p>Keys are free-form strings coming from the model/views; we hash unknown keys into a
 * stable color + glyph so nothing ever renders as a blank. A handful of well-known key
 * prefixes ("npc.", "player", "tree", "chest", "enemy.", etc.) get bespoke drawings.
 */
final class SpriteFactory {

    private SpriteFactory() {}

    /** Deterministic color derived from a key (stable across runs). */
    static Color colorFor(String key) {
        int h = stableHash(key);
        float hue = (h & 0xFFFF) / 65535f;
        float sat = 0.45f + ((h >> 16) & 0x3F) / 63f * 0.4f;
        float bri = 0.55f + ((h >> 22) & 0x3F) / 63f * 0.35f;
        return Color.getHSBColor(hue, sat, bri);
    }

    /** FNV-1a style stable hash (String.hashCode is stable too, but this spreads bits). */
    private static int stableHash(String s) {
        if (s == null) s = "";
        int h = 0x811c9dc5;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x01000193;
        }
        return h ^ (h >>> 16);
    }

    // ------------------------------------------------------------------ tiles/decor

    /** Draw a decoration keyed by {@code decorKey} centered in the given tile box. */
    static void drawDecor(Graphics2D g, String key, int x, int y, int w, int h) {
        if (key == null || key.isEmpty()) return;
        hint(g);
        String k = key.toLowerCase();
        if (k.contains("tree") || k.contains("forest")) {
            drawTree(g, x, y, w, h);
        } else if (k.contains("chest") || k.contains("treasure")) {
            drawChest(g, x, y, w, h);
        } else if (k.contains("rock") || k.contains("stone") || k.contains("boulder")) {
            drawRock(g, x, y, w, h);
        } else if (k.contains("stairs") || k.contains("entrance") || k.contains("dungeon")) {
            drawStairs(g, x, y, w, h);
        } else if (k.contains("door") || k.contains("gate")) {
            drawDoorDecor(g, x, y, w, h);
        } else if (k.contains("sign") || k.contains("post")) {
            drawSign(g, x, y, w, h);
        } else if (k.contains("flower") || k.contains("bush") || k.contains("plant")) {
            drawBush(g, x, y, w, h);
        } else {
            drawGeneric(g, key, x, y, w, h);
        }
    }

    private static void drawTree(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(90, 60, 35));
        int tw = Math.max(3, w / 6);
        g.fillRect(x + w / 2 - tw / 2, y + h / 2, tw, h / 2 - 2);
        g.setColor(new Color(34, 110, 45));
        g.fillOval(x + w / 6, y + h / 8, w * 2 / 3, h * 2 / 3);
        g.setColor(new Color(48, 140, 60));
        g.fillOval(x + w / 4, y + h / 10, w / 2, h / 2);
    }

    private static void drawBush(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(40, 120, 55));
        g.fillOval(x + w / 5, y + h / 3, w * 3 / 5, h / 2);
        g.setColor(new Color(220, 90, 120));
        g.fillOval(x + w / 2 - 2, y + h / 2 - 2, 4, 4);
    }

    private static void drawChest(Graphics2D g, int x, int y, int w, int h) {
        int cw = w * 3 / 5, ch = h * 2 / 5;
        int cx = x + (w - cw) / 2, cy = y + h / 2 - 2;
        g.setColor(new Color(120, 80, 40));
        g.fillRect(cx, cy, cw, ch);
        g.setColor(new Color(200, 170, 70));
        g.fillRect(cx, cy - ch / 3, cw, ch / 3);
        g.setColor(new Color(240, 220, 120));
        g.fillRect(cx + cw / 2 - 1, cy - 1, 3, ch / 2);
    }

    private static void drawRock(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(120, 120, 128));
        Polygon p = new Polygon();
        p.addPoint(x + w / 5, y + h * 3 / 4);
        p.addPoint(x + w / 3, y + h / 3);
        p.addPoint(x + w * 2 / 3, y + h / 4);
        p.addPoint(x + w * 4 / 5, y + h * 3 / 4);
        g.fillPolygon(p);
        g.setColor(new Color(90, 90, 98));
        g.drawPolygon(p);
    }

    private static void drawStairs(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(40, 40, 48));
        g.fillRect(x + w / 6, y + h / 6, w * 2 / 3, h * 2 / 3);
        g.setColor(new Color(80, 80, 92));
        for (int i = 0; i < 4; i++) {
            int sy = y + h / 6 + i * (h * 2 / 3) / 4;
            g.fillRect(x + w / 6, sy, w * 2 / 3 - i * (w / 12), 3);
        }
    }

    private static void drawDoorDecor(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(110, 75, 40));
        g.fillRect(x + w / 4, y + h / 6, w / 2, h * 3 / 4);
        g.setColor(new Color(230, 200, 80));
        g.fillOval(x + w * 3 / 5, y + h / 2, 4, 4);
    }

    private static void drawSign(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(90, 60, 35));
        g.fillRect(x + w / 2 - 2, y + h / 3, 4, h / 2);
        g.setColor(new Color(160, 120, 70));
        g.fillRect(x + w / 4, y + h / 4, w / 2, h / 4);
    }

    // ------------------------------------------------------------------ actors

    /** Draw an NPC / actor sprite keyed by {@code spriteKey}. */
    static void drawActor(Graphics2D g, String key, int x, int y, int w, int h, boolean hostile) {
        hint(g);
        Color body = hostile ? new Color(170, 60, 55) : colorFor(key == null ? "npc" : key);
        String k = key == null ? "" : key.toLowerCase();
        if (k.contains("enemy") || k.contains("monster") || k.contains("beast") || hostile) {
            drawMonster(g, key, x, y, w, h);
            return;
        }
        // humanoid: head + torso
        int cx = x + w / 2;
        g.setColor(new Color(232, 196, 160));
        int hr = Math.max(4, w / 5);
        g.fillOval(cx - hr, y + h / 8, hr * 2, hr * 2);
        g.setColor(body);
        Polygon torso = new Polygon();
        torso.addPoint(cx, y + h / 8 + hr);
        torso.addPoint(cx - w / 3, y + h * 5 / 6);
        torso.addPoint(cx + w / 3, y + h * 5 / 6);
        g.fillPolygon(torso);
    }

    private static void drawMonster(Graphics2D g, String key, int x, int y, int w, int h) {
        g.setColor(colorFor(key == null ? "enemy" : key));
        g.fillOval(x + w / 6, y + h / 4, w * 2 / 3, h * 2 / 3);
        // eyes
        g.setColor(Color.YELLOW);
        int ey = y + h / 2;
        g.fillOval(x + w * 2 / 5, ey, 4, 4);
        g.fillOval(x + w * 3 / 5 - 2, ey, 4, 4);
        // legs
        g.setColor(colorFor(key == null ? "enemy" : key).darker());
        g.fillRect(x + w / 3, y + h * 5 / 6, 3, h / 8);
        g.fillRect(x + w * 3 / 5, y + h * 5 / 6, 3, h / 8);
    }

    /** Draw the player with a facing indicator. */
    static void drawPlayer(Graphics2D g, int x, int y, int w, int h, Direction facing) {
        hint(g);
        int cx = x + w / 2, cy = y + h / 2;
        g.setColor(new Color(70, 120, 200));
        g.fillOval(x + w / 5, y + h / 5, w * 3 / 5, h * 3 / 5);
        g.setColor(new Color(232, 196, 160));
        int hr = Math.max(3, w / 6);
        g.fillOval(cx - hr, y + h / 6, hr * 2, hr * 2);
        // facing arrow
        g.setColor(Color.WHITE);
        Direction f = facing == null ? Direction.SOUTH : facing;
        int len = w / 3;
        int ax = cx + f.dx() * len, ay = cy + f.dy() * len;
        g.setStroke(new BasicStroke(2.5f));
        g.drawLine(cx, cy, ax, ay);
        g.fillOval(ax - 2, ay - 2, 5, 5);
    }

    // ------------------------------------------------------------------ portraits

    /** Draw a party/dialogue portrait keyed by {@code portraitKey} into the box. */
    static void drawPortrait(Graphics2D g, String key, int x, int y, int w, int h) {
        hint(g);
        Color base = colorFor(key == null ? "portrait" : key);
        // background panel
        g.setColor(base.darker().darker());
        g.fillRect(x, y, w, h);
        // face
        g.setColor(new Color(232, 200, 168));
        int fw = w * 3 / 5, fh = h * 3 / 5;
        int fx = x + (w - fw) / 2, fy = y + h / 6;
        g.fillOval(fx, fy, fw, fh);
        // hair / hood tint from key
        g.setColor(base);
        g.fillArc(fx, fy - fh / 6, fw, fh, 0, 180);
        // eyes
        g.setColor(Color.DARK_GRAY);
        g.fillOval(fx + fw / 4, fy + fh / 2, 3, 3);
        g.fillOval(fx + fw * 3 / 5, fy + fh / 2, 3, 3);
        // frame
        g.setColor(new Color(200, 180, 120));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(x, y, w - 1, h - 1);
    }

    /** Small item icon keyed by spriteKey/type. */
    static void drawItem(Graphics2D g, String key, int x, int y, int w, int h) {
        hint(g);
        String k = key == null ? "" : key.toLowerCase();
        if (k.contains("sword") || k.contains("weapon") || k.contains("blade")) {
            g.setColor(new Color(200, 200, 210));
            g.fillRect(x + w / 2 - 1, y + h / 6, 3, h * 3 / 5);
            g.setColor(new Color(150, 100, 50));
            g.fillRect(x + w / 3, y + h * 3 / 4, w / 3, 4);
        } else if (k.contains("potion") || k.contains("consum")) {
            g.setColor(new Color(200, 60, 90));
            g.fillOval(x + w / 4, y + h / 3, w / 2, h / 2);
            g.setColor(new Color(120, 90, 60));
            g.fillRect(x + w / 2 - 2, y + h / 5, 4, h / 5);
        } else if (k.contains("shield")) {
            g.setColor(new Color(120, 130, 160));
            Polygon p = new Polygon();
            p.addPoint(x + w / 2, y + h / 6);
            p.addPoint(x + w * 4 / 5, y + h / 3);
            p.addPoint(x + w / 2, y + h * 5 / 6);
            p.addPoint(x + w / 5, y + h / 3);
            g.fillPolygon(p);
        } else if (k.contains("key")) {
            g.setColor(new Color(220, 190, 80));
            g.fillOval(x + w / 4, y + h / 4, w / 3, h / 3);
            g.fillRect(x + w / 2, y + h / 3, w / 4, 3);
        } else if (k.contains("scroll")) {
            g.setColor(new Color(220, 210, 170));
            g.fillRect(x + w / 4, y + h / 4, w / 2, h / 2);
            g.setColor(new Color(150, 60, 60));
            g.drawLine(x + w / 3, y + h / 3, x + w * 2 / 3, y + h / 3);
        } else {
            drawGeneric(g, key, x, y, w, h);
        }
    }

    private static void drawGeneric(Graphics2D g, String key, int x, int y, int w, int h) {
        g.setColor(colorFor(key));
        g.fillRoundRect(x + w / 5, y + h / 5, w * 3 / 5, h * 3 / 5, 4, 4);
        g.setColor(Color.WHITE);
        String glyph = (key == null || key.isEmpty()) ? "?"
                : key.substring(key.lastIndexOf('.') + 1, key.lastIndexOf('.') + 2).toUpperCase();
        g.drawString(glyph, x + w / 2 - 3, y + h / 2 + 4);
    }

    private static void hint(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
}
