package com.whim.monopoly.engine;

public enum TurnPhase {
    AWAITING_ROLL,         // current player must roll (or act in jail)
    AWAITING_BUY,          // landed on unowned ownable: buy or decline->auction
    AUCTION,               // an auction is live
    AWAITING_END_TURN,     // landing resolved; may build/trade/mortgage then end turn
    GAME_OVER
}
