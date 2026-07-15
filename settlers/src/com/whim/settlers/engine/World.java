package com.whim.settlers.engine;

import com.whim.settlers.buildings.Building;
import com.whim.settlers.buildings.BuildingManager;
import com.whim.settlers.buildings.BuildingType;
import com.whim.settlers.economy.Economy;
import com.whim.settlers.map.TileMap;
import com.whim.settlers.transport.TransportSystem;

/**
 * Root game-state container. Holds the map, camera, and buildings; settlers,
 * roads, and players are added in later phases. Kept deliberately thin so the
 * update/render split stays clear.
 */
public final class World {

    /** The human player's id for this single-player-first build. */
    public static final int PLAYER_ID = 0;

    private final TileMap map;
    private final Camera camera;
    private final BuildingManager buildings;
    private final TransportSystem transport;
    private final Economy economy;

    /** Total simulated time in seconds — handy for animation and debugging. */
    private double clock;

    public World(TileMap map) {
        this.map = map;
        this.camera = new Camera(map.width() / 2.0, map.height() / 2.0);
        this.buildings = new BuildingManager(map);
        this.transport = new TransportSystem(map, buildings);
        this.economy = new Economy(map, buildings, transport);
    }

    /** Advance the simulation by a fixed timestep. */
    public void update(double dtSeconds) {
        clock += dtSeconds;
        float dt = (float) dtSeconds;
        buildings.update(dt);
        // Every building gets a flag next to it as soon as it exists, so the
        // player can road-connect it.
        for (Building b : buildings.all()) transport.ensureFlagFor(b);
        economy.update(dt);   // may enqueue shipments
        transport.update(dt); // moves carriers / delivers
        camera.clampTo(map.width(), map.height());
    }

    /**
     * Found the settlement by placing the Castle on the nearest buildable spot
     * to the map centre, and centre the camera on it. Returns true on success.
     * (Phase 7 will replace this with an interactive founding step.)
     */
    public boolean foundSettlement() {
        int cx = map.width() / 2, cy = map.height() / 2;
        int maxR = Math.max(map.width(), map.height());
        for (int r = 0; r < maxR; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    int ax = cx + dx, ay = cy + dy;
                    if (buildings.canPlace(BuildingType.CASTLE, ax, ay)) {
                        Building castle = buildings.place(BuildingType.CASTLE, ax, ay, PLAYER_ID);
                        transport.registerCastle(castle); // Castle flag = stockpile hub
                        camera.centreOn(ax + 1.5, ay + 1.5);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public TileMap map()             { return map; }
    public Camera camera()           { return camera; }
    public BuildingManager buildings(){ return buildings; }
    public TransportSystem transport(){ return transport; }
    public Economy economy()         { return economy; }
    public double clock()            { return clock; }
}
