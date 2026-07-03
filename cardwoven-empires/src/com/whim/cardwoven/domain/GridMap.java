package com.whim.cardwoven.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.whim.cardwoven.api.Enums.BuildingType;
import com.whim.cardwoven.api.Enums.TerrainType;
import com.whim.cardwoven.api.Views.BuildingView;
import com.whim.cardwoven.api.Views.MapView;
import com.whim.cardwoven.api.Views.TileView;

/**
 * The rectangular grid of {@link Tile}s. Terrain is generated deterministically
 * from the shared seeded {@link Random}. Building ids come from the game's
 * {@link IdGenerator} so they are unique across the game.
 */
public final class GridMap implements MapView {

    private final int rows;
    private final int cols;
    private final Tile[][] tiles;
    private final IdGenerator buildingIds;

    public GridMap(int rows, int cols, Random random, IdGenerator buildingIds) {
        this.rows = rows;
        this.cols = cols;
        this.buildingIds = buildingIds;
        this.tiles = new Tile[rows][cols];
        generate(random);
    }

    private void generate(Random random) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                tiles[r][c] = new Tile(r, c, rollTerrain(random));
            }
        }
    }

    private static TerrainType rollTerrain(Random random) {
        int roll = random.nextInt(100);
        if (roll < 40) {
            return TerrainType.PLAINS;
        }
        if (roll < 60) {
            return TerrainType.FOREST;
        }
        if (roll < 75) {
            return TerrainType.MOUNTAIN;
        }
        if (roll < 90) {
            return TerrainType.WATER;
        }
        return TerrainType.DESERT;
    }

    public boolean inBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    /** The tile at (row,col), or null if out of bounds. */
    public Tile tileAt(int row, int col) {
        if (!inBounds(row, col)) {
            return null;
        }
        return tiles[row][col];
    }

    /** Whether any orthogonally-adjacent tile is WATER (Ports require this). */
    public boolean isWaterAdjacent(int row, int col) {
        return isWater(row - 1, col) || isWater(row + 1, col)
                || isWater(row, col - 1) || isWater(row, col + 1);
    }

    private boolean isWater(int row, int col) {
        return inBounds(row, col)
                && tiles[row][col].terrain() == TerrainType.WATER;
    }

    /**
     * Place a building for {@code ownerIndex} from a BUILDING card onto the tile.
     * Returns the new {@link Building}, or null if the tile is missing/occupied
     * or the card is not a building. Terrain/cost legality is the engine's job.
     */
    public Building placeBuilding(int ownerIndex, Card buildingCard,
                                  int row, int col) {
        BuildingType bt = buildingCard == null ? null : buildingCard.buildingType();
        Tile t = tileAt(row, col);
        if (bt == null || t == null || t.building() != null) {
            return null;
        }
        Building b = new Building(buildingIds.next(), bt, row, col, ownerIndex);
        t.setBuilding(b);
        return b;
    }

    /** Remove a building from its tile (no-op if not present). */
    public void removeBuilding(Building b) {
        if (b == null) {
            return;
        }
        Tile t = tileAt(b.row(), b.col());
        if (t != null && t.building() == b) {
            t.setBuilding(null);
        }
    }

    // --- MapView ---
    public int rows() { return rows; }
    public int cols() { return cols; }

    public TileView tile(int row, int col) {
        return tileAt(row, col);
    }

    public List<BuildingView> buildingsOf(int playerIndex) {
        List<BuildingView> out = new ArrayList<BuildingView>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Building b = tiles[r][c].buildingConcrete();
                if (b != null && b.ownerPlayerIndex() == playerIndex) {
                    out.add(b);
                }
            }
        }
        return out;
    }

    /** Concrete building accessor for the engine (avoids a view cast). */
    public List<Building> buildingsOwnedBy(int playerIndex) {
        List<Building> out = new ArrayList<Building>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Building b = tiles[r][c].buildingConcrete();
                if (b != null && b.ownerPlayerIndex() == playerIndex) {
                    out.add(b);
                }
            }
        }
        return out;
    }
}
