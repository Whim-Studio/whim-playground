package com.whim.albion.api;

/** Result of a player action (move, interact, combat action, item use). Immutable. */
public final class ActionResult {

    private final boolean success;
    private final String message;

    private ActionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static ActionResult ok() { return new ActionResult(true, ""); }
    public static ActionResult ok(String message) { return new ActionResult(true, message); }
    public static ActionResult fail(String message) { return new ActionResult(false, message); }

    public boolean isSuccess() { return success; }
    public String message() { return message; }
}
