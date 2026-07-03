package com.whim.cardwoven.domain;

import com.whim.cardwoven.api.Enums.TerrainType;
import com.whim.cardwoven.api.Views.BuildingView;
import com.whim.cardwoven.api.Views.TileView;

/**
 * A single map tile with terrain, an optional building, exploration state and a
 * neutral raider strength menacing it.
 */
public final class Tile implements TileView {

    private final int row;
    private final int col;
    private final TerrainType terrain;
    private boolean explored;
    private Building building;   // null when empty
    private int raiderStrength;  // 0 when no raiders

    public Tile(int row, int col, TerrainType terrain) {
        this.row = row;
        this.col = col;
        this.terrain = terrain;
    }

    // --- mutation ---
    public void setExplored(boolean explored) { this.explored = explored; }
    public void setBuilding(Building building) { this.building = building; }
    public void setRaiderStrength(int raiderStrength) {
        this.raiderStrength = Math.max(0, raiderStrength);
    }

    /** Concrete building accessor for the engine (avoids a view cast). */
    public Building buildingConcrete() { return building; }

    /** Convenience: a land tile can host most buildings. */
    public boolean isLand() {
        return terrain != TerrainType.WATER;
    }

    // --- TileView ---
    public int row() { return row; }
    public int col() { return col; }
    public TerrainType terrain() { return terrain; }
    public boolean explored() { return explored; }
    public BuildingView building() { return building; }
    public int raiderStrength() { return raiderStrength; }

    @Override
    public String toString() {
        return "Tile(" + row + "," + col + "," + terrain
                + (building != null ? ",b=" + building.type() : "")
                + (raiderStrength > 0 ? ",raid=" + raiderStrength : "") + ")";
    }
}
