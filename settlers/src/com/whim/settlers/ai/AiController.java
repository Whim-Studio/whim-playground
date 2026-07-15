package com.whim.settlers.ai;

import com.whim.settlers.buildings.Building;
import com.whim.settlers.buildings.BuildingType;
import com.whim.settlers.economy.ProductionChains;
import com.whim.settlers.economy.Recipe;
import com.whim.settlers.engine.World;
import com.whim.settlers.map.TerrainType;
import com.whim.settlers.map.TileMap;
import com.whim.settlers.military.MilitarySystem;

/**
 * A computer opponent that plays by the same rules as the human: it places
 * buildings inside its own territory, connects them to its Castle with roads
 * (so goods relay exactly as the player's do), lets the shared {@link
 * com.whim.settlers.economy.Economy}/{@link MilitarySystem} run its production
 * and knight training, expands its border with guard buildings toward the enemy,
 * and launches attacks once it has a striking force. It calls only the same
 * public systems the UI drives — it has no privileged shortcuts.
 *
 * <p>The strategy is intentionally simple (a fixed build order + greedy site
 * search + threshold attacks); it is a competent opponent, not an optimiser.
 */
public final class AiController {

    /** Economy buildings the AI tries to raise, roughly in dependency order. */
    private static final BuildingType[] BUILD_ORDER = {
        BuildingType.WOODCUTTER, BuildingType.FORESTER, BuildingType.SAWMILL,
        BuildingType.QUARRY, BuildingType.FARM, BuildingType.WELL,
        BuildingType.WINDMILL, BuildingType.BAKERY, BuildingType.COAL_MINE,
        BuildingType.IRON_MINE, BuildingType.IRON_FOUNDRY, BuildingType.TOOLMAKER,
        BuildingType.BLACKSMITH
    };

    private static final int EXTRACT_RADIUS = 5;

    private final World world;
    private final int player;

    private float buildTimer, expandTimer, attackTimer;

    public AiController(World world, int player) {
        this.world = world;
        this.player = player;
    }

    public void update(float dt) {
        buildTimer += dt;
        expandTimer += dt;
        attackTimer += dt;
        if (buildTimer >= 3f)  { buildTimer = 0f;  buildNextEconomyBuilding(); }
        if (expandTimer >= 9f) { expandTimer = 0f; expandTerritory(); }
        if (attackTimer >= 10f){ attackTimer = 0f; maybeAttack(); }
    }

    // -------------------------------------------------------------- economy

    /** Place the first not-yet-built economy building that has a valid site. */
    private void buildNextEconomyBuilding() {
        for (BuildingType type : BUILD_ORDER) {
            if (countOwned(type) > 0) continue;
            int[] site = findSite(type);
            if (site != null) { placeAndConnect(type, site[0], site[1]); return; }
        }
    }

    // -------------------------------------------------------------- military

    /** Push the border toward the enemy by placing a Guard Hut just beyond it. */
    private void expandTerritory() {
        Building enemyHq = nearestEnemyFort(world.military());
        int tx = enemyHq != null ? enemyHq.x() : world.map().width() / 2;
        int ty = enemyHq != null ? enemyHq.y() : world.map().height() / 2;
        int[] best = null;
        int bestDist = Integer.MAX_VALUE;
        TileMap map = world.map();
        for (int y = 0; y < map.height(); y++) {
            for (int x = 0; x < map.width(); x++) {
                if (!world.canPlayerPlace(BuildingType.GUARD_HUT, x, y, player)) continue;
                int d = Math.abs(x - tx) + Math.abs(y - ty);
                if (d < bestDist) { bestDist = d; best = new int[] { x, y }; }
            }
        }
        if (best != null) placeAndConnect(BuildingType.GUARD_HUT, best[0], best[1]);
    }

    private void maybeAttack() {
        MilitarySystem mil = world.military();
        int knights = mil.knightCount(player);
        if (knights < 6) return; // build up a striking force first
        Building target = nearestEnemyFort(mil);
        if (target == null) return;
        mil.launchAttack(player, target, Math.max(2, knights / 2));
    }

    /** Nearest fort not owned by this AI (the human, or a neutral). */
    private Building nearestEnemyFort(MilitarySystem mil) {
        Building castle = ownCastle();
        int cx = castle != null ? castle.x() : world.map().width() / 2;
        int cy = castle != null ? castle.y() : world.map().height() / 2;
        Building best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Building b : world.buildings().all()) {
            if (!MilitarySystem.isFort(b.type()) || b.ownerId() == player || b.ownerId() < 0) continue;
            int d = Math.abs(b.x() - cx) + Math.abs(b.y() - cy);
            if (d < bestDist) { bestDist = d; best = b; }
        }
        return best;
    }

    // --------------------------------------------------------------- helpers

    private void placeAndConnect(BuildingType type, int x, int y) {
        Building b = world.buildings().place(type, x, y, player);
        if (b == null) return;
        int flag = world.transport().ensureFlagFor(b);
        int castle = world.transport().castleFlagId(player);
        if (flag >= 0 && castle >= 0) world.transport().buildRoad(flag, castle);
    }

    /** First tile where {@code type} is placeable and (if a harvester) near its resource. */
    private int[] findSite(BuildingType type) {
        TileMap map = world.map();
        Recipe r = ProductionChains.of(type);
        for (int y = 0; y < map.height(); y++) {
            for (int x = 0; x < map.width(); x++) {
                if (!world.canPlayerPlace(type, x, y, player)) continue;
                if (r != null && !resourceNear(r, x, y)) continue;
                return new int[] { x, y };
            }
        }
        return null;
    }

    private boolean resourceNear(Recipe r, int x, int y) {
        switch (r.extractorNeed()) {
            case NONE:       return true;
            case MOUNTAIN:   return true; // guaranteed by placement on the mountain
            case FOREST:     return terrainNear(x, y, TerrainType.FOREST);
            case STONE_ROCK: return terrainNear(x, y, TerrainType.MOUNTAIN_STONE);
            case WATER:      return terrainNear(x, y, TerrainType.WATER);
            case FARMLAND:   return terrainNear(x, y, TerrainType.GRASS);
            default:         return false;
        }
    }

    private boolean terrainNear(int x, int y, TerrainType want) {
        TileMap map = world.map();
        for (int dy = -EXTRACT_RADIUS; dy <= EXTRACT_RADIUS; dy++) {
            for (int dx = -EXTRACT_RADIUS; dx <= EXTRACT_RADIUS; dx++) {
                int tx = x + dx, ty = y + dy;
                if (map.inBounds(tx, ty) && map.get(tx, ty) == want) return true;
            }
        }
        return false;
    }

    private int countOwned(BuildingType type) {
        int n = 0;
        for (Building b : world.buildings().all()) {
            if (b.ownerId() == player && b.type() == type) n++;
        }
        return n;
    }

    private Building ownCastle() {
        for (Building b : world.buildings().all()) {
            if (b.ownerId() == player && b.type() == BuildingType.CASTLE) return b;
        }
        return null;
    }
}
