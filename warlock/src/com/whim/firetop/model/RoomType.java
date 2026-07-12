package com.whim.firetop.model;

/**
 * The kind of room a dungeon tile represents. Governs what happens when an
 * adventurer enters it.
 */
public enum RoomType {
    /** Where the party begins; harmless. */
    ENTRANCE,
    /** Corridor / empty chamber; nothing happens. */
    EMPTY,
    /** A monster lurks; combat is triggered. */
    MONSTER,
    /** Draw from the treasure deck. */
    TREASURE,
    /** A trap springs; usually a Luck test. */
    TRAP,
    /** Draw from the event deck. */
    EVENT,
    /** A special feature (fountain, shrine) with a fixed beneficial effect. */
    SPECIAL,
    /** Zagor's lair; the final confrontation and win condition. */
    LAIR
}
