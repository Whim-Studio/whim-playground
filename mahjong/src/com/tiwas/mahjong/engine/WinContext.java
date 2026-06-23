package com.tiwas.mahjong.engine;

import com.tiwas.mahjong.model.Tile;
import com.tiwas.mahjong.model.Wind;

/**
 * The circumstances of a win, needed for the situational doubles and limit hands.
 */
public final class WinContext {

    public boolean selfDraw;        // won by drawing rather than off a discard
    public boolean fullyConcealed;  // never claimed a discard this hand
    public boolean firstTile;       // won on the very first drawn/played tile
    public boolean lastTile;        // won on the last drawn tile from the wall
    public boolean finalDiscard;    // won on the final discard (wall empty)
    public boolean dealer;          // the winner is the dealer (seat East)
    public boolean firstGoAround;   // still the opening go-around (no claims yet)
    public Tile winningTile;        // the tile that completed the hand
    public Wind seatWind;
    public Wind roundWind;
}
