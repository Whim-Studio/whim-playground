package com.whim.necromunda.model.board;

/**
 * The kinds of terrain a {@link Tile} can hold. Each carries whether it blocks
 * movement and whether it blocks line-of-sight, plus a default cover value the
 * board author can override per tile. Names are functional (no trademarked lore).
 */
public enum TerrainType {
    /** Empty floor. */
    OPEN("Open", false, false, Cover.NONE),
    /** A solid wall — blocks movement and sight. */
    WALL("Wall", true, true, Cover.HARD),
    /** Elevated walkway grating — passable, offers partial cover. */
    GANTRY("Gantry", false, false, Cover.PARTIAL),
    /** A ladder connecting levels — passable at a movement cost. */
    LADDER("Ladder", false, false, Cover.NONE),
    /** A hazardous pit — blocks movement (a fighter falls in). */
    PIT("Pit", true, false, Cover.NONE),
    /** Loose rubble — passable, offers partial cover, may slow movement. */
    RUBBLE("Rubble", false, false, Cover.PARTIAL),
    /** A low barricade — passable, offers hard cover to those behind it. */
    BARRICADE("Barricade", false, false, Cover.HARD),
    /** A raised platform surface — passable, elevated. */
    PLATFORM("Platform", false, false, Cover.NONE);

    private final String label;
    private final boolean blocksMovement;
    private final boolean blocksSight;
    private final Cover defaultCover;

    TerrainType(String label, boolean blocksMovement, boolean blocksSight, Cover defaultCover) {
        this.label = label;
        this.blocksMovement = blocksMovement;
        this.blocksSight = blocksSight;
        this.defaultCover = defaultCover;
    }

    public String label() { return label; }
    public boolean blocksMovement() { return blocksMovement; }
    public boolean blocksSight() { return blocksSight; }
    public Cover defaultCover() { return defaultCover; }
}
