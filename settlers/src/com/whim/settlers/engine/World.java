package com.whim.settlers.engine;

import com.whim.settlers.map.TileMap;

/**
 * Root game-state container. Phase 0 holds only the map and camera; buildings,
 * settlers, roads, and players are added in later phases. Kept deliberately thin
 * so the update/render split stays clear.
 */
public final class World {

    private final TileMap map;
    private final Camera camera;

    /** Total simulated time in seconds — handy for animation and debugging. */
    private double clock;

    public World(TileMap map) {
        this.map = map;
        this.camera = new Camera(map.width() / 2.0, map.height() / 2.0);
    }

    /** Advance the simulation by a fixed timestep. */
    public void update(double dtSeconds) {
        clock += dtSeconds;
        camera.clampTo(map.width(), map.height());
    }

    public TileMap map()   { return map; }
    public Camera camera() { return camera; }
    public double clock()  { return clock; }
}
