package com.rampart.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.rampart.model.Castle;
import com.rampart.model.Coord;
import com.rampart.model.GameState;
import com.rampart.model.Grid;
import com.rampart.model.Tile;
import com.rampart.model.TileType;

/**
 * THE single source of territory math for the whole game. Flood-fills the exterior
 * of the map from its edges (passing through everything that is not a
 * {@link TileType#WALL}) and marks every non-wall, non-water cell that the exterior
 * cannot reach as <em>enclosed</em>. It then writes the result back onto the
 * {@link com.rampart.model} objects: each {@link Tile}'s enclosed flag, each
 * {@link Castle}'s enclosed flag and territory-cell list, and the
 * {@link GameState}'s {@code territoryFraction}.
 *
 * <p>Consumes and mutates a {@link GameState} (and, through it, its {@link Grid},
 * {@link Tile}s and {@link Castle}s). The UI must never re-implement this — it only
 * reads the flags this class writes.</p>
 */
public final class TerritoryCalculator {

    private TerritoryCalculator() {}

    /** Orthogonal neighbour column deltas (N, E, S, W). */
    private static final int[] DC = { 0, 1, 0, -1 };
    /** Orthogonal neighbour row deltas (N, E, S, W). */
    private static final int[] DR = { -1, 0, 1, 0 };

    /**
     * Recomputes enclosure for the whole board and writes it back onto the model.
     *
     * <p>Reads {@link GameState#gridModel()} and {@link GameState#castleList()};
     * writes {@link Tile#setEnclosed(boolean)}, {@link Grid#clearEnclosedFlags()},
     * {@link Castle#setEnclosed(boolean)}, {@link Castle#setTerritory(List)} and
     * {@link GameState#setTerritoryFraction(double)}.</p>
     *
     * @param state the live {@link GameState} to analyse and update
     */
    public static void recompute(GameState state) {
        Grid grid = state.gridModel();
        int cols = grid.cols();
        int rows = grid.rows();

        grid.clearEnclosedFlags();
        boolean[][] exterior = floodExterior(grid);

        int buildable = 0;
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                TileType t = grid.typeAt(c, r);
                if (t != TileType.WATER) buildable++;
                if (!exterior[c][r] && t != TileType.WALL && t != TileType.WATER) {
                    grid.tile(c, r).setEnclosed(true);
                }
            }
        }

        for (int i = 0; i < state.castleList().size(); i++) {
            Castle castle = state.castleList().get(i);
            Coord p = castle.position();
            if (!castle.alive() || exterior[p.col()][p.row()]) {
                castle.setEnclosed(false);
                castle.setTerritory(new ArrayList<Coord>());
            } else {
                List<Coord> territory = connectedInterior(grid, exterior, p);
                castle.setEnclosed(true);
                castle.setTerritory(territory);
            }
        }

        int enclosedCells = countEnclosedCells(grid);
        state.setTerritoryFraction(buildable > 0 ? (double) enclosedCells / (double) buildable : 0.0);
    }

    /**
     * Counts every cell currently flagged enclosed on the {@link Grid} (used by the
     * {@link ScoreSystem} for per-cell territory scoring).
     *
     * @param grid the {@link Grid} to scan
     * @return the number of enclosed cells
     */
    public static int countEnclosedCells(Grid grid) {
        int n = 0;
        for (int c = 0; c < grid.cols(); c++) {
            for (int r = 0; r < grid.rows(); r++) {
                if (grid.tile(c, r).enclosed()) n++;
            }
        }
        return n;
    }

    /**
     * Flood-fills the exterior region from every border cell, passing through any
     * cell that is not a {@link TileType#WALL}. {@link TileType#RUBBLE} is passable
     * (a blasted wall is a gap), so a broken loop leaks to the exterior.
     *
     * @param grid the {@link Grid} to flood
     * @return a {@code [col][row]} mask, {@code true} where the exterior reaches
     */
    private static boolean[][] floodExterior(Grid grid) {
        int cols = grid.cols();
        int rows = grid.rows();
        boolean[][] ext = new boolean[cols][rows];
        Deque<int[]> stack = new ArrayDeque<int[]>();

        for (int c = 0; c < cols; c++) {
            seed(grid, ext, stack, c, 0);
            seed(grid, ext, stack, c, rows - 1);
        }
        for (int r = 0; r < rows; r++) {
            seed(grid, ext, stack, 0, r);
            seed(grid, ext, stack, cols - 1, r);
        }

        while (!stack.isEmpty()) {
            int[] cell = stack.pop();
            for (int k = 0; k < DC.length; k++) {
                int nc = cell[0] + DC[k];
                int nr = cell[1] + DR[k];
                if (nc < 0 || nc >= cols || nr < 0 || nr >= rows) continue;
                if (ext[nc][nr]) continue;
                if (grid.typeAt(nc, nr) == TileType.WALL) continue;
                ext[nc][nr] = true;
                stack.push(new int[] { nc, nr });
            }
        }
        return ext;
    }

    /** Marks a border cell as exterior and enqueues it, unless it is a wall. */
    private static void seed(Grid grid, boolean[][] ext, Deque<int[]> stack, int c, int r) {
        if (ext[c][r]) return;
        if (grid.typeAt(c, r) == TileType.WALL) return;
        ext[c][r] = true;
        stack.push(new int[] { c, r });
    }

    /**
     * Gathers the connected interior region (non-wall, non-water, non-exterior)
     * containing the castle, as the castle's enclosed territory.
     *
     * @param grid     the {@link Grid}
     * @param exterior the exterior mask from {@link #floodExterior(Grid)}
     * @param start    the castle position
     * @return the list of {@link Coord}s making up this castle's territory
     */
    private static List<Coord> connectedInterior(Grid grid, boolean[][] exterior, Coord start) {
        int cols = grid.cols();
        int rows = grid.rows();
        boolean[][] seen = new boolean[cols][rows];
        List<Coord> out = new ArrayList<Coord>();
        Deque<int[]> stack = new ArrayDeque<int[]>();
        seen[start.col()][start.row()] = true;
        stack.push(new int[] { start.col(), start.row() });

        while (!stack.isEmpty()) {
            int[] cell = stack.pop();
            out.add(new Coord(cell[0], cell[1]));
            for (int k = 0; k < DC.length; k++) {
                int nc = cell[0] + DC[k];
                int nr = cell[1] + DR[k];
                if (nc < 0 || nc >= cols || nr < 0 || nr >= rows) continue;
                if (seen[nc][nr] || exterior[nc][nr]) continue;
                TileType t = grid.typeAt(nc, nr);
                if (t == TileType.WALL || t == TileType.WATER) continue;
                seen[nc][nr] = true;
                stack.push(new int[] { nc, nr });
            }
        }
        return out;
    }
}
