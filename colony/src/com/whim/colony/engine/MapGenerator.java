package com.whim.colony.engine;

import com.whim.colony.ColonyState;
import com.whim.colony.domain.Building;
import com.whim.colony.domain.BuildingType;
import com.whim.colony.domain.Colonist;
import com.whim.colony.domain.ColonyMap;
import com.whim.colony.domain.MapTile;
import com.whim.colony.domain.Resources;
import com.whim.colony.domain.TerrainType;

import java.util.Random;

/**
 * Builds a ready-to-run {@link ColonyState}: a sculpted {@link ColonyMap} with
 * terrain and a small base (stockpile, beds, a farm and a few walls), a handful
 * of colonists placed on walkable ground, and a starting resource stockpile.
 *
 * <p>World generation lives here in the engine so that Task 3 (UI) and the final
 * Main both get a real, populated world to render without owning any sim logic.
 * Pass a seeded {@link Random} for reproducible maps.
 */
public final class MapGenerator {

    /** Default world dimensions. */
    public static final int DEFAULT_WIDTH = 40;
    public static final int DEFAULT_HEIGHT = 30;
    /** Default number of starting colonists. */
    public static final int DEFAULT_COLONISTS = 4;

    private static final String[] NAMES = {
            "Ava", "Boone", "Cira", "Dex", "Esk", "Fen", "Gale", "Hollis",
            "Ivo", "Juno", "Katya", "Lom", "Mira", "Nero", "Odell", "Pell"
    };

    private final Random rng;

    public MapGenerator(Random rng) {
        this.rng = rng;
    }

    /** Generate a default-sized colony with the default colonist count. */
    public ColonyState generate() {
        return generate(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_COLONISTS);
    }

    /**
     * Generate a colony of the given size and population.
     *
     * @param width      map width in tiles (min 10)
     * @param height     map height in tiles (min 10)
     * @param colonists  number of colonists to place (min 1)
     */
    public ColonyState generate(int width, int height, int colonists) {
        width = Math.max(10, width);
        height = Math.max(10, height);
        colonists = Math.max(1, colonists);

        ColonyMap map = new ColonyMap(width, height);
        sculptTerrain(map);
        placeBase(map);

        Resources resources = new Resources(50, 30, 40);
        ColonyState state = new ColonyState(map, resources);

        placeColonists(state, colonists);
        state.addMessage("The colony is founded. " + colonists + " settlers get to work.");
        return state;
    }

    /** Scatter dirt patches plus impassable rock and water across the grass. */
    private void sculptTerrain(ColonyMap map) {
        int cells = map.getWidth() * map.getHeight();

        blobs(map, TerrainType.DIRT, cells / 40, 4);
        blobs(map, TerrainType.WATER, cells / 120, 3);
        blobs(map, TerrainType.ROCK, cells / 90, 3);
    }

    /** Grow {@code count} rough blobs of {@code terrain}, each up to {@code size} tiles. */
    private void blobs(ColonyMap map, TerrainType terrain, int count, int size) {
        for (int i = 0; i < count; i++) {
            int cx = rng.nextInt(map.getWidth());
            int cy = rng.nextInt(map.getHeight());
            int n = 1 + rng.nextInt(Math.max(1, size));
            for (int j = 0; j < n; j++) {
                int x = cx + rng.nextInt(3) - 1;
                int y = cy + rng.nextInt(3) - 1;
                MapTile tile = map.getTile(x, y);
                if (tile != null) {
                    tile.setTerrain(terrain);
                }
            }
        }
    }

    /** Lay down a small starting base near the map centre. */
    private void placeBase(ColonyMap map) {
        int cx = map.getWidth() / 2;
        int cy = map.getHeight() / 2;

        // Clear a walkable pad so the base is never founded on rock/water.
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int y = cy - 3; y <= cy + 3; y++) {
                MapTile tile = map.getTile(x, y);
                if (tile != null) {
                    tile.setTerrain(TerrainType.GRASS);
                    tile.setBuilding(null);
                }
            }
        }

        placeBuilding(map, cx, cy, BuildingType.STOCKPILE);
        placeBuilding(map, cx + 1, cy, BuildingType.STOCKPILE);

        placeBuilding(map, cx - 2, cy - 2, BuildingType.BED);
        placeBuilding(map, cx - 2, cy - 1, BuildingType.BED);
        placeBuilding(map, cx - 2, cy, BuildingType.BED);
        placeBuilding(map, cx - 2, cy + 1, BuildingType.BED);

        placeBuilding(map, cx + 2, cy + 2, BuildingType.FARM);
        placeBuilding(map, cx + 3, cy + 2, BuildingType.FARM);

        // A short wall stub for flavour (blocks movement, exercises pathfinding).
        placeBuilding(map, cx, cy + 3, BuildingType.WALL);
        placeBuilding(map, cx + 1, cy + 3, BuildingType.WALL);
    }

    private void placeBuilding(ColonyMap map, int x, int y, BuildingType type) {
        MapTile tile = map.getTile(x, y);
        if (tile == null) {
            return;
        }
        Building building = new Building(type, tile);
        tile.setBuilding(building);
    }

    /** Place colonists on walkable tiles near the base centre. */
    private void placeColonists(ColonyState state, int count) {
        ColonyMap map = state.getMap();
        int cx = map.getWidth() / 2;
        int cy = map.getHeight() / 2;
        for (int i = 0; i < count; i++) {
            int[] spot = findWalkableNear(map, cx, cy);
            String name = NAMES[i % NAMES.length];
            Colonist colonist = new Colonist(i, name, spot[0], spot[1]);
            // Start slightly imperfect so the AI has something to do soon.
            colonist.getNeeds().setHunger(55 + rng.nextInt(35));
            colonist.getNeeds().setRest(55 + rng.nextInt(35));
            state.getColonists().add(colonist);
        }
    }

    /** Spiral outward from (cx,cy) for the first walkable, building-free tile. */
    private int[] findWalkableNear(ColonyMap map, int cx, int cy) {
        for (int radius = 0; radius < Math.max(map.getWidth(), map.getHeight()); radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.abs(dx) != radius && Math.abs(dy) != radius) {
                        continue; // only the ring at this radius
                    }
                    int x = cx + dx;
                    int y = cy + dy;
                    MapTile tile = map.getTile(x, y);
                    if (tile != null && tile.isWalkable() && !tile.hasBuilding()) {
                        return new int[]{x, y};
                    }
                }
            }
        }
        return new int[]{cx, cy};
    }
}
