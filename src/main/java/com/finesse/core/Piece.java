package com.finesse.core;

/**
 * An immutable piece: a {@link PieceType} owned by a {@link PieceColor}.
 *
 * <p>Pieces carry no position of their own; placement is tracked by the
 * {@link Board}. Because they are immutable value objects, the same instance can
 * safely be shared across boards and copies.
 */
public final class Piece {

    private final PieceType type;
    private final PieceColor color;

    /**
     * Creates a piece.
     *
     * @param type  the kind of piece; must not be {@code null}
     * @param color the owning side; must not be {@code null}
     * @throws NullPointerException if {@code type} or {@code color} is {@code null}
     */
    public Piece(PieceType type, PieceColor color) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        if (color == null) {
            throw new NullPointerException("color");
        }
        this.type = type;
        this.color = color;
    }

    /**
     * Factory equivalent to the constructor.
     *
     * @param type  the kind of piece
     * @param color the owning side
     * @return a new {@code Piece}
     */
    public static Piece of(PieceType type, PieceColor color) {
        return new Piece(type, color);
    }

    /** @return the kind of piece. */
    public PieceType getType() {
        return type;
    }

    /** @return the owning side. */
    public PieceColor getColor() {
        return color;
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
        return type == other.type && color == other.color;
    }

    @Override
    public int hashCode() {
        return 31 * type.hashCode() + color.hashCode();
    }

    @Override
    public String toString() {
        return color + " " + type;
    }
}
