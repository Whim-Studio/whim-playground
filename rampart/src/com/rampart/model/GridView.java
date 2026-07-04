package com.rampart.model;

/**
 * Read-only snapshot of the playfield grid handed to the UI. Concrete class:
 * {@link Grid}.
 */
public interface GridView {
    /** @return number of columns (width in cells) */
    int cols();

    /** @return number of rows (height in cells) */
    int rows();

    /**
     * @param col column index
     * @param row row index
     * @return {@code true} if {@code (col,row)} lies inside the grid
     */
    boolean inBounds(int col, int row);

    /**
     * Returns the read-only tile at the given cell.
     *
     * @param col column index
     * @param row row index
     * @return the tile view
     * @throws IndexOutOfBoundsException if the cell is out of bounds
     */
    TileView tile(int col, int row);

    /**
     * Convenience accessor for a cell's terrain kind.
     *
     * @param col column index
     * @param row row index
     * @return the tile's {@link TileType}
     * @throws IndexOutOfBoundsException if the cell is out of bounds
     */
    TileType typeAt(int col, int row);
}
