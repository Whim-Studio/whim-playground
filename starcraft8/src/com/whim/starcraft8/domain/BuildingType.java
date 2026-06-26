package com.whim.starcraft8.domain;

import java.awt.Color;

/**
 * Embedded building data dictionary. As with {@link UnitType}, constants here MUST NOT
 * reference other enums' constants; prerequisite/production relationships live in
 * {@code data.TechTree}.
 */
public enum BuildingType {
    // --- Terran ---
    COMMAND_CENTER(Race.TERRAN, "Command Center", 400, 0, 400, 10, 4, 3, 600, true, false, false,
        new Color(160, 150, 110)),
    SUPPLY_DEPOT(Race.TERRAN, "Supply Depot", 100, 0, 100, 8, 2, 2, 240, false, true, false,
        new Color(150, 140, 100)),
    REFINERY(Race.TERRAN, "Refinery", 75, 0, 120, 0, 3, 2, 240, false, false, true,
        new Color(120, 160, 90)),
    BARRACKS(Race.TERRAN, "Barracks", 150, 0, 200, 0, 3, 2, 480, false, false, false,
        new Color(170, 80, 70)),
    FACTORY(Race.TERRAN, "Factory", 200, 50, 200, 0, 3, 2, 600, false, false, false,
        new Color(140, 120, 100)),

    // --- Zerg ---
    HATCHERY(Race.ZERG, "Hatchery", 400, 0, 400, 10, 4, 3, 600, true, false, false,
        new Color(140, 70, 150)),
    EXTRACTOR(Race.ZERG, "Extractor", 75, 0, 120, 0, 3, 2, 240, false, false, true,
        new Color(120, 90, 140)),
    SPAWNING_POOL(Race.ZERG, "Spawning Pool", 150, 0, 200, 0, 3, 2, 480, false, false, false,
        new Color(160, 80, 150)),
    HYDRALISK_DEN(Race.ZERG, "Hydralisk Den", 100, 50, 200, 0, 3, 2, 360, false, false, false,
        new Color(150, 70, 120)),

    // --- Protoss ---
    NEXUS(Race.PROTOSS, "Nexus", 400, 0, 400, 10, 4, 3, 600, true, false, false,
        new Color(220, 190, 80)),
    PYLON(Race.PROTOSS, "Pylon", 100, 0, 80, 8, 2, 2, 240, false, true, false,
        new Color(210, 180, 100)),
    ASSIMILATOR(Race.PROTOSS, "Assimilator", 75, 0, 120, 0, 3, 2, 240, false, false, true,
        new Color(180, 170, 90)),
    GATEWAY(Race.PROTOSS, "Gateway", 150, 0, 200, 0, 3, 2, 480, false, false, false,
        new Color(230, 200, 90));

    private final Race race;
    private final String displayName;
    private final int mineralCost;
    private final int gasCost;
    private final int maxHp;
    private final int supplyProvided;
    private final int widthTiles;
    private final int heightTiles;
    private final int buildTicks;
    private final boolean isTownHall;
    private final boolean isSupply;
    private final boolean isGas;
    private final Color baseColor;

    BuildingType(Race race, String displayName, int mineralCost, int gasCost, int maxHp,
                 int supplyProvided, int widthTiles, int heightTiles, int buildTicks,
                 boolean isTownHall, boolean isSupply, boolean isGas, Color baseColor) {
        this.race = race;
        this.displayName = displayName;
        this.mineralCost = mineralCost;
        this.gasCost = gasCost;
        this.maxHp = maxHp;
        this.supplyProvided = supplyProvided;
        this.widthTiles = widthTiles;
        this.heightTiles = heightTiles;
        this.buildTicks = buildTicks;
        this.isTownHall = isTownHall;
        this.isSupply = isSupply;
        this.isGas = isGas;
        this.baseColor = baseColor;
    }

    public Race race() { return race; }
    public String displayName() { return displayName; }
    public int mineralCost() { return mineralCost; }
    public int gasCost() { return gasCost; }
    public int maxHp() { return maxHp; }
    public int supplyProvided() { return supplyProvided; }
    public int widthTiles() { return widthTiles; }
    public int heightTiles() { return heightTiles; }
    public int buildTicks() { return buildTicks; }
    public boolean isTownHall() { return isTownHall; }
    public boolean isSupply() { return isSupply; }
    public boolean isGas() { return isGas; }
    public Color baseColor() { return baseColor; }
}
