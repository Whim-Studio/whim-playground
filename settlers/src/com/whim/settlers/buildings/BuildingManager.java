package com.whim.settlers.buildings;

import com.whim.settlers.map.TerrainType;
import com.whim.settlers.map.TileMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns all placed buildings and enforces placement rules. Keeps an occupancy
 * grid so validity and overlap checks are O(footprint) rather than O(buildings).
 */
public final class BuildingManager {

    private final TileMap map;
    private final List<Building> buildings = new ArrayList<Building>();
    /** Index into {@link #buildings} per tile, or -1 if free. */
    private final int[] occupancy;

    public BuildingManager(TileMap map) {
        this.map = map;
        this.occupancy = new int[map.width() * map.height()];
        java.util.Arrays.fill(occupancy, -1);
    }

    /** Advance construction for every building. */
    public void update(float dtSeconds) {
        for (int i = 0; i < buildings.size(); i++) {
            buildings.get(i).update(dtSeconds);
        }
    }

    /**
     * Whether {@code type} may be placed with its top-left at {@code (ax, ay)}.
     * Checks bounds, terrain rule for every footprint tile, mine resource match,
     * and that no tile is already occupied.
     */
    public boolean canPlace(BuildingType type, int ax, int ay) {
        int w = type.footprintW(), h = type.footprintH();
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                int tx = ax + dx, ty = ay + dy;
                if (!map.inBounds(tx, ty)) return false;
                if (occupancy[ty * map.width() + tx] != -1) return false;
                if (!terrainOk(type, tx, ty)) return false;
            }
        }
        return true;
    }

    private boolean terrainOk(BuildingType type, int tx, int ty) {
        TerrainType terrain = map.get(tx, ty);
        switch (type.placement()) {
            case LAND:
                return terrain.buildable();
            case MOUNTAIN:
                if (!terrain.isMountain()) return false;
                // Mines require their specific mountain resource, if pinned.
                return type.requiredResource() == null
                    || terrain == type.requiredResource();
            case COAST:
                return terrain.buildable() && adjacentToWater(tx, ty);
            default:
                return false;
        }
    }

    private boolean adjacentToWater(int tx, int ty) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = tx + dx, ny = ty + dy;
                if (map.inBounds(nx, ny) && map.get(nx, ny).isWater()) return true;
            }
        }
        return false;
    }

    /**
     * Place a building if valid. Returns the new {@link Building} or {@code null}
     * if placement was rejected.
     */
    public Building place(BuildingType type, int ax, int ay, int ownerId) {
        if (!canPlace(type, ax, ay)) return null;
        Building b = new Building(type, ax, ay, ownerId);
        int index = buildings.size();
        buildings.add(b);
        for (int dy = 0; dy < type.footprintH(); dy++) {
            for (int dx = 0; dx < type.footprintW(); dx++) {
                occupancy[(ay + dy) * map.width() + (ax + dx)] = index;
            }
        }
        return b;
    }

    /** Building covering a tile, or {@code null}. */
    public Building at(int tx, int ty) {
        if (!map.inBounds(tx, ty)) return null;
        int idx = occupancy[ty * map.width() + tx];
        return idx == -1 ? null : buildings.get(idx);
    }

    public List<Building> all() {
        return Collections.unmodifiableList(buildings);
    }

    public int count() { return buildings.size(); }
}
