package com.whim.b5wars.ui;

import com.whim.b5wars.model.Race;
import com.whim.b5wars.model.Ship;
import com.whim.b5wars.model.Side;

import java.awt.Color;
import java.awt.Font;

/**
 * Central palette and fonts so every panel reads as one system. Colors are original
 * (no copyrighted Babylon 5 / AOG art or trade dress) — just steel-blue vs amber tones.
 */
public final class UiTheme {

    private UiTheme() {
    }

    // Space / map
    public static final Color SPACE_TOP = new Color(6, 8, 18);
    public static final Color SPACE_BOTTOM = new Color(2, 3, 8);
    public static final Color HEX_LINE = new Color(38, 48, 70);
    public static final Color HEX_LINE_DIM = new Color(24, 30, 46);
    public static final Color SELECT = new Color(255, 232, 120);
    public static final Color VECTOR = new Color(120, 220, 160);
    public static final Color ARC_FILL = new Color(120, 200, 255, 46);
    public static final Color RANGE_RING = new Color(150, 200, 255, 120);

    // Panels
    public static final Color PANEL_BG = new Color(18, 21, 30);
    public static final Color PANEL_BG_ALT = new Color(26, 30, 42);
    public static final Color PANEL_LINE = new Color(60, 70, 92);
    public static final Color TEXT = new Color(222, 228, 240);
    public static final Color TEXT_DIM = new Color(150, 160, 180);

    // Faction colors (per the contract): Earth Alliance = steel blue, Narn = amber/orange.
    public static final Color EARTH_ALLIANCE = new Color(70, 130, 190);
    public static final Color NARN = new Color(232, 150, 46);
    public static final Color NEUTRAL = new Color(170, 170, 180);

    // Damage-box states
    public static final Color BOX_INTACT = new Color(90, 200, 140);
    public static final Color BOX_GONE = new Color(70, 40, 46);
    public static final Color BOX_LINE = new Color(40, 48, 64);

    public static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font FONT_SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    public static final Font FONT_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    public static final Font FONT_HEADER = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    public static final Font FONT_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    public static Color colorForRace(Race race) {
        if (race == Race.EARTH_ALLIANCE) {
            return EARTH_ALLIANCE;
        }
        if (race == Race.NARN_REGIME) {
            return NARN;
        }
        return NEUTRAL;
    }

    public static Color colorForShip(Ship ship) {
        if (ship == null || ship.getType() == null) {
            return NEUTRAL;
        }
        return colorForRace(ship.getType().getRace());
    }

    /** Contrasting outline used to distinguish the two hot-seat sides. */
    public static Color outlineForSide(Side side) {
        return side == Side.A ? new Color(235, 240, 250) : new Color(30, 34, 44);
    }
}
