package com.rampart.model;

/**
 * A single mutable grid cell: its terrain {@link TileType} plus an "enclosed" flag
 * the engine's territory pass sets. Pure state holder — implements {@link TileView}
 * so it can be handed to the UI as a read-only cell.
 */
public class Tile implements TileView {
    private final int col;
    private final int row;
    private TileType type;
    private boolean enclosed;

    /**
     * Creates a tile at a fixed position.
     *
     * @param col  column (x)
     * @param row  row (y)
     * @param type initial terrain kind (must be non-null)
     */
    public Tile(int col, int row, TileType type) {
        if (type == null) throw new IllegalArgumentException("type must not be null");
        this.col = col;
        this.row = row;
        this.type = type;
    }

    @Override public int col() { return col; }
    @Override public int row() { return row; }
    @Override public TileType type() { return type; }
    @Override public boolean enclosed() { return enclosed; }

    /** @return this tile's position as a {@link Coord} */
    public Coord coord() { return new Coord(col, row); }

    /**
     * Sets the terrain kind (engine only).
     *
     * @param type the new terrain kind (must be non-null)
     */
    public void setType(TileType type) {
        if (type == null) throw new IllegalArgumentException("type must not be null");
        this.type = type;
    }

    /**
     * Sets the enclosed flag (engine territory pass only).
     *
     * @param enclosed whether this cell is inside a sealed wall loop
     */
    public void setEnclosed(boolean enclosed) {
        this.enclosed = enclosed;
    }

    @Override
    public String toString() {
        return "Tile(" + col + "," + row + "," + type + (enclosed ? ",enclosed" : "") + ")";
    }
}
