package com.xiangqi.core;

/** An immutable piece: a side plus a type. */
public final class Piece {

    private final Side side;
    private final PieceType type;

    public Piece(Side side, PieceType type) {
        if (side == null || type == null) {
            throw new IllegalArgumentException("side and type must be non-null");
        }
        this.side = side;
        this.type = type;
    }

    public Side side() {
        return side;
    }

    public PieceType type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Piece)) {
            return false;
        }
        Piece other = (Piece) o;
        return side == other.side && type == other.type;
    }

    @Override
    public int hashCode() {
        return side.hashCode() * 31 + type.hashCode();
    }

    @Override
    public String toString() {
        return side + " " + type;
    }
}
