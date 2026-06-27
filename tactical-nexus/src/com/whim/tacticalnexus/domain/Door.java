package com.whim.tacticalnexus.domain;

/** A colored door; opened by spending one matching {@link KeyItem}. */
public final class Door implements Entity {
    private final KeyColor color;

    public Door(KeyColor color) {
        this.color = color;
    }

    public KeyColor color() {
        return color;
    }

    @Override
    public EntityType type() {
        return EntityType.DOOR;
    }

    @Override
    public boolean blocksMovement() {
        return true;
    }

    @Override
    public char glyph() {
        return 'D';
    }
}
