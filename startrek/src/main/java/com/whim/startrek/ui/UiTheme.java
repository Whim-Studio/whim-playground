package com.whim.startrek.ui;

import java.awt.Color;

import com.whim.startrek.domain.MapObjectType;
import com.whim.startrek.domain.Race;

/**
 * Shared colors / small drawing constants for the UI. Pure data; no Swing state.
 * Keeping these in one place so the galaxy + battle views read consistently.
 */
final class UiTheme {

    private UiTheme() { }

    static final Color SPACE_BG    = new Color(6, 8, 18);
    static final Color GRID_LINE   = new Color(30, 38, 64);
    static final Color PANEL_BG    = new Color(12, 16, 30);
    static final Color TEXT        = new Color(214, 224, 245);
    static final Color TEXT_DIM    = new Color(132, 146, 178);
    static final Color SELECT      = new Color(255, 214, 92);
    static final Color HULL        = new Color(214, 84, 70);
    static final Color SHIELD      = new Color(86, 156, 255);
    static final Color PHASER      = new Color(255, 120, 96);
    static final Color TORPEDO     = new Color(180, 130, 255);

    /** Stable per-race color so empires read the same across both views. */
    static Color raceColor(Race r) {
        if (r == null) {
            return new Color(150, 150, 160);
        }
        switch (r) {
            case FEDERATION: return new Color(86, 156, 255);
            case KLINGON:    return new Color(208, 72, 60);
            case ROMULAN:    return new Color(86, 200, 120);
            case DOMINION:   return new Color(190, 150, 70);
            case AKAALI:     return new Color(170, 120, 210);
            case OCAMPA:     return new Color(110, 200, 210);
            default:         return new Color(180, 180, 190);
        }
    }

    /** Base fill for a terrain cell; system/fleet decorations are drawn on top. */
    static Color terrainColor(MapObjectType t) {
        if (t == null) {
            return SPACE_BG;
        }
        switch (t) {
            case NEBULA:            return new Color(70, 50, 110);
            case ENERGY_STORM:      return new Color(40, 80, 120);
            case SUPERNOVA:         return new Color(120, 70, 30);
            case STABLE_WORMHOLE:   return new Color(30, 90, 90);
            case UNSTABLE_WORMHOLE: return new Color(90, 60, 30);
            case BLACK_HOLE:        return new Color(20, 20, 28);
            case SUPER_BLACK_HOLE:  return new Color(10, 10, 16);
            case SOLAR_SYSTEM:      return new Color(18, 24, 44);
            case EMPTY:
            default:                return SPACE_BG;
        }
    }
}
