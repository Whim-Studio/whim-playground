package com.whim.tacticalnexus.engine;

import com.whim.tacticalnexus.state.GameState;

/**
 * Immutable result of resolving a single movement vector. Pure data.
 *
 * <p>{@link #changed()} is true only when the move actually mutated state and is
 * therefore worth committing to the undo stack. Blocked moves return the
 * original {@code state} instance with {@code changed=false}.
 */
public final class MoveOutcome {

    private final GameState state;
    private final boolean changed;
    private final String message;

    public MoveOutcome(GameState state, boolean changed, String message) {
        this.state = state;
        this.changed = changed;
        this.message = message;
    }

    /** Resulting state (same instance as the input when nothing changed). */
    public GameState state() {
        return state;
    }

    /** true if the move mutated state (undo-worthy). */
    public boolean changed() {
        return changed;
    }

    /** Short human-readable description for the status line / log. */
    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return "MoveOutcome{changed=" + changed + ", message=" + message + "}";
    }
}
