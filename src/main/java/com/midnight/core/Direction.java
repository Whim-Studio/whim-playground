package com.midnight.core;

/**
 * The eight points of the compass, listed clockwise starting from {@link #NORTH}.
 *
 * <p>The world grid places y=0 at the NORTH edge and increases SOUTHward, so a
 * step {@code NORTH} decreases y and a step {@code SOUTH} increases it; {@code
 * EAST} increases x. Use {@link #dx()}/{@link #dy()} for the unit step.
 */
public enum Direction {
    NORTH,
    NORTHEAST,
    EAST,
    SOUTHEAST,
    SOUTH,
    SOUTHWEST,
    WEST,
    NORTHWEST;

    /** Horizontal step: {@code -1}, {@code 0}, or {@code +1} (EAST = {@code +1}). */
    public int dx() {
        switch (this) {
            case EAST:
            case NORTHEAST:
            case SOUTHEAST:
                return +1;
            case WEST:
            case NORTHWEST:
            case SOUTHWEST:
                return -1;
            default:
                return 0;
        }
    }

    /** Vertical step: {@code -1}, {@code 0}, or {@code +1} (NORTH = {@code -1}, SOUTH = {@code +1}). */
    public int dy() {
        switch (this) {
            case NORTH:
            case NORTHEAST:
            case NORTHWEST:
                return -1;
            case SOUTH:
            case SOUTHEAST:
            case SOUTHWEST:
                return +1;
            default:
                return 0;
        }
    }

    /** The directly opposite heading (a 180-degree turn). */
    public Direction opposite() {
        return values()[(ordinal() + 4) % 8];
    }

    /** Turn 45 degrees to the right (clockwise). */
    public Direction clockwise() {
        return values()[(ordinal() + 1) % 8];
    }

    /** Turn 45 degrees to the left (anticlockwise). */
    public Direction anticlockwise() {
        return values()[(ordinal() + 7) % 8];
    }
}
