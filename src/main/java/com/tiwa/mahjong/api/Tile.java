package com.tiwa.mahjong.api;

/**
 * A single immutable tile. Implementations live in Task 1's {@code model} package.
 *
 * <p>Rank conventions:
 * <ul>
 *   <li>Suited (Dots/Bamboo/Characters): 1-9.</li>
 *   <li>Wind: 1-4 (East, South, West, North) - see {@link Wind#rank()}.</li>
 *   <li>Dragon: 1-3 (Red, Green, White) - see {@link Dragon#rank()}.</li>
 *   <li>Flower / Season: 1-4 (the four distinct bonus tiles of that category).</li>
 * </ul>
 *
 * <p>Equality is by ({@link #getSuit()}, {@link #getRank()}): the four physical copies of a tile
 * are equal to one another. Implementations MUST override {@code equals}/{@code hashCode} accordingly.</p>
 */
public interface Tile {

    Suit getSuit();

    /** 1-based rank within the suit (see class javadoc for the per-suit mapping). */
    int getRank();

    /** True for Winds and Dragons. */
    boolean isHonor();

    /** True for Dots, Bamboo, Characters. */
    boolean isSuited();

    /** True for Flowers and Seasons. */
    boolean isBonus();

    /** True for a suited tile of rank 1 or 9 (relevant to terminal chows and 13 Orphans). */
    boolean isTerminal();
}
