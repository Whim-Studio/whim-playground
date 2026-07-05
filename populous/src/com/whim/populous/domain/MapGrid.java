package com.whim.populous.domain;

import java.util.ArrayDeque;
import java.util.Deque;

import com.whim.populous.api.Views.MapView;
import com.whim.populous.api.Views.TileView;

/**
 * The 64x64 landscape. Implements the read-only {@link MapView} and provides the
 * terraforming mutation helpers the engine calls (raise/lower brush, direct set,
 * flat-plateau measurement, transient ageing).
 *
 * <p><b>Sea level</b> is a single shared reference (default 0). The FLOOD power
 * raises it temporarily; because {@link Tile#terrain()} reads it live, raising
 * sea level instantly re-derives every tile's terrain — low ground becomes water
 * and drowns whatever stands on it.
 *
 * <p><b>Terraforming</b> mimics Populous: a click nudges a tile up/down by one
 * step and smooths its neighbours so slopes stay walkable (max 1 step between
 * adjacent tiles). Repeated clicks over an area naturally flatten it into a
 * buildable plateau. Land is only buildable when {@link #flatAreaAt} reports a
 * contiguous run of equal-elevation, dry tiles.
 */
public final class MapGrid implements MapView {

    public static final int DEFAULT_COLS = 64;
    public static final int DEFAULT_ROWS = 64;
    public static final int DEFAULT_SEA_LEVEL = 0;

    private final int cols;
    private final int rows;
    private final Tile[] tiles;   // row-major: index = row*cols + col
    private int seaLevel;
    private final int baseSeaLevel;

    public MapGrid(int cols, int rows, int seaLevel) {
        this.cols = cols;
        this.rows = rows;
        this.seaLevel = seaLevel;
        this.baseSeaLevel = seaLevel;
        this.tiles = new Tile[cols * rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                tiles[r * cols + c] = new Tile(this, c, r, seaLevel - 2);
            }
        }
    }

    public MapGrid() {
        this(DEFAULT_COLS, DEFAULT_ROWS, DEFAULT_SEA_LEVEL);
    }

    // ---- MapView ------------------------------------------------------------

    @Override public int cols() { return cols; }
    @Override public int rows() { return rows; }
    @Override public int seaLevel() { return seaLevel; }

    @Override
    public TileView tileAt(int col, int row) {
        return tile(col, row);
    }

    // ---- concrete accessors -------------------------------------------------

    public boolean inBounds(int col, int row) {
        return col >= 0 && col < cols && row >= 0 && row < rows;
    }

    /** Concrete tile accessor for the engine (null if out of bounds). */
    public Tile tile(int col, int row) {
        if (!inBounds(col, row)) {
            return null;
        }
        return tiles[row * cols + col];
    }

    public int baseSeaLevel() { return baseSeaLevel; }

    /** Set the live sea level (FLOOD raises it, then the engine restores it). */
    public void setSeaLevel(int level) { this.seaLevel = level; }
    public void raiseSeaLevel(int steps) { this.seaLevel += steps; }

    // ---- terraforming -------------------------------------------------------

    /** Directly set a tile's elevation (generation / precise edits). */
    public void setElevation(int col, int row, int elevation) {
        Tile t = tile(col, row);
        if (t != null) {
            t.setElevation(elevation);
        }
    }

    /** Raise the tile by one step and smooth neighbours upward. */
    public void raise(int col, int row) {
        adjust(col, row, +1);
    }

    /** Lower the tile by one step and smooth neighbours downward. */
    public void lower(int col, int row) {
        adjust(col, row, -1);
    }

    /**
     * Raise/lower a square brush of the given radius by one step, smoothing the
     * ring around it. radius 0 == single tile. This is the classic drag-brush.
     */
    public void raiseBrush(int col, int row, int radius) {
        brush(col, row, radius, +1);
    }

    public void lowerBrush(int col, int row, int radius) {
        brush(col, row, radius, -1);
    }

    private void brush(int col, int row, int radius, int dir) {
        for (int dr = -radius; dr <= radius; dr++) {
            for (int dc = -radius; dc <= radius; dc++) {
                Tile t = tile(col + dc, row + dr);
                if (t != null) {
                    t.addElevation(dir);
                }
            }
        }
        smoothAround(col, row, radius + 1);
    }

    private void adjust(int col, int row, int dir) {
        Tile t = tile(col, row);
        if (t == null) {
            return;
        }
        t.addElevation(dir);
        smoothAround(col, row, 1);
    }

    /**
     * Enforce the "no cliff steeper than one step" rule around a changed area so
     * terraforming produces gentle slopes and, with repeated edits, flat tops.
     */
    private void smoothAround(int col, int row, int reach) {
        for (int dr = -reach; dr <= reach; dr++) {
            for (int dc = -reach; dc <= reach; dc++) {
                Tile t = tile(col + dc, row + dr);
                if (t == null) {
                    continue;
                }
                clampToNeighbours(col + dc, row + dr, t);
            }
        }
    }

    private void clampToNeighbours(int c, int r, Tile t) {
        // Pull each 4-neighbour to within one step of this tile.
        pull(c + 1, r, t.elevation());
        pull(c - 1, r, t.elevation());
        pull(c, r + 1, t.elevation());
        pull(c, r - 1, t.elevation());
    }

    private void pull(int c, int r, int centerElev) {
        Tile n = tile(c, r);
        if (n == null) {
            return;
        }
        if (n.elevation() > centerElev + 1) {
            n.setElevation(centerElev + 1);
        } else if (n.elevation() < centerElev - 1) {
            n.setElevation(centerElev - 1);
        }
    }

    // ---- plateau measurement ------------------------------------------------

    /**
     * Size of the contiguous flat plateau containing (col,row): the number of
     * orthogonally-connected tiles that share this tile's exact elevation and
     * are dry, buildable land (at or above sea level). This is the number the
     * settlement tiering feeds on. Returns 0 for water/out-of-bounds.
     */
    public int flatAreaAt(int col, int row) {
        Tile start = tile(col, row);
        if (start == null || !TerrainRules.isBuildable(start.elevation(), seaLevel)) {
            return 0;
        }
        int target = start.elevation();
        boolean[] seen = new boolean[cols * rows];
        Deque<int[]> stack = new ArrayDeque<int[]>();
        stack.push(new int[] { col, row });
        seen[row * cols + col] = true;
        int count = 0;
        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            int c = p[0];
            int r = p[1];
            count++;
            pushIfFlat(stack, seen, c + 1, r, target);
            pushIfFlat(stack, seen, c - 1, r, target);
            pushIfFlat(stack, seen, c, r + 1, target);
            pushIfFlat(stack, seen, c, r - 1, target);
        }
        return count;
    }

    private void pushIfFlat(Deque<int[]> stack, boolean[] seen, int c, int r, int target) {
        if (!inBounds(c, r)) {
            return;
        }
        int idx = r * cols + c;
        if (seen[idx]) {
            return;
        }
        Tile t = tiles[idx];
        if (t.elevation() == target && TerrainRules.isBuildable(target, seaLevel) && !t.hasTransient()) {
            seen[idx] = true;
            stack.push(new int[] { c, r });
        }
    }

    /** Age transient overrides (SWAMP/LAVA) across the whole map by one tick. */
    public void ageTransients() {
        for (int i = 0; i < tiles.length; i++) {
            tiles[i].ageTransient();
        }
    }
}
