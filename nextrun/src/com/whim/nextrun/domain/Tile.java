package com.whim.nextrun.domain;

/**
 * One cell of the world grid. A tile knows whether it has been seen (fog of
 * war), what it contains, and any payload (enemy / loot amount) attached.
 */
public final class Tile {
    public EntityType type = EntityType.EMPTY;
    public boolean discovered = false;
    public int payload = 0;       // gold amount, material amount, etc.
    public Enemy enemy = null;    // present when type == ENEMY
    public String structureName = null; // present when type == STRUCTURE

    public boolean isWalkable() {
        // Enemies block the tile until resolved; everything else can be entered.
        return type != EntityType.ENEMY;
    }
}
