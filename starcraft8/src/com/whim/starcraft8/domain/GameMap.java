package com.whim.starcraft8.domain;

import java.util.HashMap;
import java.util.Map;

/** Tile grid with terrain and packed-tile resource amounts. */
public final class GameMap {
    private final int width, height;
    private final Terrain[] terrain;
    private final Map<Integer, Integer> resources = new HashMap<Integer, Integer>();

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.terrain = new Terrain[width * height];
        for (int i = 0; i < terrain.length; i++) {
            terrain[i] = Terrain.GROUND;
        }
    }

    public int width() { return width; }
    public int height() { return height; }

    public boolean inBounds(int tx, int ty) {
        return tx >= 0 && ty >= 0 && tx < width && ty < height;
    }

    public Terrain terrainAt(int tx, int ty) {
        if (!inBounds(tx, ty)) return Terrain.UNBUILDABLE;
        return terrain[ty * width + tx];
    }

    public void setTerrain(int tx, int ty, Terrain t) {
        if (!inBounds(tx, ty)) return;
        terrain[ty * width + tx] = t;
    }

    /** All w*h tiles starting at (tx,ty) must be in-bounds and GROUND. */
    public boolean buildable(int tx, int ty, int w, int h) {
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                int cx = tx + dx;
                int cy = ty + dy;
                if (!inBounds(cx, cy)) return false;
                if (terrain[cy * width + cx] != Terrain.GROUND) return false;
            }
        }
        return true;
    }

    public int resourceAt(int tx, int ty) {
        if (!inBounds(tx, ty)) return 0;
        Integer v = resources.get(Integer.valueOf(ty * width + tx));
        return v == null ? 0 : v.intValue();
    }

    public void setResourceAt(int tx, int ty, int amt) {
        if (!inBounds(tx, ty)) return;
        resources.put(Integer.valueOf(ty * width + tx), Integer.valueOf(amt));
    }
}
