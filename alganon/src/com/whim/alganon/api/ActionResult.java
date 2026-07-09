package com.whim.alganon.api;

/** Uniform result of any player-driven intent handed across the controller seam. */
public final class ActionResult {
    private final boolean success;
    private final String message;

    private ActionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static ActionResult ok() { return new ActionResult(true, ""); }
    public static ActionResult ok(String msg) { return new ActionResult(true, msg); }
    public static ActionResult fail(String msg) { return new ActionResult(false, msg); }

    public boolean isSuccess() { return success; }
    public String message() { return message; }
}
