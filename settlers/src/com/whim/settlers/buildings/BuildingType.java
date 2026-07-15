package com.whim.settlers.buildings;

import com.whim.settlers.map.TerrainType;

import java.awt.Color;

/**
 * The full building roster from the design spec, with the metadata the rest of
 * the game keys off: footprint size, placement rule, build time, and (for
 * Phase 3) planks/stone construction cost. Production inputs/outputs are
 * intentionally NOT modelled here — those belong to the Phase 3 economy layer;
 * this enum is about placement and construction.
 *
 * <p>Names follow the original's roster (Woodcutter's Hut, Sawmill, Forester's
 * Hut, …). Costs are {@code // approximate} pending balance tuning.
 */
public enum BuildingType {
    // --- Headquarters ---
    CASTLE      ("Castle",            BuildingCategory.HEADQUARTERS, 3, 3, PlacementRule.LAND,  0,  0, 0f,   'C', new Color(0xEFE4B0)),
    WAREHOUSE   ("Warehouse",         BuildingCategory.HEADQUARTERS, 2, 2, PlacementRule.LAND,  4,  3, 12f,  'W', new Color(0xD9C77A)),

    // --- Wood chain ---
    WOODCUTTER  ("Woodcutter's Hut",  BuildingCategory.WOOD, 1, 1, PlacementRule.LAND, 2, 0, 4f,  'w', new Color(0xB5883F)),
    SAWMILL     ("Sawmill",           BuildingCategory.WOOD, 2, 1, PlacementRule.LAND, 3, 0, 6f,  'S', new Color(0xC79B4B)),
    FORESTER    ("Forester's Hut",    BuildingCategory.WOOD, 1, 1, PlacementRule.LAND, 2, 0, 4f,  'r', new Color(0x8FA84D)),

    // --- Stone ---
    QUARRY      ("Quarryman's Hut",   BuildingCategory.STONE, 1, 1, PlacementRule.LAND, 2, 0, 4f, 'q', new Color(0xBFBFBF)),

    // --- Food ---
    FARM        ("Farm",              BuildingCategory.FOOD, 2, 2, PlacementRule.LAND, 3, 0, 8f,  'F', new Color(0xE3C15A)),
    WINDMILL    ("Windmill",          BuildingCategory.FOOD, 1, 1, PlacementRule.LAND, 3, 1, 6f,  'm', new Color(0xE8D18A)),
    WELL        ("Well",              BuildingCategory.FOOD, 1, 1, PlacementRule.LAND, 1, 1, 3f,  'o', new Color(0x9DC0D8)),
    BAKERY      ("Bakery",            BuildingCategory.FOOD, 1, 1, PlacementRule.LAND, 3, 1, 6f,  'b', new Color(0xE0A85A)),
    FISHERMAN   ("Fisherman's Hut",   BuildingCategory.FOOD, 1, 1, PlacementRule.COAST,2, 0, 4f, 'h', new Color(0x6FA8C8)),
    PIG_FARM    ("Pig Farm",          BuildingCategory.FOOD, 2, 2, PlacementRule.LAND, 3, 0, 8f,  'p', new Color(0xD9A0A0)),
    BUTCHER     ("Butcher's Shop",    BuildingCategory.FOOD, 1, 1, PlacementRule.LAND, 3, 1, 6f,  'B', new Color(0xC97A7A)),

    // --- Mines (each on its matching mountain terrain) ---
    COAL_MINE   ("Coal Mine",         BuildingCategory.MINE, 1, 1, PlacementRule.MOUNTAIN, 2, 0, 5f, 'C', new Color(0x4A4A4A), TerrainType.MOUNTAIN_COAL),
    IRON_MINE   ("Iron Mine",         BuildingCategory.MINE, 1, 1, PlacementRule.MOUNTAIN, 2, 0, 5f, 'I', new Color(0x8A6F5A), TerrainType.MOUNTAIN_IRON),
    GOLD_MINE   ("Gold Mine",         BuildingCategory.MINE, 1, 1, PlacementRule.MOUNTAIN, 2, 0, 5f, 'G', new Color(0xB59A3D), TerrainType.MOUNTAIN_GOLD),
    STONE_MINE  ("Granite Mine",      BuildingCategory.MINE, 1, 1, PlacementRule.MOUNTAIN, 2, 0, 5f, 'N', new Color(0x9A9A9A), TerrainType.MOUNTAIN_STONE),

    // --- Metal ---
    IRON_FOUNDRY("Iron Foundry",      BuildingCategory.METAL, 1, 1, PlacementRule.LAND, 3, 1, 7f, 'y', new Color(0xB08D57)),
    GOLD_FOUNDRY("Gold Foundry",      BuildingCategory.METAL, 1, 1, PlacementRule.LAND, 3, 1, 7f, 'u', new Color(0xC9A94B)),

    // --- Tools & weapons ---
    TOOLMAKER   ("Tool Maker's Shop", BuildingCategory.TOOLS, 1, 1, PlacementRule.LAND, 3, 1, 7f, 't', new Color(0x9AA0A6)),
    BLACKSMITH  ("Blacksmith's",      BuildingCategory.TOOLS, 1, 1, PlacementRule.LAND, 3, 1, 7f, 'k', new Color(0x7A7F85)),

    // --- Military (ascending tiers) ---
    GUARD_HUT   ("Guard Hut",         BuildingCategory.MILITARY, 1, 1, PlacementRule.LAND, 3, 2, 6f,  '1', new Color(0xD86A6A)),
    GUARD_TOWER ("Guard Tower",       BuildingCategory.MILITARY, 1, 1, PlacementRule.LAND, 4, 4, 9f,  '2', new Color(0xC85A5A)),
    GARRISON    ("Garrison",          BuildingCategory.MILITARY, 2, 2, PlacementRule.LAND, 6, 6, 14f, '3', new Color(0xB04A4A)),

    // --- Shipping (stretch) ---
    SHIPYARD    ("Ship Maker's",      BuildingCategory.SHIPPING, 2, 1, PlacementRule.COAST, 4, 0, 9f, 'z', new Color(0x5A8FC8));

    private final String displayName;
    private final BuildingCategory category;
    private final int footprintW, footprintH;
    private final PlacementRule placement;
    private final int planksCost, stoneCost;
    private final float buildTimeSeconds;
    private final char glyph;
    private final Color color;
    private final TerrainType requiredResource; // nullable; only for mines

    BuildingType(String displayName, BuildingCategory category, int w, int h,
                 PlacementRule placement, int planksCost, int stoneCost,
                 float buildTimeSeconds, char glyph, Color color) {
        this(displayName, category, w, h, placement, planksCost, stoneCost,
             buildTimeSeconds, glyph, color, null);
    }

    BuildingType(String displayName, BuildingCategory category, int w, int h,
                 PlacementRule placement, int planksCost, int stoneCost,
                 float buildTimeSeconds, char glyph, Color color,
                 TerrainType requiredResource) {
        this.displayName = displayName;
        this.category = category;
        this.footprintW = w;
        this.footprintH = h;
        this.placement = placement;
        this.planksCost = planksCost;
        this.stoneCost = stoneCost;
        this.buildTimeSeconds = buildTimeSeconds;
        this.glyph = glyph;
        this.color = color;
        this.requiredResource = requiredResource;
    }

    public String displayName()      { return displayName; }
    public BuildingCategory category(){ return category; }
    public int footprintW()          { return footprintW; }
    public int footprintH()          { return footprintH; }
    public PlacementRule placement()  { return placement; }
    public int planksCost()          { return planksCost; }
    public int stoneCost()           { return stoneCost; }
    public float buildTimeSeconds()  { return buildTimeSeconds; }
    public char glyph()              { return glyph; }
    public Color color()             { return color; }
    public TerrainType requiredResource() { return requiredResource; }

    public boolean isMilitary() { return category == BuildingCategory.MILITARY; }
}
