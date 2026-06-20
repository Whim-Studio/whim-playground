package com.tiwa.mahjong.model;

import com.tiwa.mahjong.api.Meld;
import com.tiwa.mahjong.api.PlayerView;
import com.tiwa.mahjong.api.Tile;
import com.tiwa.mahjong.api.Wind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Concrete {@link PlayerView} with the mutation API needed by dealing and the (Task 2) turn loop.
 * A player owns concealed tiles, declared melds, revealed bonus tiles (flowers/seasons), and a
 * flag recording whether it has claimed any discard this hand.
 */
public final class Player implements PlayerView {

    private final int seatIndex;
    private Wind seatWind;
    private final List<Tile> concealed = new ArrayList<Tile>();
    private final List<Meld> melds = new ArrayList<Meld>();
    private final List<Tile> bonus = new ArrayList<Tile>();
    private boolean claimedDiscardThisHand;

    public Player(int seatIndex, Wind seatWind) {
        if (seatIndex < 0 || seatIndex > 3) {
            throw new IllegalArgumentException("seatIndex must be 0..3");
        }
        if (seatWind == null) {
            throw new IllegalArgumentException("seatWind must not be null");
        }
        this.seatIndex = seatIndex;
        this.seatWind = seatWind;
    }

    @Override
    public int getSeatIndex() {
        return seatIndex;
    }

    @Override
    public Wind getSeatWind() {
        return seatWind;
    }

    public void setSeatWind(Wind seatWind) {
        if (seatWind == null) {
            throw new IllegalArgumentException("seatWind must not be null");
        }
        this.seatWind = seatWind;
    }

    @Override
    public List<Tile> getConcealedTiles() {
        return Collections.unmodifiableList(concealed);
    }

    @Override
    public List<Meld> getMelds() {
        return Collections.unmodifiableList(melds);
    }

    @Override
    public List<Tile> getBonusTiles() {
        return Collections.unmodifiableList(bonus);
    }

    @Override
    public boolean hasClaimedDiscardThisHand() {
        return claimedDiscardThisHand;
    }

    // ---- mutators ----

    public void addConcealedTile(Tile tile) {
        if (tile == null) {
            throw new IllegalArgumentException("tile must not be null");
        }
        concealed.add(tile);
    }

    public void addConcealedTiles(List<Tile> tiles) {
        for (Tile t : tiles) {
            addConcealedTile(t);
        }
    }

    /** Removes one matching tile (by value); returns true if a tile was removed. */
    public boolean removeConcealedTile(Tile tile) {
        return concealed.remove(tile);
    }

    public void addMeld(Meld meld) {
        if (meld == null) {
            throw new IllegalArgumentException("meld must not be null");
        }
        melds.add(meld);
    }

    /** Records a revealed bonus tile (flower/season). Throws if the tile is not a bonus tile. */
    public void revealBonusTile(Tile tile) {
        if (tile == null || !tile.isBonus()) {
            throw new IllegalArgumentException("revealBonusTile requires a flower/season tile");
        }
        bonus.add(tile);
    }

    /** Marks that this player has claimed a discard this hand (disqualifies Fully Concealed Hand). */
    public void markClaimedDiscard() {
        this.claimedDiscardThisHand = true;
    }

    /** Resets the per-hand claim flag (e.g. at the start of a new hand). */
    public void resetClaimedDiscard() {
        this.claimedDiscardThisHand = false;
    }

    @Override
    public String toString() {
        return "Player[seat=" + seatIndex + ", wind=" + seatWind + ", concealed=" + concealed.size()
                + ", melds=" + melds.size() + ", bonus=" + bonus.size() + "]";
    }
}
