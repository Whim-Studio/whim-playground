package com.whim.nextrun.engine;

/** Every distinct action a player can take. Each has a turn cost (see {@link ActionCosts}). */
public enum ActionType {
    MOVE,
    GATHER,
    LOOT,
    EXPLORE,
    FIGHT,
    BRIBE,
    SNEAK,
    CRAFT_WEAPON,
    CRAFT_ARMOR,
    BUILD,
    REST
}
