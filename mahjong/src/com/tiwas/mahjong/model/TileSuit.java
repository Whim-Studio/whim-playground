package com.tiwas.mahjong.model;

/**
 * The seven categories of tile in the set. DOTS, BAMBOO and CHARACTERS are the
 * three numbered "suits" (ranks 1-9). WIND and DRAGON are the honour tiles.
 * FLOWER and SEASON are the bonus tiles that never form melds.
 */
public enum TileSuit {
    DOTS,
    BAMBOO,
    CHARACTERS,
    WIND,
    DRAGON,
    FLOWER,
    SEASON;

    /** True for the three numbered suits that can form chows / runs. */
    public boolean isSuited() {
        return this == DOTS || this == BAMBOO || this == CHARACTERS;
    }

    /** True for winds and dragons (honour tiles). */
    public boolean isHonour() {
        return this == WIND || this == DRAGON;
    }

    /** True for the bonus tiles (flowers and seasons). */
    public boolean isBonus() {
        return this == FLOWER || this == SEASON;
    }
}
