package com.tiwas.mahjong.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The wall of tiles. These rules use no dead wall: all 144 tiles are live and a
 * replacement tile (for a flower, season or kong) is simply the next normal tile
 * in draw order. When the wall is exhausted the hand is a drawn game.
 */
public final class Wall {

    private final List<Tile> tiles;
    private int pointer;

    public Wall(List<Tile> tiles) {
        this.tiles = new ArrayList<Tile>(tiles);
        this.pointer = 0;
    }

    /** Number of tiles still available to draw. */
    public int remaining() {
        return tiles.size() - pointer;
    }

    public boolean isEmpty() {
        return remaining() <= 0;
    }

    /** Draw the next live tile, or null if the wall is empty. */
    public Tile draw() {
        if (isEmpty()) {
            return null;
        }
        return tiles.get(pointer++);
    }

    /**
     * A replacement draw (flower/season/kong). Identical to a normal draw under
     * these rules — the next live tile clockwise from the last drawn tile.
     */
    public Tile drawReplacement() {
        return draw();
    }
}
