package com.tycoon.core;
// LOCAL-ONLY VERIFICATION STUB (Task 1 owns this) — never committed by Task 2.
public enum RoomType {
    DEVELOPMENT(10), RESEARCH(12), QA(8), LOUNGE(5), SERVER(15), MARKETING(9);
    private final int floorCost;
    RoomType(int c) { this.floorCost = c; }
    public int floorCostPerTile() { return floorCost; }
}
