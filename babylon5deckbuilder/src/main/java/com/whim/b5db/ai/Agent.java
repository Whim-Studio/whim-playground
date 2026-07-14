package com.whim.b5db.ai;

import com.whim.b5db.engine.GameEngine;
import com.whim.b5db.engine.GameState;
import com.whim.b5db.engine.PlayerState;
import com.whim.b5db.model.Card;

import java.util.List;

/**
 * A decision-making agent. The only choice the engine delegates is the
 * ACQUISITION_PHASE: which cards to buy with the INFLUENCE accumulated this
 * turn. (Card play and conflict resolution are automatic in this model.)
 */
public interface Agent {

    /**
     * @return an ordered list of cards to purchase this turn. The plan must
     *         respect the player's current INFLUENCE; the engine re-validates
     *         each purchase and silently skips any it cannot afford.
     */
    List<Card> chooseAcquisitions(GameState state, PlayerState me, GameEngine engine);

    /** @return a short label used in reports and the UI. */
    String label();
}
