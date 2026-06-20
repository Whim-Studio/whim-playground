package com.tiwa.mahjong.model;

import com.tiwa.mahjong.api.Tile;
import com.tiwa.mahjong.api.Wall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * The 144-tile wall (18 long x 2 high per side). There is NO dead wall - every tile is drawable.
 * Both {@link #draw()} and {@link #drawReplacement()} take the next tile from the single drawing
 * front, so a replacement is simply the next normal tile clockwise from the last drawn tile.
 *
 * <p>The wall is shuffled deterministically from the supplied {@link Random} so tests are
 * reproducible given a seed.</p>
 */
public final class StandardWall implements Wall {

    private final List<Tile> tiles;
    private int next;

    /** Builds the full 144-tile set and shuffles it with the supplied source of randomness. */
    public StandardWall(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.tiles = TileFactory.buildFullSet();
        Collections.shuffle(this.tiles, random);
        this.next = 0;
    }

    /** Convenience constructor seeding a {@link Random} from a long. */
    public StandardWall(long seed) {
        this(new Random(seed));
    }

    @Override
    public Tile draw() {
        if (isEmpty()) {
            throw new NoSuchElementException("wall is empty");
        }
        return tiles.get(next++);
    }

    @Override
    public Tile drawReplacement() {
        // Same single drawing front as draw(): the next normal tile clockwise.
        return draw();
    }

    @Override
    public int tilesRemaining() {
        return tiles.size() - next;
    }

    @Override
    public boolean isEmpty() {
        return next >= tiles.size();
    }

    @Override
    public boolean isLastTile() {
        return tilesRemaining() == 1;
    }

    /** Test/diagnostic snapshot of the remaining tiles in draw order. */
    public List<Tile> remainingTiles() {
        return new ArrayList<Tile>(tiles.subList(next, tiles.size()));
    }
}
