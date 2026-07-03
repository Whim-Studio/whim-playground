package com.whim.cardwoven.ui;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.whim.cardwoven.api.Enums.AttachmentType;
import com.whim.cardwoven.api.Enums.BuildingType;
import com.whim.cardwoven.api.Enums.CardType;
import com.whim.cardwoven.api.Enums.TerrainType;

/**
 * Central palette + a couple of color helpers for the whole UI. Kept dependency
 * free (java.awt only) and Java 8 compatible. A clean, readable, slightly muted
 * "parchment + ink" palette so procedural shapes stay legible.
 */
public final class UiColors {
    private UiColors() {}

    // ---- Chrome / backgrounds ------------------------------------------
    public static final Color WINDOW_BG   = new Color(0x1E2230);
    public static final Color PANEL_BG    = new Color(0x272C3D);
    public static final Color PANEL_BG_2  = new Color(0x2F3550);
    public static final Color MAP_BG      = new Color(0x15181F);
    public static final Color GRID_LINE   = new Color(0x3A4157);
    public static final Color SELECT_GLOW = new Color(0xFFD86B);

    // ---- Text ----------------------------------------------------------
    public static final Color TEXT        = new Color(0xEDEFF5);
    public static final Color TEXT_MUTED  = new Color(0x9AA3B8);
    public static final Color TEXT_DARK   = new Color(0x1B1E27);

    // ---- Resources -----------------------------------------------------
    public static final Color GOLD        = new Color(0xE8C15A);
    public static final Color COMMAND     = new Color(0x6FA8FF);
    public static final Color DECK        = new Color(0x8FB98A);
    public static final Color DISCARD     = new Color(0xB98A8A);

    // ---- Raiders / danger ----------------------------------------------
    public static final Color RAIDER      = new Color(0xC7503C);

    private static final Map<TerrainType, Color> TERRAIN = new HashMap<TerrainType, Color>();
    private static final Map<TerrainType, Color> TERRAIN_HI = new HashMap<TerrainType, Color>();
    static {
        TERRAIN.put(TerrainType.PLAINS,   new Color(0x7FA65B));
        TERRAIN.put(TerrainType.FOREST,   new Color(0x3F6B41));
        TERRAIN.put(TerrainType.MOUNTAIN, new Color(0x8A8F9C));
        TERRAIN.put(TerrainType.WATER,    new Color(0x3C7CA6));
        TERRAIN.put(TerrainType.DESERT,   new Color(0xCBB472));
        TERRAIN_HI.put(TerrainType.PLAINS,   new Color(0x9BC077));
        TERRAIN_HI.put(TerrainType.FOREST,   new Color(0x568A58));
        TERRAIN_HI.put(TerrainType.MOUNTAIN, new Color(0xA8AEBD));
        TERRAIN_HI.put(TerrainType.WATER,    new Color(0x5AA0CC));
        TERRAIN_HI.put(TerrainType.DESERT,   new Color(0xE3D08F));
    }

    public static Color terrain(TerrainType t) {
        Color c = TERRAIN.get(t);
        return c != null ? c : new Color(0x555B6E);
    }

    public static Color terrainHi(TerrainType t) {
        Color c = TERRAIN_HI.get(t);
        return c != null ? c : new Color(0x6B7285);
    }

    private static final Map<BuildingType, Color> BUILDING = new HashMap<BuildingType, Color>();
    static {
        BUILDING.put(BuildingType.CITY,   new Color(0xE7E1D0));
        BUILDING.put(BuildingType.FARM,   new Color(0xE6C24E));
        BUILDING.put(BuildingType.TEMPLE, new Color(0xC9A2E8));
        BUILDING.put(BuildingType.PORT,   new Color(0x63C6C0));
    }

    public static Color building(BuildingType t) {
        Color c = BUILDING.get(t);
        return c != null ? c : Color.WHITE;
    }

    private static final Map<AttachmentType, Color> ATTACH = new HashMap<AttachmentType, Color>();
    static {
        ATTACH.put(AttachmentType.WORKER, new Color(0xE8C15A));
        ATTACH.put(AttachmentType.IDOL,   new Color(0xC9A2E8));
        ATTACH.put(AttachmentType.WITCH,  new Color(0x6FA8FF));
    }

    public static Color attachment(AttachmentType t) {
        Color c = ATTACH.get(t);
        return c != null ? c : Color.LIGHT_GRAY;
    }

    private static final Map<CardType, Color> CARD = new HashMap<CardType, Color>();
    static {
        CARD.put(CardType.BUILDING,   new Color(0xC9A46A));
        CARD.put(CardType.ATTACHMENT, new Color(0x9C7BC4));
        CARD.put(CardType.MILITARY,   new Color(0xC7503C));
        CARD.put(CardType.ECONOMY,    new Color(0xC9A83C));
        CARD.put(CardType.EXPLORE,    new Color(0x4FA3A0));
        CARD.put(CardType.SIN,        new Color(0x5A4E63));
    }

    public static Color card(CardType t) {
        Color c = CARD.get(t);
        return c != null ? c : new Color(0x777D90);
    }

    /** Blend two colors by t in [0,1]. */
    public static Color mix(Color a, Color b, double t) {
        double u = 1.0 - t;
        return new Color(
            (int) Math.round(a.getRed()   * u + b.getRed()   * t),
            (int) Math.round(a.getGreen() * u + b.getGreen() * t),
            (int) Math.round(a.getBlue()  * u + b.getBlue()  * t));
    }

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }
}
