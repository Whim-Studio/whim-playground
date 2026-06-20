package com.tiwa.mahjong.api;

/**
 * The four winds. Seating order (highest dice roll = East) and round-wind rotation are clockwise:
 * East -&gt; South -&gt; West -&gt; North. Play, however, proceeds counter-clockwise (Section 2-3).
 *
 * <p>For a {@link Tile} of suit {@link Suit#WIND} the rank maps to {@code ordinal() + 1}
 * (East=1, South=2, West=3, North=4).</p>
 */
public enum Wind {
    EAST, SOUTH, WEST, NORTH;

    /** Next wind clockwise (used for round-wind rotation and seat assignment). */
    public Wind clockwise() {
        return values()[(ordinal() + 1) % 4];
    }

    /** 1-based rank used by {@link Tile#getRank()} for wind tiles. */
    public int rank() {
        return ordinal() + 1;
    }
}
