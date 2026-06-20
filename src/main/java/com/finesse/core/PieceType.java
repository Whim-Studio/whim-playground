package com.finesse.core;

/**
 * The kinds of pieces that can appear on the board.
 *
 * <p>The six standard chess piece types form the baseline. The Finesse variant
 * (or any other variant) may require additional piece types &mdash; to add one,
 * simply append a new constant to this enum (for example {@code MARSHAL} or
 * {@code ARCHBISHOP}). Movement behaviour for variant types is <em>not</em>
 * encoded here; it lives in the {@link MoveGenerator} implementation supplied by
 * the variant module, which is free to switch on these constants. Keeping the
 * enum open this way means the core engine never needs to know the full piece
 * roster ahead of time.
 */
public enum PieceType {
    KING,
    QUEEN,
    ROOK,
    BISHOP,
    KNIGHT,
    PAWN,

    // Variant-specific types are appended below this line. The core makes no
    // assumption about the set of types beyond the six standard ones above;
    // movement behaviour lives in the variant's MoveGenerator.

    /**
     * The Finesse variant piece: moves like a bishop for quiet (non-capturing)
     * moves, but captures only via a knight's leap. Behaviour is defined in
     * {@code com.finesse.variant.FinesseMoveGenerator} (see {@code RULES.md}).
     */
    FINESSE;
}
