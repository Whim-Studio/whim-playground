package com.whim.startrek.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * The galaxy grid, backed by a {@code GridCell[][]}. Accessors are bounds-checked.
 * Convenience methods aggregate all fleets and systems across the map.
 */
public class GalaxyMap {

    private final int rows;
    private final int cols;
    private final GridCell[][] cells;

    public GalaxyMap(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.cells = new GridCell[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new GridCell(r, c, MapObjectType.EMPTY);
            }
        }
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    /** Bounds-checked: returns null if out of bounds. */
    public GridCell getCell(int row, int col) {
        if (!inBounds(row, col)) {
            return null;
        }
        return cells[row][col];
    }

    public boolean inBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public List<Fleet> allFleets() {
        List<Fleet> result = new ArrayList<Fleet>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                result.addAll(cells[r][c].getFleets());
            }
        }
        return result;
    }

    public List<StarSystem> allSystems() {
        List<StarSystem> result = new ArrayList<StarSystem>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                StarSystem s = cells[r][c].getSystem();
                if (s != null) {
                    result.add(s);
                }
            }
        }
        return result;
    }
}
