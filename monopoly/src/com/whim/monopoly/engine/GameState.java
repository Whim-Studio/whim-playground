package com.whim.monopoly.engine;

import com.whim.monopoly.domain.Board;
import com.whim.monopoly.domain.OwnableSpace;
import com.whim.monopoly.domain.Player;

public interface GameState {
    Board getBoard();
    java.util.List<Player> getPlayers();            // turn order; includes bankrupt (flagged)
    java.util.List<Player> getActivePlayers();      // not bankrupt
    Player getCurrentPlayer();
    Holding holdingAt(int spaceIndex);              // for any ownable index
    TurnPhase getPhase();
    boolean isGameOver();
    Player getWinner();                             // null until game over
    int[] getLastDice();                            // {d1,d2}; {0,0} before first roll
    // auction view (valid when phase == AUCTION)
    OwnableSpace getAuctionSpace();
    int getAuctionHighBid();
    Player getAuctionHighBidder();                  // null if no bid yet
}
