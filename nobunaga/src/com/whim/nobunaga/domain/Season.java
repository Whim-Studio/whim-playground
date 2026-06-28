package com.whim.nobunaga.domain;

/**
 * The four seasons of the Sengoku year. Each game turn is one season; the
 * Fall season triggers the rice harvest. {@link #next()} is cyclic so that
 * WINTER rolls over to SPRING (and {@link GameState#advanceClock()} bumps the
 * year on that rollover).
 */
public enum Season {
    SPRING("Spring"),
    SUMMER("Summer"),
    FALL("Fall"),
    WINTER("Winter");

    private final String label;

    Season(String label) {
        this.label = label;
    }

    /** Cyclic successor: WINTER -> SPRING. */
    public Season next() {
        switch (this) {
            case SPRING: return SUMMER;
            case SUMMER: return FALL;
            case FALL:   return WINTER;
            default:     return SPRING;
        }
    }

    /** Display name, e.g. "Spring". */
    public String label() {
        return label;
    }
}
