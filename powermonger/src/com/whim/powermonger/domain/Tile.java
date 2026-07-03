package com.whim.powermonger.domain;

import com.whim.powermonger.api.Enums.TerrainType;
import com.whim.powermonger.api.Views;

/**
 * A single map cell. Passive state only — the engine mutates snow/food as the
 * simulation advances. Implements {@link Views.TileView} for the UI.
 */
public final class Tile implements Views.TileView {

    private final int x;
    private final int y;
    private TerrainType terrain;
    private int elevation;      // 0..maxElevation
    private boolean trees;
    private int foodPotential;  // 0..100
    private boolean snow;
    private int townId = -1;

    public Tile(int x, int y, TerrainType terrain, int elevation) {
        this.x = x;
        this.y = y;
        this.terrain = terrain;
        this.elevation = elevation;
    }

    // ---- Views.TileView ----
    @Override public int x() { return x; }
    @Override public int y() { return y; }
    @Override public TerrainType terrain() { return terrain; }
    @Override public int elevation() { return elevation; }
    @Override public boolean hasTown() { return townId >= 0; }
    @Override public int townId() { return townId; }
    @Override public boolean hasTrees() { return trees; }
    @Override public int foodPotential() { return foodPotential; }
    @Override public boolean snowCovered() { return snow; }

    // ---- mutators (engine / generator) ----
    public void setTerrain(TerrainType terrain) { this.terrain = terrain; }
    public void setElevation(int elevation) { this.elevation = elevation; }
    public void setTrees(boolean trees) { this.trees = trees; }
    public void setFoodPotential(int foodPotential) {
        this.foodPotential = clamp(foodPotential);
    }
    public void setSnow(boolean snow) { this.snow = snow; }
    public void setTownId(int townId) { this.townId = townId; }

    private static int clamp(int v) {
        if (v < 0) return 0;
        if (v > 100) return 100;
        return v;
    }
}
