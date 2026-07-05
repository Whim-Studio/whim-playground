package com.whim.oggalaxy.api;

/** Simple success/failure result with a human-readable message for the UI. */
public final class Result {

    public final boolean ok;
    public final String message;

    private Result(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    public static Result ok() {
        return new Result(true, "");
    }

    public static Result ok(String message) {
        return new Result(true, message);
    }

    public static Result fail(String message) {
        return new Result(false, message);
    }
}
