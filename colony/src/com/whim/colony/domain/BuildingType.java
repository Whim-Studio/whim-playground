package com.whim.colony.domain;

/**
 * The kind of a {@link Building}. The {@code blocksMovement} flag lets terrain
 * that is otherwise walkable become impassable once a solid structure (a WALL)
 * is placed on it.
 */
public enum BuildingType {
    STOCKPILE(false),
    BED(false),
    FARM(false),
    WALL(true);

    private final boolean blocksMovement;

    BuildingType(boolean blocksMovement) {
        this.blocksMovement = blocksMovement;
    }

    /** @return true if this structure prevents colonists from entering its tile. */
    public boolean blocksMovement() {
        return blocksMovement;
    }
}
