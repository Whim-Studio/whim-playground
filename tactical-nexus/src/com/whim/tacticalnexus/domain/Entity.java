package com.whim.tacticalnexus.domain;

/**
 * A data-only occupant of a grid cell. Entities carry no interaction rules
 * (those live in the engine, Task 2) and never mutate themselves.
 */
public interface Entity {
    EntityType type();

    /** True if the player cannot step onto this cell until it is resolved. */
    boolean blocksMovement();

    /** Debug/text fallback glyph, e.g. '#','D','K','E','+','&gt;','&lt;'. */
    char glyph();
}
