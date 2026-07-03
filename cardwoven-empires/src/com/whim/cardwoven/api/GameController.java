package com.whim.cardwoven.api;

import com.whim.cardwoven.api.Views.GameStateView;

/**
 * The single seam between the UI (Task 3) and the game logic (Task 2).
 *
 * The UI holds a GameController, renders {@link #state()}, and calls the action
 * methods in response to user input. Every action returns an {@link ActionResult}
 * and (on success) mutates state; the UI then re-reads {@link #state()} and
 * repaints. The UI must NOT touch domain or engine classes directly.
 *
 * Task 3 can develop against a stub implementation of this interface; Task 2
 * provides the real engine-backed implementation. Java 8 only.
 */
public interface GameController {

    /** Immutable-facing snapshot of the whole game for rendering. */
    GameStateView state();

    /**
     * Play a BUILDING card from the current human player's hand onto a tile.
     * Fails if the tile is occupied, terrain is illegal, cost unaffordable, or
     * the card is not a building.
     */
    ActionResult playBuilding(int cardId, int row, int col);

    /**
     * Attach an ATTACHMENT card from hand to a building the player owns.
     * Fails if capacity is full, the attachment is illegal for that building
     * type, or cost is unaffordable.
     */
    ActionResult attachCard(int cardId, int buildingId);

    /**
     * Play an ECONOMY / EXPLORE / one-shot card (non-building, non-attachment).
     * For EXPLORE cards, row/col name the tile to reveal (ignored otherwise).
     */
    ActionResult playCard(int cardId, int row, int col);

    /**
     * Commit a MILITARY card from hand against a raider or enemy building at the
     * given tile. Resolves combat immediately.
     */
    ActionResult resolveCombat(int cardId, int row, int col);

    /**
     * Advance to the next phase/turn. When the human ends their turn the engine
     * runs AI opponents, applies yields, checks victory, and returns control.
     */
    ActionResult endTurn();

    /** Start / restart a game with the chosen human faction. */
    void newGame(Enums.Faction humanFaction);
}
