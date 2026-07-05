package com.whim.populous.domain;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Views.PapalMagnetView;

/**
 * The rally point ("papal magnet") for one deity. When active, that side's
 * followers are drawn toward its tile — in Populous this is how you gather your
 * people onto a chosen spot before flattening land or triggering Armageddon.
 * There is exactly one magnet per side; it belongs to a fixed {@link Allegiance}.
 */
public final class PapalMagnet implements PapalMagnetView {

    private final Allegiance side;
    private boolean active;
    private int col;
    private int row;

    public PapalMagnet(Allegiance side) {
        this.side = side;
    }

    // ---- PapalMagnetView ----------------------------------------------------

    @Override public boolean active() { return active; }
    @Override public int col() { return col; }
    @Override public int row() { return row; }
    @Override public Allegiance side() { return side; }

    // ---- engine mutation ----------------------------------------------------

    /** Plant the magnet at a tile and activate it. */
    public void placeAt(int col, int row) {
        this.col = col;
        this.row = row;
        this.active = true;
    }

    /** Remove the rally point. */
    public void clear() {
        this.active = false;
    }

    public void setActive(boolean active) { this.active = active; }
}
