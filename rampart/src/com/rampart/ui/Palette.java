package com.rampart.ui;

import com.rampart.model.TileType;

import java.awt.Color;

/**
 * The single color-by-type table for the Rampart renderer. Everything is drawn
 * with {@code Graphics2D} primitives, so this class is the only place tile and
 * unit colors are fixed. Pure presentation data — no game logic.
 */
public final class Palette {
    private Palette() {}

    // ---- Terrain tiles (indexed by TileType) ----
    public static final Color WATER      = new Color(24, 52, 92);
    public static final Color WATER_DEEP = new Color(16, 38, 70);
    public static final Color LAND       = new Color(96, 132, 66);
    public static final Color LAND_EDGE  = new Color(72, 104, 48);
    public static final Color WALL       = new Color(150, 150, 158);
    public static final Color WALL_EDGE  = new Color(96, 96, 104);
    public static final Color RUBBLE     = new Color(110, 96, 84);
    public static final Color CANNON_BG  = new Color(120, 150, 74);
    public static final Color CASTLE_BG  = new Color(120, 150, 74);

    // ---- Entities ----
    public static final Color CASTLE_BODY = new Color(206, 186, 132);
    public static final Color CASTLE_TRIM = new Color(140, 118, 74);
    public static final Color CASTLE_DEAD = new Color(90, 78, 70);
    public static final Color CASTLE_FLAG = new Color(210, 64, 60);

    public static final Color CANNON_BODY = new Color(40, 40, 48);
    public static final Color CANNON_TRIM = new Color(80, 80, 92);
    public static final Color CANNON_READY = new Color(120, 220, 140);
    public static final Color CANNON_RELOAD = new Color(220, 160, 70);

    public static final Color SHIP_HULL   = new Color(70, 46, 32);
    public static final Color SHIP_HULL_HI = new Color(104, 70, 46);
    public static final Color SHIP_SAIL   = new Color(224, 220, 206);
    public static final Color SHIP_DAMAGE = new Color(210, 70, 50);

    // ---- Territory shading / grid ----
    public static final Color ENCLOSED_TINT = new Color(240, 224, 120, 46);
    public static final Color GRID_LINE      = new Color(0, 0, 0, 40);

    // ---- Ghost piece preview ----
    public static final Color GHOST_LEGAL   = new Color(120, 230, 150, 150);
    public static final Color GHOST_ILLEGAL = new Color(230, 90, 80, 150);
    public static final Color GHOST_BORDER  = new Color(255, 255, 255, 180);

    // ---- HUD / screens ----
    public static final Color HUD_BG     = new Color(10, 14, 22);
    public static final Color HUD_TEXT   = new Color(238, 238, 244);
    public static final Color HUD_LABEL  = new Color(230, 190, 70);
    public static final Color HUD_WARN   = new Color(224, 84, 72);
    public static final Color SCREEN_BG  = new Color(14, 20, 34);
    public static final Color SCREEN_TITLE = new Color(232, 200, 96);
    public static final Color OVERLAY    = new Color(0, 0, 0, 150);

    /**
     * The base fill color for a terrain tile.
     *
     * @param type the tile's terrain kind
     * @return the color to paint the cell's body
     */
    public static Color forTile(TileType type) {
        if (type == null) return WATER;
        switch (type) {
            case WATER:  return WATER;
            case LAND:   return LAND;
            case WALL:   return WALL;
            case RUBBLE: return RUBBLE;
            case CANNON: return CANNON_BG;
            case CASTLE: return CASTLE_BG;
            default:     return WATER;
        }
    }

    /**
     * The edge/detail color paired with a terrain tile.
     *
     * @param type the tile's terrain kind
     * @return the color for the cell's border accent
     */
    public static Color edgeFor(TileType type) {
        if (type == null) return WATER_DEEP;
        switch (type) {
            case WATER:  return WATER_DEEP;
            case LAND:   return LAND_EDGE;
            case WALL:   return WALL_EDGE;
            case RUBBLE: return new Color(84, 72, 62);
            case CANNON: return LAND_EDGE;
            case CASTLE: return LAND_EDGE;
            default:     return WATER_DEEP;
        }
    }
}
