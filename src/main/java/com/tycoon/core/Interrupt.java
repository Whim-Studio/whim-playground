package com.tycoon.core;

/**
 * A signal that halts the auto-turn loop and returns control to the player.
 */
public final class Interrupt {
    private final InterruptType type;
    private final long hour;
    private final String message;

    public Interrupt(InterruptType type, long hour, String message) {
        this.type = type;
        this.hour = hour;
        this.message = message;
    }

    public InterruptType type() {
        return type;
    }

    public long hour() {
        return hour;
    }

    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return "[" + hour + "h] " + type + ": " + message;
    }
}
