package com.whim.xcom.model;

/**
 * The five difficulty tiers of 1994 UFO: Enemy Unknown. The tier feeds alien
 * stat/quantity scaling and scoring; kept in the pure model so the ruleset can
 * read it without any UI dependency.
 */
public enum Difficulty {
    BEGINNER(0),
    EXPERIENCED(1),
    VETERAN(2),
    GENIUS(3),
    SUPERHUMAN(4);

    private final int level;

    Difficulty(int level) {
        this.level = level;
    }

    /** @return 0 (Beginner) .. 4 (Superhuman). */
    public int level() {
        return level;
    }
}
