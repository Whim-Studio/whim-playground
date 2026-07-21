package com.railroad.model;

/**
 * An undirected piece of track joining two adjacent tiles. Two segments are
 * equal if they connect the same pair of tiles regardless of orientation, so the
 * network never stores a duplicate.
 */
public final class TrackSegment {

    private final GridPoint a;
    private final GridPoint b;
    private final int cost;

    public TrackSegment(GridPoint a, GridPoint b, int cost) {
        this.a = a;
        this.b = b;
        this.cost = cost;
    }

    public GridPoint getA() {
        return a;
    }

    public GridPoint getB() {
        return b;
    }

    /** Cash spent to build this segment. */
    public int getCost() {
        return cost;
    }

    /** Given one endpoint, returns the other (or null if p is neither). */
    public GridPoint other(GridPoint p) {
        if (p.equals(a)) {
            return b;
        }
        if (p.equals(b)) {
            return a;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TrackSegment)) {
            return false;
        }
        TrackSegment s = (TrackSegment) o;
        return (a.equals(s.a) && b.equals(s.b)) || (a.equals(s.b) && b.equals(s.a));
    }

    @Override
    public int hashCode() {
        // Order-independent so (a,b) and (b,a) hash alike.
        return a.hashCode() ^ b.hashCode();
    }
}
