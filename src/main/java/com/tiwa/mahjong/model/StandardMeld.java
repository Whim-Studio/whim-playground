package com.tiwa.mahjong.model;

import com.tiwa.mahjong.api.Meld;
import com.tiwa.mahjong.api.MeldType;
import com.tiwa.mahjong.api.Tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Concrete {@link Meld}: PUNG/KONG/CHOW/PAIR over a fixed tile list.
 * Chows are always concealed (per {@link MeldType} contract); the representative of a chow is
 * its lowest tile, and of any other meld is any (the first) tile.
 */
public final class StandardMeld implements Meld {

    private final MeldType type;
    private final List<Tile> tiles;
    private final boolean concealed;

    public StandardMeld(MeldType type, List<Tile> tiles, boolean concealed) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (tiles == null || tiles.isEmpty()) {
            throw new IllegalArgumentException("tiles must not be empty");
        }
        int expected = expectedSize(type);
        if (tiles.size() != expected) {
            throw new IllegalArgumentException(type + " requires " + expected + " tiles, got " + tiles.size());
        }
        // Chows may never be claimed from a discard, so they are always concealed.
        this.type = type;
        this.tiles = Collections.unmodifiableList(new ArrayList<Tile>(tiles));
        this.concealed = type == MeldType.CHOW || concealed;
    }

    private static int expectedSize(MeldType type) {
        switch (type) {
            case PAIR:
                return 2;
            case PUNG:
            case CHOW:
                return 3;
            case KONG:
                return 4;
            default:
                throw new IllegalArgumentException("Unknown meld type: " + type);
        }
    }

    @Override
    public MeldType getType() {
        return type;
    }

    @Override
    public List<Tile> getTiles() {
        return tiles;
    }

    @Override
    public boolean isConcealed() {
        return concealed;
    }

    @Override
    public Tile representative() {
        if (type == MeldType.CHOW) {
            Tile lowest = tiles.get(0);
            for (Tile t : tiles) {
                if (t.getRank() < lowest.getRank()) {
                    lowest = t;
                }
            }
            return lowest;
        }
        return tiles.get(0);
    }

    /** Convenience factory for a pung/kong/pair of identical tiles. */
    public static StandardMeld ofIdentical(MeldType type, Tile tile, boolean concealed) {
        int size = expectedSize(type);
        List<Tile> list = new ArrayList<Tile>(size);
        for (int i = 0; i < size; i++) {
            list.add(tile);
        }
        return new StandardMeld(type, list, concealed);
    }

    /** Convenience factory for a concealed chow of three consecutive suited tiles. */
    public static StandardMeld chow(Tile low, Tile mid, Tile high) {
        if (low.getSuit() != mid.getSuit() || mid.getSuit() != high.getSuit() || !low.getSuit().isSuited()) {
            throw new IllegalArgumentException("Chow requires three tiles of one suited suit");
        }
        List<Tile> list = new ArrayList<Tile>(3);
        list.add(low);
        list.add(mid);
        list.add(high);
        return new StandardMeld(MeldType.CHOW, list, true);
    }

    @Override
    public String toString() {
        return (concealed ? "concealed " : "exposed ") + type + tiles;
    }
}
