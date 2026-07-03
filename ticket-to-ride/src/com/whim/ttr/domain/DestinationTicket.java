package com.whim.ttr.domain;

/**
 * A destination ticket: connect {@link #from()} to {@link #to()} with a
 * continuous chain of your claimed routes to score {@link #points()} at game
 * end, or lose that many if unconnected. Immutable.
 */
public final class DestinationTicket {

    private final String from;
    private final String to;
    private final int points;

    public DestinationTicket(String from, String to, int points) {
        this.from = from;
        this.to = to;
        this.points = points;
    }

    public String from() { return from; }
    public String to() { return to; }
    public int points() { return points; }

    @Override public String toString() {
        return from + " → " + to + " (" + points + ")";
    }
}
