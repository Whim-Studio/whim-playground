package com.midnight.ui;

import java.awt.Color;

import com.midnight.core.Terrain;

/**
 * Shared colour/label lookups for terrain and a couple of small drawing
 * constants. Keeps {@link LandscapePanel} and {@link MapPanel} visually
 * consistent without either re-deriving the palette.
 */
final class TerrainArt {

    private TerrainArt() {
    }

    /** Flat top-down fill colour for a terrain type (used by the strategic map). */
    static Color mapColor(Terrain t) {
        if (t == null) {
            return new Color(0x6B8E5A);
        }
        switch (t) {
            case PLAINS:    return new Color(0x7FA85C);
            case DOWNS:     return new Color(0xA8B36A);
            case FOREST:    return new Color(0x2E5E33);
            case MOUNTAINS: return new Color(0x8A8A8A);
            case SNOW:      return new Color(0xE8EEF4);
            case WASTELAND: return new Color(0x9C7B53);
            case LAKE:      return new Color(0x3A6EA5);
            case CITADEL:   return new Color(0xB0A48C);
            case KEEP:      return new Color(0x9E9379);
            case TOWER:     return new Color(0x8C8270);
            case VILLAGE:   return new Color(0xC8A86A);
            case HENGE:     return new Color(0x7C8794);
            case RUINS:     return new Color(0x6E6A60);
            default:        return new Color(0x6B8E5A);
        }
    }

    /** Mid/foreground colour used by the first-person view (a touch richer). */
    static Color groundColor(Terrain t) {
        if (t == null) {
            return new Color(0x5E7E45);
        }
        switch (t) {
            case PLAINS:    return new Color(0x6F9A4E);
            case DOWNS:     return new Color(0x9AA75E);
            case FOREST:    return new Color(0x24502A);
            case MOUNTAINS: return new Color(0x7C7C7C);
            case SNOW:      return new Color(0xDCE6EE);
            case WASTELAND: return new Color(0x8A6A45);
            case LAKE:      return new Color(0x356596);
            case CITADEL:   return new Color(0xA89A80);
            case KEEP:      return new Color(0x948A70);
            case TOWER:     return new Color(0x827966);
            case VILLAGE:   return new Color(0xBE9E60);
            case HENGE:     return new Color(0x727D8A);
            case RUINS:     return new Color(0x645F56);
            default:        return new Color(0x5E7E45);
        }
    }

    static boolean isHighGround(Terrain t) {
        return t == Terrain.MOUNTAINS || t == Terrain.SNOW;
    }

    static boolean hasStructure(Terrain t) {
        return t == Terrain.CITADEL || t == Terrain.KEEP || t == Terrain.TOWER
                || t == Terrain.VILLAGE || t == Terrain.HENGE || t == Terrain.RUINS;
    }

    static boolean isTrees(Terrain t) {
        return t == Terrain.FOREST;
    }
}
