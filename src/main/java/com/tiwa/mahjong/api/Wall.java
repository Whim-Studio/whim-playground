package com.tiwa.mahjong.api;

/**
 * The 144-tile wall (18 long x 2 high per side). There is NO dead wall - all tiles are used.
 * Replacement tiles (for flowers, seasons, and kongs) come from the next normal tile clockwise
 * from the last drawn tile, i.e. the same single drawing front (Section 1).
 *
 * <p>Implementation lives in Task 1's {@code wall}/{@code model} package.</p>
 */
public interface Wall {

    /** Draw the next normal tile (clockwise). Throws if {@link #isEmpty()}. */
    Tile draw();

    /**
     * Draw a replacement tile for a flower, season, or kong. Same source as {@link #draw()}.
     * Callers must consult {@link #isLastTile()}/{@link #isEmpty()} first: a kong on the last tile
     * has no replacement and forces a drawn game (Section 4).
     */
    Tile drawReplacement();

    int tilesRemaining();

    boolean isEmpty();

    /** True when the tile just drawn (or about to be drawn) is the final wall tile. */
    boolean isLastTile();
}
