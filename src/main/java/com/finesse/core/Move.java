package com.finesse.core;

/**
 * An immutable description of a single move: moving from one square to another,
 * with an optional promotion.
 *
 * <p>The {@code promotion} field is {@code null} for ordinary moves. When a move
 * results in a piece becoming another type (for example a pawn reaching the far
 * rank), {@code promotion} holds the resulting {@link PieceType}. The core does
 * not decide <em>when</em> promotion is legal; that is the variant's concern.
 */
public final class Move {

    private final Position from;
    private final Position to;
    private final PieceType promotion;

    /**
     * Creates a non-promoting move.
     *
     * @param from the origin square; must not be {@code null}
     * @param to   the destination square; must not be {@code null}
     */
    public Move(Position from, Position to) {
        this(from, to, null);
    }

    /**
     * Creates a move, optionally with a promotion.
     *
     * @param from      the origin square; must not be {@code null}
     * @param to        the destination square; must not be {@code null}
     * @param promotion the type to promote to, or {@code null} for none
     * @throws NullPointerException if {@code from} or {@code to} is {@code null}
     */
    public Move(Position from, Position to, PieceType promotion) {
        if (from == null) {
            throw new NullPointerException("from");
        }
        if (to == null) {
            throw new NullPointerException("to");
        }
        this.from = from;
        this.to = to;
        this.promotion = promotion;
    }

    /** @return the origin square. */
    public Position getFrom() {
        return from;
    }

    /** @return the destination square. */
    public Position getTo() {
        return to;
    }

    /** @return the promotion type, or {@code null} if this move is not a promotion. */
    public PieceType getPromotion() {
        return promotion;
    }

    /** @return {@code true} if this move carries a promotion. */
    public boolean isPromotion() {
        return promotion != null;
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
        return from.equals(other.from)
                && to.equals(other.to)
                && promotion == other.promotion;
    }

    @Override
    public int hashCode() {
        int result = from.hashCode();
        result = 31 * result + to.hashCode();
        result = 31 * result + (promotion == null ? 0 : promotion.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Move(" + from + " -> " + to
                + (promotion == null ? "" : ", =" + promotion) + ")";
    }
}
