package com.whim.ttr.domain;

import com.whim.ttr.api.CardColor;
import com.whim.ttr.api.RouteKind;

/**
 * A single edge between two cities. A "double route" on the physical board is
 * modelled as two separate {@code Route} objects that share the same endpoints
 * but carry distinct ids (and usually distinct colors).
 *
 * <p>Everything is immutable except {@link #ownerId()}, which the engine sets
 * once when a player claims the route.</p>
 */
public final class Route {

    private final String id;
    private final String cityA;
    private final String cityB;
    private final int length;
    private final CardColor color;      // null == GRAY (any single color)
    private final RouteKind kind;
    private final int locomotivesRequired;
    private Integer ownerId;            // null until claimed

    public Route(String id, String cityA, String cityB, int length,
                 CardColor color, RouteKind kind, int locomotivesRequired) {
        this.id = id;
        this.cityA = cityA;
        this.cityB = cityB;
        this.length = length;
        this.color = color;
        this.kind = kind;
        this.locomotivesRequired = locomotivesRequired;
        this.ownerId = null;
    }

    public String id() { return id; }
    public String cityA() { return cityA; }
    public String cityB() { return cityB; }
    public int length() { return length; }
    public CardColor color() { return color; }
    public RouteKind kind() { return kind; }
    public int locomotivesRequired() { return locomotivesRequired; }

    public Integer ownerId() { return ownerId; }
    public void setOwnerId(Integer ownerId) { this.ownerId = ownerId; }
    public boolean isClaimed() { return ownerId != null; }

    /** True if this route joins {@code a} and {@code b} in either direction. */
    public boolean connects(String a, String b) {
        return (cityA.equals(a) && cityB.equals(b))
            || (cityA.equals(b) && cityB.equals(a));
    }

    /** The endpoint at the far side of {@code from}, or null if {@code from} is not an endpoint. */
    public String other(String from) {
        if (cityA.equals(from)) return cityB;
        if (cityB.equals(from)) return cityA;
        return null;
    }

    @Override public String toString() { return id; }
}
