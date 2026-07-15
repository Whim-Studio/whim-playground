package com.whim.ruinlander.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the initial playable world (a {@link GridMap}) and a starter {@link Player}.
 *
 * <p>Coupling choice: the <b>injection variant</b> is used. {@link #build(Map)} takes a
 * {@code Map<String,Item>} of starter item definitions (the controller passes
 * {@code engine.ItemDb.all()}), so this domain package compiles standalone with zero
 * {@code domain -> engine} reference. A no-argument {@link #build()} convenience exists
 * for tests/standalone runs and fabricates a tiny local item set instead.
 *
 * <p>Generation is fully deterministic — zero RNG — so the same layout is produced every run.
 */
public final class WorldFactory {

    public static final int WIDTH = 24;
    public static final int HEIGHT = 18;

    /** Bundle of the freshly-built world and its player. */
    public static final class World {
        public final GridMap map;
        public final Player player;

        public World(GridMap map, Player player) {
            this.map = map;
            this.player = player;
        }
    }

    private WorldFactory() {
    }

    /** Standalone convenience build using locally-fabricated starter items (no engine needed). */
    public static World build() {
        return build(null);
    }

    /**
     * Build the world, drawing starter/loot items from {@code starterItems} when provided.
     * When {@code starterItems} is null or empty, a minimal local item set is fabricated so
     * the player still receives a usable weapon and a few supplies.
     */
    public static World build(Map<String, Item> starterItems) {
        GridMap map = new GridMap(WIDTH, HEIGHT);
        paintTerrain(map);

        Position start = new Position(2, HEIGHT / 2);
        map.setPlayerStart(start);
        map.getTile(start.x, start.y).setTerrain(TerrainType.ROAD);
        map.getTile(start.x, start.y).setDiscovered(true);

        Player player = new Player(start);
        equipStarter(player, starterItems);

        placeSettlements(map);
        placeContainers(map, starterItems);
        placeEnemies(map);

        return new World(map, player);
    }

    // ---- terrain -----------------------------------------------------------

    private static void paintTerrain(GridMap map) {
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                map.getTile(x, y).setTerrain(terrainFor(x, y));
            }
        }
        // A horizontal road spine across the middle.
        int roadY = map.getHeight() / 2;
        for (int x = 0; x < map.getWidth(); x++) {
            map.getTile(x, roadY).setTerrain(TerrainType.ROAD);
        }
        // A small lake (WATER) — impassable, gives variety.
        for (int y = 3; y <= 5; y++) {
            for (int x = 17; x <= 19; x++) {
                if (map.inBounds(x, y)) {
                    map.getTile(x, y).setTerrain(TerrainType.WATER);
                }
            }
        }
        // A concentrated TOXIC zone in the lower-right.
        for (int y = 12; y <= 15; y++) {
            for (int x = 16; x <= 21; x++) {
                if (map.inBounds(x, y)) {
                    map.getTile(x, y).setTerrain(TerrainType.TOXIC);
                }
            }
        }
    }

    /** Deterministic base terrain selection from coordinates (no RNG). */
    private static TerrainType terrainFor(int x, int y) {
        // Left third: open wasteland. Middle: forest belt. Right: ruined city.
        if (x < 6) {
            return ((x + y) % 7 == 0) ? TerrainType.FOREST : TerrainType.WASTELAND;
        } else if (x < 14) {
            return ((x * 3 + y) % 5 == 0) ? TerrainType.WASTELAND : TerrainType.FOREST;
        } else {
            return ((x + y) % 4 == 0) ? TerrainType.WASTELAND : TerrainType.CITY;
        }
    }

    // ---- entities ----------------------------------------------------------

    private static void placeSettlements(GridMap map) {
        Settlement haven = new Settlement("Rusthaven", Faction.SCAVENGERS, new Position(5, 4));
        map.getTile(5, 4).setTerrain(TerrainType.SETTLEMENT);
        map.setEntity(5, 4, haven);

        Settlement bastion = new Settlement("Enclave Bastion", Faction.ENCLAVE, new Position(20, 9));
        map.getTile(20, 9).setTerrain(TerrainType.SETTLEMENT);
        map.setEntity(20, 9, bastion);
    }

    private static void placeContainers(GridMap map, Map<String, Item> items) {
        int[][] spots = { {8, 2}, {12, 14}, {3, 15}, {15, 6} };
        for (int i = 0; i < spots.length; i++) {
            int x = spots[i][0];
            int y = spots[i][1];
            if (!canPlace(map, x, y)) continue;
            Container c = new Container(new Position(x, y));
            fillLoot(c.getLoot(), items, i);
            map.setEntity(x, y, c);
        }
    }

    private static void placeEnemies(GridMap map) {
        // Raiders near the city, a mutant in the toxic zone, an animal in the forest.
        addEnemy(map, new Enemy(EntityType.RAIDER, "Raider", 24, 6, 8, 0.65, 0.05,
                Faction.RAIDERS, null), 16, 8);
        addEnemy(map, new Enemy(EntityType.RAIDER, "Raider Scout", 18, 7, 6, 0.70, 0.0,
                Faction.RAIDERS, null), 18, 2);
        addEnemy(map, new Enemy(EntityType.MUTANT, "Glow Mutant", 36, 5, 12, 0.55, 0.15,
                Faction.NEUTRAL, null), 19, 13);
        addEnemy(map, new Enemy(EntityType.ANIMAL, "Wild Dog", 14, 8, 5, 0.60, 0.0,
                Faction.NEUTRAL, null), 10, 11);
    }

    private static void addEnemy(GridMap map, Enemy e, int x, int y) {
        if (canPlace(map, x, y)) {
            map.setEntity(x, y, e);
        }
    }

    private static boolean canPlace(GridMap map, int x, int y) {
        Tile t = map.getTile(x, y);
        return t != null && t.isPassable() && !t.hasEntity();
    }

    // ---- items -------------------------------------------------------------

    private static void equipStarter(Player player, Map<String, Item> items) {
        Weapon starter = findWeapon(items);
        if (starter == null) {
            starter = new Weapon("pipe", "Lead Pipe", 2.5,
                    WeaponClass.MELEE, 7, 0.75, 3, 1, null);
        }
        player.equipWeapon(starter);

        // A few starter supplies.
        addStarter(player, items, ItemCategory.FOOD, fallbackFood(), 2);
        addStarter(player, items, ItemCategory.WATER, fallbackWater(), 2);
        addStarter(player, items, ItemCategory.MEDICAL, fallbackBandage(), 1);
        addStarter(player, items, ItemCategory.MATERIAL, fallbackScrap(), 3);
    }

    private static void addStarter(Player player, Map<String, Item> items,
                                   ItemCategory cat, Item fallback, int qty) {
        Item it = findByCategory(items, cat);
        player.getInventory().add(it != null ? it : fallback, qty);
    }

    private static void fillLoot(Inventory loot, Map<String, Item> items, int seedIndex) {
        // Deterministic loot variety driven by the container index (no RNG).
        Item food = orFallback(findByCategory(items, ItemCategory.FOOD), fallbackFood());
        Item water = orFallback(findByCategory(items, ItemCategory.WATER), fallbackWater());
        Item scrap = orFallback(findByCategory(items, ItemCategory.MATERIAL), fallbackScrap());
        Item med = orFallback(findByCategory(items, ItemCategory.MEDICAL), fallbackBandage());

        loot.add(scrap, 1 + (seedIndex % 3));
        if (seedIndex % 2 == 0) {
            loot.add(food, 1);
        } else {
            loot.add(water, 1);
        }
        if (seedIndex % 4 == 1) {
            loot.add(med, 1);
        }
    }

    private static Item orFallback(Item it, Item fb) {
        return it != null ? it : fb;
    }

    private static Weapon findWeapon(Map<String, Item> items) {
        if (items == null) return null;
        for (Item it : items.values()) {
            if (it instanceof Weapon) {
                return (Weapon) it;
            }
        }
        return null;
    }

    private static Item findByCategory(Map<String, Item> items, ItemCategory cat) {
        if (items == null) return null;
        List<Item> matches = new ArrayList<Item>();
        for (Item it : items.values()) {
            if (it.getCategory() == cat && !(it instanceof Weapon) && !(it instanceof Armor)) {
                matches.add(it);
            }
        }
        if (matches.isEmpty()) {
            for (Item it : items.values()) {
                if (it.getCategory() == cat) {
                    matches.add(it);
                }
            }
        }
        return matches.isEmpty() ? null : matches.get(0);
    }

    // Local fallbacks so the world is playable without an injected item map.
    private static Item fallbackFood() {
        return new Item("canned_food", "Canned Food", ItemCategory.FOOD, 0.5, true,
                30, 0, 0, 0, 0);
    }

    private static Item fallbackWater() {
        return new Item("water_bottle", "Water Bottle", ItemCategory.WATER, 0.6, true,
                0, 35, 0, 0, 0);
    }

    private static Item fallbackBandage() {
        return new Item("bandage", "Bandage", ItemCategory.MEDICAL, 0.1, true,
                0, 0, 0, 20, 0);
    }

    private static Item fallbackScrap() {
        return new Item("scrap", "Scrap Metal", ItemCategory.MATERIAL, 0.4, true);
    }
}
