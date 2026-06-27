package com.whim.tacticalnexus.ui;

import com.whim.tacticalnexus.engine.MoveOutcome;
import com.whim.tacticalnexus.engine.MoveResolver;
import com.whim.tacticalnexus.state.GameState;
import com.whim.tacticalnexus.state.StateManager;

/**
 * Wires {@link StateManager} together with the pure {@link MoveResolver} engine.
 *
 * <p>This controller owns <b>no game rules</b>. A move simply asks the engine to
 * resolve a movement vector and, only when the resulting {@link MoveOutcome} says
 * the state actually {@link MoveOutcome#changed() changed}, commits the new state
 * so it becomes undoable. Undo/redo delegate straight to the {@link StateManager}.
 *
 * <p>Every public method returns whether the view should repaint as a result.
 */
public final class GameController {

    private final StateManager manager;
    private String lastMessage = "";

    public GameController(StateManager manager) {
        this.manager = manager;
    }

    /**
     * Resolve a movement vector through the engine.
     *
     * @param dRow row delta in {-1, 0, 1}
     * @param dCol column delta in {-1, 0, 1}
     * @return {@code true} if the state changed and the view should repaint
     */
    public boolean move(int dRow, int dCol) {
        GameState before = manager.current();
        MoveOutcome outcome = MoveResolver.resolve(before, dRow, dCol);
        lastMessage = outcome.message();
        if (outcome.changed()) {
            manager.commit(outcome.state());
            return true;
        }
        return false;
    }

    /** @return {@code true} if an undo happened and the view should repaint. */
    public boolean undo() {
        if (!manager.canUndo()) {
            return false;
        }
        manager.undo();
        lastMessage = "Undo";
        return true;
    }

    /** @return {@code true} if a redo happened and the view should repaint. */
    public boolean redo() {
        if (!manager.canRedo()) {
            return false;
        }
        manager.redo();
        lastMessage = "Redo";
        return true;
    }

    /** Short human-readable description of the most recent action. */
    public String lastMessage() {
        return lastMessage;
    }

    public StateManager stateManager() {
        return manager;
    }
}
