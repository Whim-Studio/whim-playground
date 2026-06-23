package com.tiwas.mahjong.engine;

import com.tiwas.mahjong.model.ScoreSheet;

/** The outcome of a completed hand: a win, a drawn game, or a false-mahjong penalty. */
public final class HandResult {

    public int winner = -1;            // player index, or -1 for a drawn game
    public boolean drawnGame;
    public boolean falseMahjong;
    public int offender = -1;          // player index of a false-mahjong declarer
    public ScoreSheet sheet;           // present for a real win
    public int[] deltas;               // per-player score change this hand
    public String message = "";
    public boolean gameOver;

    public HandResult(int players) {
        deltas = new int[players];
    }
}
