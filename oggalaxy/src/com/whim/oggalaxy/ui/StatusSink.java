package com.whim.oggalaxy.ui;

/**
 * A channel a panel uses to report the outcome of a command back to the shell
 * (MainFrame) and to ask for an immediate state re-poll so the change shows at once.
 */
public interface StatusSink {
    /** Show a short transient status message; ok=false renders it as an error. */
    void status(String msg, boolean ok);

    /** Ask the shell to poll {@code controller.state()} and refresh now. */
    void requestRefresh();
}
