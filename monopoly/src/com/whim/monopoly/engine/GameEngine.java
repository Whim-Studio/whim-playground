package com.whim.monopoly.engine;

import com.whim.monopoly.domain.Player;

public interface GameEngine {
    GameState getState();
    void addListener(GameListener l);

    // --- turn flow ---
    void rollDice();              // rolls, moves current player, resolves landing, emits logs,
                                  // sets phase to AWAITING_BUY / AWAITING_END_TURN / AWAITING_ROLL(doubles)
    void endTurn();              // advances to next active player; phase -> AWAITING_ROLL

    // --- buy / auction (phase AWAITING_BUY then AUCTION) ---
    void buyProperty();          // current player buys landed ownable at list price
    void declineProperty();      // start mandatory auction for the landed ownable
    void placeBid(Player p, int amount);   // amount must exceed current high bid
    void passAuction(Player p);            // p drops out; engine resolves when one bidder remains

    // --- building / mortgage (allowed in AWAITING_END_TURN for current player) ---
    boolean canBuildHouse(int spaceIndex);
    void buildHouse(int spaceIndex);
    void sellHouse(int spaceIndex);
    boolean canMortgage(int spaceIndex);
    void mortgage(int spaceIndex);
    void unmortgage(int spaceIndex);

    // --- jail (phase AWAITING_ROLL when current player isInJail) ---
    void payJailFine();          // pay $50, then must still rollDice() to move
    void useJailCard();          // spend a Get-Out-of-Jail-Free card, then rollDice()

    // --- trading ---
    boolean isTradeValid(Trade t);
    void executeTrade(Trade t);  // moves cash/deeds/jail cards; mortgaged deeds stay mortgaged

    // --- bankruptcy ---
    void declareBankruptcy();    // current player yields to current creditor (bank or player)
}
