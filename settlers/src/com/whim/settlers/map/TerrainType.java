package com.whim.settlers.map;

import java.awt.Color;

/**
 * Terrain classes for the tile world. Colours here are procedural placeholder
 * art (per the project's clean-room art policy) — no original game assets.
 *
 * <p>Phase 0 only renders {@link #GRASS} and {@link #WATER}; the mountain/
 * resource variants exist so the palette and map model are stable when Phase 1
 * fills in real terrain generation.
 */
public enum TerrainType {
    GRASS       (new Color(0x6Fae4d), true),
    FOREST      (new Color(0x3f7d32), true),
    DESERT      (new Color(0xd9c48a), true),
    WATER       (new Color(0x2f6fb0), false),
    // Mountain variants carry a mineable resource in later phases.
    MOUNTAIN_COAL (new Color(0x5a5a5a), false),
    MOUNTAIN_IRON (new Color(0x8a6f5a), false),
    MOUNTAIN_GOLD (new Color(0xb59a3d), false),
    MOUNTAIN_STONE(new Color(0x9a9a9a), false);

    private final Color color;
    private final boolean buildable;

    TerrainType(Color color, boolean buildable) {
        this.color = color;
        this.buildable = buildable;
    }

    /** Placeholder render colour for this terrain. */
    public Color color() { return color; }

    /** Whether ordinary (non-mine) buildings may be placed on this terrain. */
    public boolean buildable() { return buildable; }

    /** True for the four mineable mountain terrains. */
    public boolean isMountain() {
        return this == MOUNTAIN_COAL || this == MOUNTAIN_IRON
            || this == MOUNTAIN_GOLD || this == MOUNTAIN_STONE;
    }

    public boolean isWater() { return this == WATER; }

    /**
     * Single-character code used by the hand-built map text format and
     * {@code io/MapLoader}. Stable identifiers so maps stay human-editable.
     */
    public char code() {
        switch (this) {
            case GRASS:          return '.';
            case FOREST:         return 'f';
            case DESERT:         return 'd';
            case WATER:          return 'w';
            case MOUNTAIN_COAL:  return 'c';
            case MOUNTAIN_IRON:  return 'i';
            case MOUNTAIN_GOLD:  return 'g';
            case MOUNTAIN_STONE: return 's';
            default:             return '?';
        }
    }

    /** Inverse of {@link #code()}; returns GRASS for unknown characters. */
    public static TerrainType fromCode(char c) {
        for (TerrainType t : values()) {
            if (t.code() == c) return t;
        }
        return GRASS;
    }
}
