package com.finesse.core;

/**
 * An immutable board coordinate.
 *
 * <p>A {@code Position} is a simple value object holding a {@code file} (column)
 * and a {@code rank} (row). Coordinates are intentionally generic integers so
 * that boards of arbitrary (non-8x8) dimensions can be represented. By
 * convention {@code file} and {@code rank} are zero-based, with {@code (0, 0)}
 * being the lower-left corner, but the engine imposes no upper bound here &mdash;
 * bounds checking is the responsibility of {@link Board#isInBounds(Position)}.
 */
public final class Position {

    private final int file;
    private final int rank;

    /**
     * Creates a position.
     *
     * @param file the column (zero-based by convention)
     * @param rank the row (zero-based by convention)
     */
    public Position(int file, int rank) {
        this.file = file;
        this.rank = rank;
    }

    /**
     * Factory equivalent to the constructor, provided for readability at call sites.
     *
     * @param file the column
     * @param rank the row
     * @return a new {@code Position}
     */
    public static Position of(int file, int rank) {
        return new Position(file, rank);
    }

    /** @return the column (zero-based by convention). */
    public int getFile() {
        return file;
    }

    /** @return the row (zero-based by convention). */
    public int getRank() {
        return rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Position)) {
            return false;
        }
        Position other = (Position) o;
        return file == other.file && rank == other.rank;
    }

    @Override
    public int hashCode() {
        return 31 * file + rank;
    }

    @Override
    public String toString() {
        return "Position(" + file + ", " + rank + ")";
    }
}
