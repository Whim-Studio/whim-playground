package com.tiwa.mahjong.scoring;

import com.tiwa.mahjong.api.Meld;
import com.tiwa.mahjong.api.MeldType;
import com.tiwa.mahjong.api.Suit;
import com.tiwa.mahjong.api.Tile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tiny in-test implementations of the {@code api} interfaces, so the scoring tests depend on
 * {@code com.tiwa.mahjong.api} alone (Task 1's concrete model classes need not exist on this branch).
 */
final class Stubs {

    private Stubs() {
    }

    static final class StubTile implements Tile {
        private final Suit suit;
        private final int rank;

        StubTile(Suit suit, int rank) {
            this.suit = suit;
            this.rank = rank;
        }

        public Suit getSuit() {
            return suit;
        }

        public int getRank() {
            return rank;
        }

        public boolean isHonor() {
            return suit.isHonor();
        }

        public boolean isSuited() {
            return suit.isSuited();
        }

        public boolean isBonus() {
            return suit.isBonus();
        }

        public boolean isTerminal() {
            return suit.isSuited() && (rank == 1 || rank == 9);
        }
    }

    static final class StubMeld implements Meld {
        private final MeldType type;
        private final List<Tile> tiles;
        private final boolean concealed;

        StubMeld(MeldType type, boolean concealed, List<Tile> tiles) {
            this.type = type;
            this.concealed = concealed;
            this.tiles = tiles;
        }

        public MeldType getType() {
            return type;
        }

        public List<Tile> getTiles() {
            return tiles;
        }

        public boolean isConcealed() {
            return concealed;
        }

        public Tile representative() {
            // For a chow this is the lowest tile (tiles are supplied in ascending order).
            return tiles.get(0);
        }
    }

    static Tile tile(Suit suit, int rank) {
        return new StubTile(suit, rank);
    }

    static Meld pung(Suit suit, int rank, boolean concealed) {
        Tile t = tile(suit, rank);
        return new StubMeld(MeldType.PUNG, concealed, Arrays.asList(t, t, t));
    }

    static Meld kong(Suit suit, int rank, boolean concealed) {
        Tile t = tile(suit, rank);
        return new StubMeld(MeldType.KONG, concealed, Arrays.asList(t, t, t, t));
    }

    static Meld chow(Suit suit, int low, boolean concealed) {
        List<Tile> tiles = new ArrayList<Tile>();
        tiles.add(tile(suit, low));
        tiles.add(tile(suit, low + 1));
        tiles.add(tile(suit, low + 2));
        return new StubMeld(MeldType.CHOW, concealed, tiles);
    }

    static Meld pair(Suit suit, int rank, boolean concealed) {
        Tile t = tile(suit, rank);
        return new StubMeld(MeldType.PAIR, concealed, Arrays.asList(t, t));
    }
}
