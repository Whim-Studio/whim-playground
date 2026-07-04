package com.rampart.model;

import java.util.List;

/**
 * Read-only snapshot of the wall piece currently being placed in the REPAIR phase.
 * Concrete class: {@link WallPiece}. The UI reads {@link #absoluteCells()} to draw
 * the ghost/preview at the anchor and calls the engine to rotate or drop it.
 */
public interface WallPieceView {
    /** @return the polyomino shape of this piece */
    WallShape shape();

    /** @return the current rotation index in {@code [0, 3]} */
    int rotation();

    /** @return the piece's anchor cell (the origin its offsets are relative to) */
    Coord anchor();

    /**
     * @return an unmodifiable list of the piece's cell offsets for the current
     *         rotation, relative to the anchor
     */
    List<Coord> offsets();

    /**
     * @return an unmodifiable list of the piece's occupied cells in absolute grid
     *         coordinates (anchor + current-rotation offsets)
     */
    List<Coord> absoluteCells();

    /** @return the number of cells this piece occupies */
    int size();
}
