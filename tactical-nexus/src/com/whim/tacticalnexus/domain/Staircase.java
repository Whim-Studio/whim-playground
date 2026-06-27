package com.whim.tacticalnexus.domain;

/** A staircase linking floors. Does not block movement. */
public final class Staircase implements Entity {
    private final StairDirection direction;

    public Staircase(StairDirection dir) {
        this.direction = dir;
    }

    public StairDirection direction() {
        return direction;
    }

    @Override
    public EntityType type() {
        return EntityType.STAIR;
    }

    @Override
    public boolean blocksMovement() {
        return false;
    }

    @Override
    public char glyph() {
        return direction == StairDirection.UP ? '>' : '<';
    }
}
