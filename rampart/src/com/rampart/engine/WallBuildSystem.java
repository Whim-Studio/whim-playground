package com.rampart.engine;

import java.util.List;
import java.util.Random;

import com.rampart.model.Coord;
import com.rampart.model.GameState;
import com.rampart.model.Grid;
import com.rampart.model.Phase;
import com.rampart.model.Rules;
import com.rampart.model.TileType;
import com.rampart.model.WallPiece;
import com.rampart.model.WallShape;

/**
 * Deals and places the Tetris-style wall pieces of the REPAIR phase. Consumes and
 * mutates a {@link GameState}: it owns the current {@link WallPiece}
 * ({@link GameState#setCurrentPiece(WallPiece)}), the queued-piece preview list
 * ({@link GameState#queuedPieceList()}), and — on a valid placement — flips the
 * covered {@link Grid} cells to {@link TileType#WALL} before re-running the
 * {@link TerritoryCalculator}.
 */
public final class WallBuildSystem {

    /**
     * Begins a REPAIR phase: clears any prior pieces and deals a fresh current piece
     * plus a {@link Rules#REPAIR_QUEUE_SIZE}-deep preview queue of random shapes.
     *
     * @param state the live {@link GameState}
     * @param rng   the deterministic {@link Random} used to pick shapes
     */
    public void startRepair(GameState state, Random rng) {
        state.queuedPieceList().clear();
        state.setCurrentPiece(randomPiece(state, rng));
        for (int i = 0; i < Rules.REPAIR_QUEUE_SIZE; i++) {
            state.queuedPieceList().add(randomPiece(state, rng));
        }
    }

    /**
     * Rotates the current wall piece 90&deg; clockwise via {@link WallPiece#rotate()}.
     * No-op when no piece is active.
     *
     * @param state the live {@link GameState}
     */
    public void rotatePiece(GameState state) {
        WallPiece piece = state.currentPieceModel();
        if (piece != null) piece.rotate();
    }

    /**
     * Attempts to drop the current {@link WallPiece} with its anchor at
     * {@code (col,row)}. Valid only during {@link Phase#REPAIR} when every projected
     * cell ({@link WallPiece#absoluteCells()}) is in bounds and currently
     * {@link TileType#LAND} or {@link TileType#RUBBLE} (so pieces reseal gaps and
     * extend onto open land but never overlap water, walls, cannons, or castles). On
     * success it writes {@link TileType#WALL} into each cell, deals the next piece,
     * and recomputes territory.
     *
     * @param state the live {@link GameState}
     * @param col   anchor column
     * @param row   anchor row
     * @param rng   the deterministic {@link Random} used to deal the replacement
     * @return {@code true} if the piece was placed
     */
    public boolean placePiece(GameState state, int col, int row, Random rng) {
        if (state.phase() != Phase.REPAIR) return false;
        WallPiece piece = state.currentPieceModel();
        if (piece == null) return false;

        Grid grid = state.gridModel();
        piece.setAnchor(new Coord(col, row));
        List<Coord> cells = piece.absoluteCells();
        for (int i = 0; i < cells.size(); i++) {
            Coord cell = cells.get(i);
            if (!grid.inBounds(cell.col(), cell.row())) return false;
            TileType t = grid.typeAt(cell.col(), cell.row());
            if (t != TileType.LAND && t != TileType.RUBBLE) return false;
        }

        for (int i = 0; i < cells.size(); i++) {
            Coord cell = cells.get(i);
            grid.setType(cell.col(), cell.row(), TileType.WALL);
        }
        dealNext(state, rng);
        TerritoryCalculator.recompute(state);
        return true;
    }

    /** Advances the current piece from the front of the queue and refills the queue. */
    private void dealNext(GameState state, Random rng) {
        if (!state.queuedPieceList().isEmpty()) {
            state.setCurrentPiece(state.queuedPieceList().remove(0));
        } else {
            state.setCurrentPiece(randomPiece(state, rng));
        }
        state.queuedPieceList().add(randomPiece(state, rng));
    }

    /** Builds a random-shape piece anchored at the grid centre. */
    private WallPiece randomPiece(GameState state, Random rng) {
        WallShape[] shapes = WallShape.values();
        WallShape shape = shapes[rng.nextInt(shapes.length)];
        Coord anchor = new Coord(Rules.GRID_COLS / 2, Rules.GRID_ROWS / 2);
        return new WallPiece(shape, anchor);
    }
}
