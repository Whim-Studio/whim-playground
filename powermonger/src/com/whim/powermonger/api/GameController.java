package com.whim.powermonger.api;

import com.whim.powermonger.api.Enums.CommandType;
import com.whim.powermonger.api.Enums.Posture;
import com.whim.powermonger.api.Views.GameStateView;

/**
 * The single seam between the UI (Task 3) and the simulation engine (Task 2).
 * Task 2 provides the implementation; Task 3 depends only on this interface.
 *
 * Threading contract: the engine advances on its own background thread. All
 * mutating methods here are thread-safe and may be called from the Swing EDT.
 * {@link #state()} returns a consistent snapshot safe to read on the EDT.
 */
public interface GameController {

    /** Consistent read-only snapshot for rendering. Never null. */
    GameStateView state();

    /**
     * Issue an indirect order to a Captain's bloc. If the target captain is a
     * subordinate, the order is delayed by a carrier pigeon whose flight time is
     * proportional to the distance from the supreme commander (command lag).
     */
    ActionResult issueOrder(int captainId, CommandType type, int targetTileX, int targetTileY);

    /** Set the movement destination (a plain MOVE order). */
    ActionResult setDestination(int captainId, int targetTileX, int targetTileY);

    /** Change a bloc's posture (1/2/3 swords -> 25/50/100%). */
    ActionResult setPosture(int captainId, Posture posture);

    /** Mark a captain as the UI selection. */
    void selectCaptain(int captainId);

    /** Currently selected captain id, or -1. */
    int selectedCaptainId();

    /** (Re)initialise the world with a deterministic seed. */
    void newGame(long seed);

    /** Start the background simulation thread. Idempotent. */
    void start();

    /** Stop the background simulation thread. Idempotent. */
    void stop();
}
