package com.whim.settlers.economy;

/**
 * Every transportable good in the economy, tagged with a {@link GoodCategory}.
 * Tools gate which buildings can be staffed; weapons/coins feed the Phase 5
 * military. Kept as one enum so inventories and the distribution UI can treat
 * goods uniformly.
 */
public enum Good {
    // Raw / harvested
    WOOD     ("Wood",      GoodCategory.RAW),
    STONE    ("Stone",     GoodCategory.RAW),
    GRAIN    ("Grain",     GoodCategory.RAW),
    WATER    ("Water",     GoodCategory.RAW),
    FISH     ("Fish",      GoodCategory.FOOD),
    PIG      ("Pig",       GoodCategory.RAW),
    COAL     ("Coal",      GoodCategory.RAW),
    IRON_ORE ("Iron ore",  GoodCategory.RAW),
    GOLD_ORE ("Gold ore",  GoodCategory.RAW),

    // Processed materials / food
    PLANK    ("Planks",    GoodCategory.MATERIAL),
    FLOUR    ("Flour",     GoodCategory.MATERIAL),
    BREAD    ("Bread",     GoodCategory.FOOD),
    MEAT     ("Meat",      GoodCategory.FOOD),
    IRON     ("Iron",      GoodCategory.MATERIAL),
    GOLD     ("Gold coin", GoodCategory.COIN),

    // Tools (gate building staffing)
    AXE      ("Axe",       GoodCategory.TOOL),
    SAW      ("Saw",       GoodCategory.TOOL),
    PICK     ("Pick",      GoodCategory.TOOL),
    SCYTHE   ("Scythe",    GoodCategory.TOOL),
    ROD      ("Rod",       GoodCategory.TOOL),
    CLEAVER  ("Cleaver",   GoodCategory.TOOL),
    SHOVEL   ("Shovel",    GoodCategory.TOOL),
    HAMMER   ("Hammer",    GoodCategory.TOOL),

    // Weapons (Phase 5)
    SWORD    ("Sword",     GoodCategory.WEAPON),
    SHIELD   ("Shield",    GoodCategory.WEAPON);

    private final String label;
    private final GoodCategory category;

    Good(String label, GoodCategory category) {
        this.label = label;
        this.category = category;
    }

    public String label()          { return label; }
    public GoodCategory category()  { return category; }
    public boolean isTool()         { return category == GoodCategory.TOOL; }
    public boolean isFood()         { return category == GoodCategory.FOOD; }
}
