package com.whim.tacticalnexus.domain;

/** A collectible gem that boosts one player stat by {@code amount}. */
public final class StatGem implements Entity {
    private final GemType gem;
    private final int amount;

    public StatGem(GemType gem, int amount) {
        this.gem = gem;
        this.amount = amount;
    }

    public GemType gem() {
        return gem;
    }

    public int amount() {
        return amount;
    }

    @Override
    public EntityType type() {
        return EntityType.GEM;
    }

    @Override
    public boolean blocksMovement() {
        return false;
    }

    @Override
    public char glyph() {
        return '+';
    }
}
