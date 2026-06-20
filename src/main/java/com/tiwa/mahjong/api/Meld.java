package com.tiwa.mahjong.api;

import java.util.List;

/**
 * A revealed or concealed group within a hand. Implementations live in Task 1's {@code model} package.
 */
public interface Meld {

    MeldType getType();

    /** The tiles forming this meld (size 2 for PAIR, 3 for PUNG/CHOW, 4 for KONG). */
    List<Tile> getTiles();

    /**
     * True if the meld was formed entirely from drawn tiles (never from a discard claim).
     * Concealed pungs/kongs and all chows score at the concealed rate and feed concealed-hand bonuses.
     */
    boolean isConcealed();

    /** The rank-defining tile (any tile for pung/kong/pair; the lowest tile for a chow). */
    Tile representative();
}
