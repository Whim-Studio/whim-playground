package com.finesse.variant;

import com.finesse.core.Board;
import com.finesse.core.GameState;
import com.finesse.core.Piece;
import com.finesse.core.PieceColor;
import com.finesse.core.PieceType;
import com.finesse.core.Position;

/**
 * Builds the starting {@link GameState} for Finesse Chess.
 *
 * <p>See {@code RULES.md} for the authoritative documentation. In short: an 8x8
 * board with the standard chess layout except that the bishops are replaced by
 * the variant {@link PieceType#FINESSE} piece on files 2 and 5.
 */
public final class FinesseSetup {

    /** Board dimensions for the variant. */
    public static final int FILES = 8;
    public static final int RANKS = 8;

    private FinesseSetup() {
    }

    /**
     * @return a fresh {@link GameState} with all pieces placed in their starting
     *         squares and White to move.
     */
    public static GameState newGame() {
        Board board = new Board(FILES, RANKS);

        // Back-rank order, files 0..7. Bishops -> FINESSE.
        PieceType[] backRank = {
            PieceType.ROOK,
            PieceType.KNIGHT,
            PieceType.FINESSE,
            PieceType.QUEEN,
            PieceType.KING,
            PieceType.FINESSE,
            PieceType.KNIGHT,
            PieceType.ROOK,
        };

        for (int file = 0; file < FILES; file++) {
            // White on ranks 0 (back) and 1 (pawns).
            board.setPiece(new Position(file, 0),
                    new Piece(backRank[file], PieceColor.WHITE));
            board.setPiece(new Position(file, 1),
                    new Piece(PieceType.PAWN, PieceColor.WHITE));

            // Black on ranks 7 (back) and 6 (pawns).
            board.setPiece(new Position(file, 7),
                    new Piece(backRank[file], PieceColor.BLACK));
            board.setPiece(new Position(file, 6),
                    new Piece(PieceType.PAWN, PieceColor.BLACK));
        }

        return new GameState(board, PieceColor.WHITE);
    }
}
