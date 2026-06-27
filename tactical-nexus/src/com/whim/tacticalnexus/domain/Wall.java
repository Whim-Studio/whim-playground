package com.whim.tacticalnexus.domain;

/** An impassable wall. */
public final class Wall implements Entity {
    public Wall() {
    }

    @Override
    public EntityType type() {
        return EntityType.WALL;
    }

    @Override
    public boolean blocksMovement() {
        return true;
    }

    @Override
    public char glyph() {
        return '#';
    }
}
