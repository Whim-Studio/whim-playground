package com.finesse.core;

import java.util.List;

/**
 * Generates the legal moves for a piece, according to the rules of a particular
 * variant.
 *
 * <p>This interface is the contract between the core engine and any variant. The
 * core deliberately provides <em>no</em> implementation: Finesse-specific (or
 * any other) movement and legality rules live in the variant module, which
 * supplies a concrete {@code MoveGenerator}. This keeps the core free of rule
 * assumptions and lets multiple variants reuse the same board and state types.
 */
public interface MoveGenerator {

    /**
     * Returns every legal move for the piece standing on {@code from}, given the
     * current game state.
     *
     * <p>Implementations decide what "legal" means for their variant (including,
     * for example, whether moves that leave one's own king in check are
     * excluded). If {@code from} is empty, off the board, or holds a piece not
     * belonging to the side to move, implementations should return an empty list
     * rather than throwing.
     *
     * @param state the current game state; must not be {@code null}
     * @param from  the square of the piece whose moves are requested
     * @return an immutable or freshly-allocated list of legal {@link Move}s;
     *         never {@code null}, possibly empty
     */
    List<Move> legalMoves(GameState state, Position from);
}
