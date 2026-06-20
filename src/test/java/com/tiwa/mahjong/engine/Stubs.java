package com.tiwa.mahjong.engine;

import com.tiwa.mahjong.api.Meld;
import com.tiwa.mahjong.api.MeldType;
import com.tiwa.mahjong.api.PlayerView;
import com.tiwa.mahjong.api.Suit;
import com.tiwa.mahjong.api.Tile;
import com.tiwa.mahjong.api.Wall;
import com.tiwa.mahjong.api.Wind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tiny in-test implementations of the api interfaces. The engine codes strictly against api, so
 * these stubs stand in for Task 1's concrete model.
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Tile)) {
                return false;
            }
            Tile other = (Tile) o;
            return suit == other.getSuit() && rank == other.getRank();
        }

        @Override
        public int hashCode() {
            return suit.ordinal() * 31 + rank;
        }

        @Override
        public String toString() {
            return suit + "-" + rank;
        }
    }

    static final class StubMeld implements Meld {
        private final MeldType type;
        private final List<Tile> tiles;
        private final boolean concealed;

        StubMeld(MeldType type, boolean concealed, Tile... tiles) {
            this.type = type;
            this.concealed = concealed;
            this.tiles = Collections.unmodifiableList(new ArrayList<Tile>(Arrays.asList(tiles)));
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
            return tiles.get(0);
        }
    }

    static final class StubPlayerView implements PlayerView {
        private final int seatIndex;
        private final Wind seatWind;
        private final List<Tile> concealed;
        private final List<Meld> melds;
        private final List<Tile> bonus;
        private final boolean claimedDiscard;

        StubPlayerView(int seatIndex, Wind seatWind, List<Tile> concealed, List<Meld> melds,
                       List<Tile> bonus, boolean claimedDiscard) {
            this.seatIndex = seatIndex;
            this.seatWind = seatWind;
            this.concealed = concealed;
            this.melds = melds;
            this.bonus = bonus;
            this.claimedDiscard = claimedDiscard;
        }

        public int getSeatIndex() {
            return seatIndex;
        }

        public Wind getSeatWind() {
            return seatWind;
        }

        public List<Tile> getConcealedTiles() {
            return concealed;
        }

        public List<Meld> getMelds() {
            return melds;
        }

        public List<Tile> getBonusTiles() {
            return bonus;
        }

        public boolean hasClaimedDiscardThisHand() {
            return claimedDiscard;
        }
    }

    /** A wall that hands out queued tiles and reports configurable last-tile/empty states. */
    static final class StubWall implements Wall {
        private final List<Tile> tiles;
        private int index = 0;
        private boolean lastTile;

        StubWall(List<Tile> tiles, boolean lastTile) {
            this.tiles = new ArrayList<Tile>(tiles);
            this.lastTile = lastTile;
        }

        public Tile draw() {
            if (isEmpty()) {
                throw new IllegalStateException("wall empty");
            }
            return tiles.get(index++);
        }

        public Tile drawReplacement() {
            return draw();
        }

        public int tilesRemaining() {
            return tiles.size() - index;
        }

        public boolean isEmpty() {
            return index >= tiles.size();
        }

        public boolean isLastTile() {
            return lastTile;
        }

        void setLastTile(boolean v) {
            this.lastTile = v;
        }
    }

    static Tile tile(Suit suit, int rank) {
        return new StubTile(suit, rank);
    }
}
