package com.xiangqi.core;

/** An immutable move from one position to another. */
public final class Move {

    private final Position from;
    private final Position to;

    public Move(Position from, Position to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to must be non-null");
        }
        this.from = from;
        this.to = to;
    }

    public Position from() {
        return from;
    }

    public Position to() {
        return to;
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
        return from + "->" + to;
    }
}
