package com.whim.alganon.ui;

import com.whim.alganon.api.Enums.ChatChannel;
import com.whim.alganon.api.Enums.ControlState;
import com.whim.alganon.api.Enums.Faction;
import com.whim.alganon.api.Enums.ResourceType;
import com.whim.alganon.api.Enums.TileType;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Central palette + font + small drawing helpers so every panel reads as one dark, slightly
 * "arcane parchment" themed UI. Colors are picked, not sampled from any Alganon asset.
 */
public final class UiTheme {
    private UiTheme() {}

    // --- core surface palette ---
    public static final Color BG            = new Color(0x14, 0x12, 0x1B);
    public static final Color PANEL         = new Color(0x22, 0x1F, 0x2E);
    public static final Color PANEL_LIGHT   = new Color(0x2E, 0x2A, 0x3D);
    public static final Color PANEL_DARK    = new Color(0x18, 0x16, 0x22);
    public static final Color BORDER        = new Color(0x4A, 0x42, 0x60);
    public static final Color BORDER_HI     = new Color(0x7C, 0x6C, 0xA8);
    public static final Color TEXT          = new Color(0xE6, 0xE1, 0xF0);
    public static final Color TEXT_DIM      = new Color(0x9A, 0x92, 0xB0);
    public static final Color TEXT_FAINT    = new Color(0x6A, 0x63, 0x80);
    public static final Color ACCENT        = new Color(0xC9, 0xA8, 0x5A); // gold
    public static final Color ACCENT_HOT    = new Color(0xE8, 0xC6, 0x76);

    // --- semantic ---
    public static final Color HP            = new Color(0xC0, 0x3A, 0x3A);
    public static final Color HP_BG         = new Color(0x3A, 0x1A, 0x1A);
    public static final Color XP            = new Color(0x7A, 0x5A, 0xC0);
    public static final Color GOOD          = new Color(0x6C, 0xC0, 0x74);
    public static final Color BAD           = new Color(0xD0, 0x5A, 0x5A);

    public static final Color ASHARR        = new Color(0x5A, 0x8A, 0xC0);
    public static final Color KUJIX         = new Color(0xB0, 0x50, 0x50);

    public static final Font FONT_TITLE  = new Font("Serif", Font.BOLD, 46);
    public static final Font FONT_H1     = new Font("SansSerif", Font.BOLD, 22);
    public static final Font FONT_H2     = new Font("SansSerif", Font.BOLD, 16);
    public static final Font FONT_BODY   = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_SMALL  = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_MONO   = new Font("Monospaced", Font.PLAIN, 12);

    public static Color resourceColor(ResourceType t) {
        if (t == null) return new Color(0x50, 0x80, 0xC0);
        switch (t) {
            case FURY:  return new Color(0xC8, 0x6A, 0x3A);
            case FOCUS: return new Color(0x6A, 0xC0, 0x8A);
            case MANA:  return new Color(0x4A, 0x7A, 0xD0);
            default:    return new Color(0x50, 0x80, 0xC0);
        }
    }

    public static Color factionColor(Faction f) {
        return f == Faction.KUJIX ? KUJIX : ASHARR;
    }

    public static Color controlColor(ControlState c) {
        if (c == null) return TEXT_DIM;
        switch (c) {
            case ASHARR:    return ASHARR;
            case KUJIX:     return KUJIX;
            case CONTESTED: return ACCENT;
            default:        return TEXT_FAINT;
        }
    }

    public static Color channelColor(ChatChannel ch) {
        if (ch == null) return TEXT;
        switch (ch) {
            case SYSTEM:  return new Color(0xC9, 0xA8, 0x5A);
            case SAY:     return TEXT;
            case FAMILY:  return new Color(0x8A, 0xC0, 0x6C);
            case FACTION: return new Color(0x6A, 0x9A, 0xD0);
            case COMBAT:  return new Color(0xD0, 0x7A, 0x6A);
            case LOOT:    return new Color(0xC0, 0x8A, 0xD0);
            default:      return TEXT;
        }
    }

    public static Color tileColor(TileType t) {
        if (t == null) return new Color(0x10, 0x10, 0x14);
        switch (t) {
            case GRASS: return new Color(0x33, 0x4A, 0x2E);
            case DIRT:  return new Color(0x4A, 0x3A, 0x28);
            case STONE: return new Color(0x40, 0x40, 0x48);
            case WATER: return new Color(0x22, 0x3A, 0x58);
            case WALL:  return new Color(0x2A, 0x26, 0x30);
            case ROAD:  return new Color(0x55, 0x4A, 0x38);
            case SAND:  return new Color(0x6A, 0x5E, 0x40);
            case FLOOR: return new Color(0x38, 0x34, 0x40);
            case VOID:  return new Color(0x0A, 0x0A, 0x0E);
            default:    return new Color(0x20, 0x20, 0x28);
        }
    }

    public static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /** Panel background with a subtle border, the shared "card" look. */
    public static void panel(Graphics2D g, int x, int y, int w, int h) {
        panel(g, x, y, w, h, PANEL);
    }

    public static void panel(Graphics2D g, int x, int y, int w, int h, Color fill) {
        g.setColor(fill);
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(BORDER);
        g.drawRoundRect(x, y, w - 1, h - 1, 10, 10);
    }

    /** A labelled progress bar (hp/resource/xp). value/max clamped. */
    public static void bar(Graphics2D g, int x, int y, int w, int h, double frac, Color fill, Color bg) {
        double f = frac < 0 ? 0 : (frac > 1 ? 1 : frac);
        g.setColor(bg);
        g.fillRoundRect(x, y, w, h, h, h);
        g.setColor(fill);
        g.fillRoundRect(x, y, (int) Math.round(w * f), h, h, h);
        g.setColor(BORDER);
        g.drawRoundRect(x, y, w - 1, h - 1, h, h);
    }

    public static Color mix(Color a, Color b, double t) {
        double u = 1 - t;
        return new Color(
                (int) (a.getRed() * u + b.getRed() * t),
                (int) (a.getGreen() * u + b.getGreen() * t),
                (int) (a.getBlue() * u + b.getBlue() * t));
    }
}
