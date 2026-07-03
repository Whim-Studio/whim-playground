package com.whim.cardwoven.api;

/** Result of a controller action: success flag plus a human-readable message. */
public final class ActionResult {
    private final boolean success;
    private final String message;

    private ActionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static ActionResult ok(String message) {
        return new ActionResult(true, message);
    }

    public static ActionResult fail(String message) {
        return new ActionResult(false, message);
    }

    public boolean isSuccess() { return success; }
    public String message() { return message; }

    @Override
    public String toString() {
        return (success ? "OK: " : "FAIL: ") + message;
    }
}
