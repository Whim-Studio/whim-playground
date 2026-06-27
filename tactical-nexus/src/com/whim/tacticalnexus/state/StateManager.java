package com.whim.tacticalnexus.state;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Stack-based undo/redo over immutable {@link GameState} snapshots. Because the
 * states share structure, {@link #commit} only pushes references — no deep copy.
 */
public final class StateManager {
    private final Deque<GameState> undoStack = new ArrayDeque<GameState>();
    private final Deque<GameState> redoStack = new ArrayDeque<GameState>();
    private GameState current;

    public StateManager(GameState initial) {
        this.current = initial;
    }

    public GameState current() {
        return current;
    }

    /** Pushes the current state onto the undo stack, adopts {@code next}, clears redo. */
    public void commit(GameState next) {
        undoStack.push(current);
        current = next;
        redoStack.clear();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /** Steps back one state (or returns the unchanged current if nothing to undo). */
    public GameState undo() {
        if (undoStack.isEmpty()) {
            return current;
        }
        redoStack.push(current);
        current = undoStack.pop();
        return current;
    }

    /** Re-applies the last undone state (or returns the unchanged current). */
    public GameState redo() {
        if (redoStack.isEmpty()) {
            return current;
        }
        undoStack.push(current);
        current = redoStack.pop();
        return current;
    }
}
