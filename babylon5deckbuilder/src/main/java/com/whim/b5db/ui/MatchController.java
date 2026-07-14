package com.whim.b5db.ui;

import com.whim.b5db.ai.Agent;
import com.whim.b5db.ai.HeuristicAgent;
import com.whim.b5db.engine.GameConfig;
import com.whim.b5db.engine.GameEngine;
import com.whim.b5db.engine.GameState;
import com.whim.b5db.engine.PlayerState;
import com.whim.b5db.engine.Seat;
import com.whim.b5db.model.Card;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin coordinator between the Swing UI and the {@link GameEngine}. Holds the
 * live {@link GameState}, drives AI seats automatically, and pauses on human
 * seats so the player can choose purchases. Kept UI-free so it can be unit
 * tested headlessly.
 */
public final class MatchController {

    private final GameEngine engine;
    private final GameState state;
    private final Map<Integer, Agent> agents = new HashMap<>();
    private boolean humanTurnStarted;

    public MatchController(List<Card> catalogue, List<Seat> seats, long seed) {
        this.engine = new GameEngine(catalogue, new GameConfig());
        this.state = engine.createGame(seats, seed);
        for (int i = 0; i < seats.size(); i++) {
            if (seats.get(i).ai) {
                agents.put(i, new HeuristicAgent(HeuristicAgent.Difficulty.NORMAL));
            }
        }
    }

    public GameState state() {
        return state;
    }

    public GameEngine engine() {
        return engine;
    }

    public boolean isOver() {
        return engine.isOver(state);
    }

    public PlayerState current() {
        return state.current();
    }

    public boolean isHumanTurn() {
        return !current().ai();
    }

    /**
     * Advance AI seats until it is a human's turn or the game ends. Returns the
     * number of AI turns played (for a UI log).
     */
    public int runAiUntilHumanOrOver() {
        int count = 0;
        while (!isOver() && current().ai()) {
            engine.playTurn(state, agents.get(state.currentPlayerIndex()));
            count++;
        }
        return count;
    }

    /** Begin the human's turn (START/STRATEGY/ACTION); safe to call once per turn. */
    public void beginHumanTurn() {
        if (!humanTurnStarted && isHumanTurn() && !isOver()) {
            engine.beginTurn(state);
            humanTurnStarted = true;
        }
    }

    public List<Card> affordable() {
        return engine.affordable(state, current());
    }

    public boolean buy(Card c) {
        return engine.purchase(state, current(), c);
    }

    /** Finish the human's turn and hand control back to the loop. */
    public void endHumanTurn() {
        engine.concludeTurn(state);
        humanTurnStarted = false;
    }

    public PlayerState leader() {
        return state.leader();
    }
}
