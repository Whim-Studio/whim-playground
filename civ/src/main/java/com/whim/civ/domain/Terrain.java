package com.whim.civ.domain;

/**
 * Square-grid terrain. Base yields are the unworked tile yields BEFORE improvements,
 * government penalties, or specialist bonuses. defenseBonusPct is added to a defender.
 */
public enum Terrain {
    // food, shields, trade, moveCost, defenseBonusPct, canIrrigate, canMine, canFarmRoad
    OCEAN     (1, 0, 2, 1, 0,   false, false, false),
    GRASSLAND (2, 1, 0, 1, 0,   true,  false, true),
    PLAINS    (1, 1, 1, 1, 0,   true,  false, true),
    FOREST    (1, 2, 0, 2, 25,  false, false, true),
    HILLS     (1, 0, 0, 2, 100, true,  true,  true),
    MOUNTAINS (0, 1, 0, 3, 200, false, true,  true),
    DESERT    (0, 1, 0, 1, 0,   true,  true,  true),
    TUNDRA    (1, 0, 0, 1, 0,   false, false, true),
    ARCTIC    (0, 0, 0, 2, 0,   false, false, false),
    SWAMP     (1, 0, 0, 2, 0,   false, false, true),
    JUNGLE    (1, 0, 0, 2, 0,   false, false, true);

    private final int food;
    private final int shields;
    private final int trade;
    private final int moveCost;
    private final int defenseBonusPct;
    private final boolean canIrrigate;
    private final boolean canMine;
    private final boolean canFarmRoad;

    Terrain(int food, int shields, int trade, int moveCost, int defenseBonusPct,
            boolean canIrrigate, boolean canMine, boolean canFarmRoad) {
        this.food = food;
        this.shields = shields;
        this.trade = trade;
        this.moveCost = moveCost;
        this.defenseBonusPct = defenseBonusPct;
        this.canIrrigate = canIrrigate;
        this.canMine = canMine;
        this.canFarmRoad = canFarmRoad;
    }

    public int getFood() { return food; }
    public int getShields() { return shields; }
    public int getTrade() { return trade; }
    public int getMoveCost() { return moveCost; }            // movement points to enter (square grid)
    public int getDefenseBonusPct() { return defenseBonusPct; } // terrain defense bonus, e.g. 100 == +100%
    public boolean canIrrigate() { return canIrrigate; }
    public boolean canMine() { return canMine; }
    public boolean canBuildRoad() { return canFarmRoad; }    // also gates railroad
}
