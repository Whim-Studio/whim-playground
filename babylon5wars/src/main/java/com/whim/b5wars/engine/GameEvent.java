package com.whim.b5wars.engine;

/**
 * A structured log event produced by the engine (movement, fire, hit, crit, phase, victory).
 * The UI/log panel renders these; tests assert on {@link #getType()} / {@link #getMessage()}.
 */
public final class GameEvent {

    private final String type;
    private final String message;

    public GameEvent(String type, String message) {
        this.type = type;
        this.message = message;
    }

    /** One of "MOVE","FIRE","HIT","MISS","CRIT","PHASE","VICTORY" (free-form, not enforced). */
    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "[" + type + "] " + message;
    }
}
