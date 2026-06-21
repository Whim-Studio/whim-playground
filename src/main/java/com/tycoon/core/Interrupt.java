package com.tycoon.core;
// LOCAL-ONLY VERIFICATION STUB (Task 1 owns this) — never committed by Task 2.
public final class Interrupt {
    private final InterruptType type;
    private final long hour;
    private final String message;
    public Interrupt(InterruptType type, long hour, String message) {
        this.type = type; this.hour = hour; this.message = message;
    }
    public InterruptType type() { return type; }
    public long hour() { return hour; }
    public String message() { return message; }
}
