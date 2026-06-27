package com.whim.tacticalnexus.domain;

/** A collectible colored key. Does not block movement. */
public final class KeyItem implements Entity {
    private final KeyColor color;

    public KeyItem(KeyColor color) {
        this.color = color;
    }

    public KeyColor color() {
        return color;
    }

    @Override
    public EntityType type() {
        return EntityType.KEY;
    }

    @Override
    public boolean blocksMovement() {
        return false;
    }

    @Override
    public char glyph() {
        return 'K';
    }
}
