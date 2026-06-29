package com.whim.nextrun.domain;

/** What occupies (or marks) a tile on the world grid. */
public enum EntityType {
    EMPTY,
    RESOURCE,   // gatherable materials
    GOLD_PILE,  // lootable gold
    RUIN,       // explore for loot / lore
    ENEMY,      // must be resolved: fight / bribe / sneak
    STRUCTURE,  // a settlement building the player raised
    PORTAL      // the way out — reaching it with the run intact is one victory
}
