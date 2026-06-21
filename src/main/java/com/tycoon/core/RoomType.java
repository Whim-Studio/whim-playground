package com.tycoon.core;

/**
 * Functional room categories. Each has a per-tile floor build cost.
 */
public enum RoomType {
    DEVELOPMENT(20),
    RESEARCH(25),
    QA(18),
    LOUNGE(12),
    SERVER(30),
    MARKETING(22);

    private final int floorCostPerTile;

    RoomType(int floorCostPerTile) {
        this.floorCostPerTile = floorCostPerTile;
    }

    public int floorCostPerTile() {
        return floorCostPerTile;
    }
}
