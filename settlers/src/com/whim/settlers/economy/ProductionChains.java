package com.whim.settlers.economy;

import com.whim.settlers.buildings.BuildingType;
import com.whim.settlers.economy.Recipe.ExtractorNeed;

import java.util.EnumMap;
import java.util.Map;

/**
 * The production dependency graph: one {@link Recipe} per productive
 * {@link BuildingType}. Buildings absent from the map (Castle, Warehouse,
 * military) have no recipe. Times and quantities are {@code // approximate} —
 * for balance tuning, not confirmed source values.
 */
public final class ProductionChains {

    private static final Map<BuildingType, Recipe> RECIPES =
            new EnumMap<BuildingType, Recipe>(BuildingType.class);

    private ProductionChains() { }

    public static Recipe of(BuildingType type) { return RECIPES.get(type); }

    private static void put(BuildingType t, Recipe r) { RECIPES.put(t, r); }

    private static Map<Good, Integer> in(Good g1, int n1) {
        Map<Good, Integer> m = new EnumMap<Good, Integer>(Good.class);
        m.put(g1, n1);
        return m;
    }

    private static Map<Good, Integer> in(Good g1, int n1, Good g2, int n2) {
        Map<Good, Integer> m = in(g1, n1);
        m.put(g2, n2);
        return m;
    }

    static {
        // --- Wood ---
        put(BuildingType.WOODCUTTER, new Recipe(null, Good.WOOD, 1, 4f,
                Good.AXE, false, ExtractorNeed.FOREST, Profession.WOODCUTTER));
        put(BuildingType.SAWMILL, new Recipe(in(Good.WOOD, 1), Good.PLANK, 1, 3f,
                Good.SAW, false, ExtractorNeed.NONE, Profession.SAWYER));
        // Forester: special (null output) — replants forest near its hut.
        put(BuildingType.FORESTER, new Recipe(null, null, 0, 6f,
                Good.SHOVEL, false, ExtractorNeed.NONE, Profession.FORESTER));

        // --- Stone ---
        put(BuildingType.QUARRY, new Recipe(null, Good.STONE, 1, 5f,
                Good.PICK, false, ExtractorNeed.STONE_ROCK, Profession.STONEMASON));

        // --- Food ---
        put(BuildingType.FARM, new Recipe(null, Good.GRAIN, 1, 7f,
                Good.SCYTHE, false, ExtractorNeed.FARMLAND, Profession.FARMER));
        put(BuildingType.WINDMILL, new Recipe(in(Good.GRAIN, 1), Good.FLOUR, 1, 4f,
                null, false, ExtractorNeed.NONE, Profession.MILLER));
        put(BuildingType.WELL, new Recipe(null, Good.WATER, 1, 4f,
                null, false, ExtractorNeed.FARMLAND, Profession.WELL_DIGGER));
        put(BuildingType.BAKERY, new Recipe(in(Good.FLOUR, 1, Good.WATER, 1), Good.BREAD, 1, 5f,
                null, false, ExtractorNeed.NONE, Profession.BAKER));
        put(BuildingType.FISHERMAN, new Recipe(null, Good.FISH, 1, 5f,
                Good.ROD, false, ExtractorNeed.WATER, Profession.FISHERMAN));
        put(BuildingType.PIG_FARM, new Recipe(in(Good.GRAIN, 1), Good.PIG, 1, 7f,
                null, false, ExtractorNeed.NONE, Profession.PIG_FARMER));
        put(BuildingType.BUTCHER, new Recipe(in(Good.PIG, 1), Good.MEAT, 1, 5f,
                Good.CLEAVER, false, ExtractorNeed.NONE, Profession.BUTCHER));

        // --- Mines (consume food; output fixed by the mountain they sit on) ---
        put(BuildingType.COAL_MINE, new Recipe(null, Good.COAL, 1, 5f,
                Good.PICK, true, ExtractorNeed.MOUNTAIN, Profession.MINER));
        put(BuildingType.IRON_MINE, new Recipe(null, Good.IRON_ORE, 1, 5f,
                Good.PICK, true, ExtractorNeed.MOUNTAIN, Profession.MINER));
        put(BuildingType.GOLD_MINE, new Recipe(null, Good.GOLD_ORE, 1, 5f,
                Good.PICK, true, ExtractorNeed.MOUNTAIN, Profession.MINER));
        put(BuildingType.STONE_MINE, new Recipe(null, Good.STONE, 1, 5f,
                Good.PICK, true, ExtractorNeed.MOUNTAIN, Profession.MINER));

        // --- Metal ---
        put(BuildingType.IRON_FOUNDRY, new Recipe(in(Good.IRON_ORE, 1, Good.COAL, 1), Good.IRON, 1, 6f,
                null, false, ExtractorNeed.NONE, Profession.SMELTER));
        put(BuildingType.GOLD_FOUNDRY, new Recipe(in(Good.GOLD_ORE, 1, Good.COAL, 1), Good.GOLD, 1, 6f,
                null, false, ExtractorNeed.NONE, Profession.GOLDSMITH));

        // --- Tools & weapons (special outputs resolved in Economy) ---
        // Tool maker: IRON + PLANK -> a tool chosen by the player's tool priority.
        put(BuildingType.TOOLMAKER, new Recipe(in(Good.IRON, 1, Good.PLANK, 1), null, 0, 6f,
                null, false, ExtractorNeed.NONE, Profession.TOOLMAKER));
        // Blacksmith: IRON + COAL -> alternating SWORD / SHIELD.
        put(BuildingType.BLACKSMITH, new Recipe(in(Good.IRON, 1, Good.COAL, 1), null, 0, 6f,
                Good.HAMMER, false, ExtractorNeed.NONE, Profession.BLACKSMITH));
    }

    /** The tools the Tool Maker can output, in a stable default priority order. */
    public static Good[] toolOutputs() {
        return new Good[] { Good.AXE, Good.PICK, Good.SAW, Good.SCYTHE,
                            Good.ROD, Good.CLEAVER, Good.SHOVEL, Good.HAMMER };
    }
}
