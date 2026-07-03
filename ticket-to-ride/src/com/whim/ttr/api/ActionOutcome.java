package com.whim.ttr.api;

import java.util.Collections;
import java.util.List;

/**
 * Immutable result of an engine action, returned to the UI.
 *
 * <p>For a TUNNEL claim, {@link #beginClaimRoute} returns an outcome with
 * {@link #awaitingTunnel} == true, {@link #tunnelDraw} holding the 3 flipped
 * cards, and {@link #tunnelExtra} holding how many additional matching cards
 * the player must supply via {@code GameEngine.confirmTunnel(...)}.</p>
 */
public final class ActionOutcome {

    private final boolean success;
    private final String message;
    private final boolean awaitingTunnel;
    private final List<CardColor> tunnelDraw;
    private final int tunnelExtra;

    public ActionOutcome(boolean success, String message,
                         boolean awaitingTunnel, List<CardColor> tunnelDraw, int tunnelExtra) {
        this.success = success;
        this.message = message == null ? "" : message;
        this.awaitingTunnel = awaitingTunnel;
        this.tunnelDraw = tunnelDraw == null
                ? Collections.<CardColor>emptyList()
                : Collections.unmodifiableList(tunnelDraw);
        this.tunnelExtra = tunnelExtra;
    }

    /** Convenience: a plain success/failure with a message. */
    public static ActionOutcome of(boolean success, String message) {
        return new ActionOutcome(success, message, false, null, 0);
    }

    /** Convenience: a pending tunnel prompt. */
    public static ActionOutcome tunnel(List<CardColor> draw, int extra) {
        return new ActionOutcome(false, "Tunnel: " + extra + " extra card(s) required",
                true, draw, extra);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public boolean isAwaitingTunnel() { return awaitingTunnel; }
    public List<CardColor> getTunnelDraw() { return tunnelDraw; }
    public int getTunnelExtra() { return tunnelExtra; }
}
