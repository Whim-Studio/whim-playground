package com.rampart.model;

/**
 * The rectangular playfield: a dense 2D array of {@link Tile}s addressed by
 * {@code (col, row)}. Provides bounds-checked accessors and mutators. All tiles
 * are created up front (defaulting to {@link TileType#WATER}); the level factory
 * and the engine set their real types.
 *
 * <p>This class holds state only — no flood-fill, no adjacency logic. Those live in
 * the engine (Task 2).
 */
public class Grid implements GridView {
    private final int cols;
    private final int rows;
    private final Tile[][] tiles; // [col][row]

    /**
     * Creates a grid of the given size with every cell initialised to
     * {@link TileType#WATER}.
     *
     * @param cols number of columns (must be &gt; 0)
     * @param rows number of rows (must be &gt; 0)
     */
    public Grid(int cols, int rows) {
        if (cols <= 0 || rows <= 0) {
            throw new IllegalArgumentException("grid must be positive-sized: " + cols + "x" + rows);
        }
        this.cols = cols;
        this.rows = rows;
        this.tiles = new Tile[cols][rows];
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                tiles[c][r] = new Tile(c, r, TileType.WATER);
            }
        }
    }

    @Override public int cols() { return cols; }
    @Override public int rows() { return rows; }

    @Override
    public boolean inBounds(int col, int row) {
        return col >= 0 && col < cols && row >= 0 && row < rows;
    }

    /**
     * @param c a coordinate
     * @return {@code true} if the coordinate lies inside the grid
     */
    public boolean inBounds(Coord c) {
        return c != null && inBounds(c.col(), c.row());
    }

    @Override
    public Tile tile(int col, int row) {
        requireInBounds(col, row);
        return tiles[col][row];
    }

    /**
     * Returns the concrete tile at a coordinate (engine convenience).
     *
     * @param c the coordinate
     * @return the tile
     * @throws IndexOutOfBoundsException if out of bounds
     */
    public Tile tile(Coord c) {
        return tile(c.col(), c.row());
    }

    @Override
    public TileType typeAt(int col, int row) {
        return tile(col, row).type();
    }

    /**
     * Sets the terrain kind at a cell (engine only).
     *
     * @param col  column
     * @param row  row
     * @param type new terrain kind
     * @throws IndexOutOfBoundsException if out of bounds
     */
    public void setType(int col, int row, TileType type) {
        tile(col, row).setType(type);
    }

    /** Clears the enclosed flag on every tile (engine, before a territory pass). */
    public void clearEnclosedFlags() {
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                tiles[c][r].setEnclosed(false);
            }
        }
    }

    private void requireInBounds(int col, int row) {
        if (!inBounds(col, row)) {
            throw new IndexOutOfBoundsException("cell out of bounds: (" + col + "," + row + ")");
        }
    }
}
