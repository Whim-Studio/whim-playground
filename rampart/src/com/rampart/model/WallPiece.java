package com.rampart.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Tetris-style polyomino wall piece dealt during the REPAIR phase. Holds an
 * anchor coordinate, a {@link WallShape}, and the four precomputed rotation states
 * (each a list of cell offsets relative to the anchor). {@link #rotate()} only
 * advances the rotation index and {@link #absoluteCells()} projects offsets onto
 * the grid — there is NO placement validation, overlap checking, or grid mutation
 * here. That is the engine's job (Task 2).
 *
 * <p>Rotation states are derived once at construction by rotating the shape's base
 * layout 90&deg; clockwise and normalising it back into the non-negative quadrant,
 * so all four rotations share the same anchor semantics.
 */
public class WallPiece implements WallPieceView {
    private final WallShape shape;
    /** rotations[r] = immutable offset list for rotation index r (0..3). */
    private final List<List<Coord>> rotations;
    private Coord anchor;
    private int rotation;

    /**
     * Creates a piece of the given shape anchored at a cell, at rotation 0.
     *
     * @param shape  the polyomino shape (must be non-null)
     * @param anchor the anchor cell (must be non-null)
     */
    public WallPiece(WallShape shape, Coord anchor) {
        if (shape == null) throw new IllegalArgumentException("shape must not be null");
        if (anchor == null) throw new IllegalArgumentException("anchor must not be null");
        this.shape = shape;
        this.anchor = anchor;
        this.rotations = buildRotations(baseOffsets(shape));
    }

    @Override public WallShape shape() { return shape; }
    @Override public int rotation() { return rotation; }
    @Override public Coord anchor() { return anchor; }

    @Override
    public List<Coord> offsets() {
        return rotations.get(rotation);
    }

    @Override
    public List<Coord> absoluteCells() {
        List<Coord> base = rotations.get(rotation);
        List<Coord> out = new ArrayList<Coord>(base.size());
        for (int i = 0; i < base.size(); i++) {
            Coord o = base.get(i);
            out.add(new Coord(anchor.col() + o.col(), anchor.row() + o.row()));
        }
        return Collections.unmodifiableList(out);
    }

    @Override public int size() { return rotations.get(rotation).size(); }

    /**
     * Advances to the next clockwise rotation state (wraps 3 &rarr; 0). Data only.
     */
    public void rotate() {
        rotation = (rotation + 1) % rotations.size();
    }

    /**
     * Sets the rotation index directly (engine only).
     *
     * @param rotation rotation index; taken modulo the number of states
     */
    public void setRotation(int rotation) {
        int n = rotations.size();
        this.rotation = ((rotation % n) + n) % n;
    }

    /**
     * Moves the anchor to a new cell (engine only).
     *
     * @param anchor the new anchor (must be non-null)
     */
    public void setAnchor(Coord anchor) {
        if (anchor == null) throw new IllegalArgumentException("anchor must not be null");
        this.anchor = anchor;
    }

    // ---- Rotation-state construction (pure geometry, no game logic) ----

    /**
     * Builds the four rotation states from a base offset list. Duplicate rotations
     * (e.g. the O square) are kept so every piece has exactly four indices, matching
     * {@link Rules#PIECE_ROTATIONS}.
     */
    private static List<List<Coord>> buildRotations(List<Coord> base) {
        List<List<Coord>> all = new ArrayList<List<Coord>>(Rules.PIECE_ROTATIONS);
        List<Coord> current = base;
        for (int i = 0; i < Rules.PIECE_ROTATIONS; i++) {
            all.add(Collections.unmodifiableList(new ArrayList<Coord>(current)));
            current = normalize(rotateCw(current));
        }
        return all;
    }

    /** Rotates each offset 90&deg; clockwise about the origin: (c,r) -&gt; (-r, c). */
    private static List<Coord> rotateCw(List<Coord> cells) {
        List<Coord> out = new ArrayList<Coord>(cells.size());
        for (int i = 0; i < cells.size(); i++) {
            Coord c = cells.get(i);
            out.add(new Coord(-c.row(), c.col()));
        }
        return out;
    }

    /** Shifts a cell list so its minimum column and row are both zero. */
    private static List<Coord> normalize(List<Coord> cells) {
        int minC = Integer.MAX_VALUE;
        int minR = Integer.MAX_VALUE;
        for (int i = 0; i < cells.size(); i++) {
            minC = Math.min(minC, cells.get(i).col());
            minR = Math.min(minR, cells.get(i).row());
        }
        List<Coord> out = new ArrayList<Coord>(cells.size());
        for (int i = 0; i < cells.size(); i++) {
            Coord c = cells.get(i);
            out.add(new Coord(c.col() - minC, c.row() - minR));
        }
        return out;
    }

    /** The rotation-0 cell layout for each shape, in {@code (col,row)} offsets. */
    private static List<Coord> baseOffsets(WallShape shape) {
        List<Coord> b = new ArrayList<Coord>();
        switch (shape) {
            case DOT:
                b.add(new Coord(0, 0));
                break;
            case I:
                b.add(new Coord(0, 0)); b.add(new Coord(1, 0));
                b.add(new Coord(2, 0)); b.add(new Coord(3, 0));
                break;
            case O:
                b.add(new Coord(0, 0)); b.add(new Coord(1, 0));
                b.add(new Coord(0, 1)); b.add(new Coord(1, 1));
                break;
            case T:
                b.add(new Coord(0, 0)); b.add(new Coord(1, 0));
                b.add(new Coord(2, 0)); b.add(new Coord(1, 1));
                break;
            case L:
                b.add(new Coord(0, 0)); b.add(new Coord(0, 1));
                b.add(new Coord(0, 2)); b.add(new Coord(1, 2));
                break;
            case J:
                b.add(new Coord(1, 0)); b.add(new Coord(1, 1));
                b.add(new Coord(1, 2)); b.add(new Coord(0, 2));
                break;
            case S:
                b.add(new Coord(1, 0)); b.add(new Coord(2, 0));
                b.add(new Coord(0, 1)); b.add(new Coord(1, 1));
                break;
            case Z:
                b.add(new Coord(0, 0)); b.add(new Coord(1, 0));
                b.add(new Coord(1, 1)); b.add(new Coord(2, 1));
                break;
            default:
                b.add(new Coord(0, 0));
                break;
        }
        return b;
    }

    @Override
    public String toString() {
        return "WallPiece(" + shape + ",rot=" + rotation + ",anchor=" + anchor + ")";
    }
}
