package com.tiwa.mahjong.api;

/**
 * The seven tile categories of the full 144-tile set defined in Tiwa's Mah Jong Rulebook (Section 1).
 *
 * <p>Suited tiles ({@link #DOTS}, {@link #BAMBOO}, {@link #CHARACTERS}) carry ranks 1-9 (x4 each).
 * Honors ({@link #WIND}, {@link #DRAGON}) are scored at the honor rate. Bonus tiles
 * ({@link #FLOWER}, {@link #SEASON}) reveal immediately, are worth 4 points each, never form melds,
 * and trigger an immediate $2 payment in money games.</p>
 */
public enum Suit {
    DOTS(true, false, false),
    BAMBOO(true, false, false),
    CHARACTERS(true, false, false),
    WIND(false, true, false),
    DRAGON(false, true, false),
    FLOWER(false, false, true),
    SEASON(false, false, true);

    private final boolean suited;
    private final boolean honor;
    private final boolean bonus;

    Suit(boolean suited, boolean honor, boolean bonus) {
        this.suited = suited;
        this.honor = honor;
        this.bonus = bonus;
    }

    /** True for Dots, Bamboo, Characters (rank 1-9). */
    public boolean isSuited() {
        return suited;
    }

    /** True for Winds and Dragons (scored at the honor rate). */
    public boolean isHonor() {
        return honor;
    }

    /** True for Flowers and Seasons (4 points, never melds, immediate $2 payment). */
    public boolean isBonus() {
        return bonus;
    }
}
