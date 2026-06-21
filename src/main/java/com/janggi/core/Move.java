package com.janggi.core;

/**
 * Immutable move from one intersection to another. A "pass" (legal in Janggi for
 * either side) is represented as {@code new Move(p, p)} where from == to.
 */
public final class Move {

    private final Position from;
    private final Position to;

    public Move(Position from, Position to) {
        this.from = from;
        this.to = to;
    }

    public Position from() {
        return from;
    }

    public Position to() {
        return to;
    }

    public boolean isPass() {
        return from.equals(to);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Move)) {
            return false;
        }
        Move other = (Move) o;
        return from.equals(other.from) && to.equals(other.to);
    }

    @Override
    public int hashCode() {
        return from.hashCode() * 31 + to.hashCode();
    }

    @Override
    public String toString() {
        if (isPass()) {
            return "pass(" + from + ")";
        }
        return from + "->" + to;
    }
}
