package com.tiwa.mahjong.engine;

import com.tiwa.mahjong.api.Tile;

/**
 * Outcome of declaring a kong (Section 4). A kong always draws a replacement tile - unless it was
 * declared on the last wall tile, in which case no replacement exists and the hand becomes a
 * drawn game (no winner).
 */
public final class KongResult {

    private final boolean drawnGame;
    private final Tile replacement;

    private KongResult(boolean drawnGame, Tile replacement) {
        this.drawnGame = drawnGame;
        this.replacement = replacement;
    }

    /** A replacement tile was drawn; play continues. */
    public static KongResult withReplacement(Tile replacement) {
        if (replacement == null) {
            throw new IllegalArgumentException("replacement must not be null");
        }
        return new KongResult(false, replacement);
    }

    /** Kong on the last tile: no replacement, the hand ends as a drawn game. */
    public static KongResult drawnGame() {
        return new KongResult(true, null);
    }

    public boolean isDrawnGame() {
        return drawnGame;
    }

    /** The replacement tile, or {@code null} when {@link #isDrawnGame()} is true. */
    public Tile getReplacement() {
        return replacement;
    }
}
