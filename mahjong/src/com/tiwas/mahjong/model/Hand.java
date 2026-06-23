package com.tiwas.mahjong.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A player's hand: the concealed tiles still held, the melds already exposed or
 * declared, and the bonus tiles (flowers/seasons) set aside.
 *
 * The {@code claimedDiscard} flag records whether this player has ever claimed a
 * discard during the current hand; if never, the hand qualifies as Fully
 * Concealed. The {@code pair} field is filled in when the hand is decomposed for
 * a win.
 */
public final class Hand {

    private final List<Tile> tiles = new ArrayList<Tile>();
    private final List<Meld> melds = new ArrayList<Meld>();
    private final List<Tile> bonus = new ArrayList<Tile>();
    private boolean claimedDiscard = false;
    private List<Tile> pair = null;

    public List<Tile> getTiles() {
        return tiles;
    }

    public List<Meld> getMelds() {
        return melds;
    }

    public List<Tile> getBonus() {
        return bonus;
    }

    public boolean hasClaimedDiscard() {
        return claimedDiscard;
    }

    public void setClaimedDiscard(boolean claimed) {
        this.claimedDiscard = claimed;
    }

    public List<Tile> getPair() {
        return pair;
    }

    public void setPair(List<Tile> pair) {
        this.pair = pair;
    }

    public void addTile(Tile t) {
        tiles.add(t);
    }

    public boolean removeTile(Tile t) {
        return tiles.remove(t);
    }

    public void addMeld(Meld m) {
        melds.add(m);
    }

    public void addBonus(Tile t) {
        bonus.add(t);
    }

    /** Count of identical (by face) tiles currently held concealed. */
    public int count(Tile face) {
        int n = 0;
        for (int i = 0; i < tiles.size(); i++) {
            if (tiles.get(i).equals(face)) {
                n++;
            }
        }
        return n;
    }

    /** Total tiles committed to the hand: concealed + 3 per meld (+1 per kong). */
    public int tileCount() {
        int n = tiles.size();
        for (int i = 0; i < melds.size(); i++) {
            n += melds.get(i).getTiles().size();
        }
        return n;
    }

    /** Reset for a new hand, keeping nothing. */
    public void clear() {
        tiles.clear();
        melds.clear();
        bonus.clear();
        claimedDiscard = false;
        pair = null;
    }
}
