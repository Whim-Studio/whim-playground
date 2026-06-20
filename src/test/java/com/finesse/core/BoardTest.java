package com.finesse.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for the core {@link Board} contract: bounds checking, set/get/remove,
 * deep-copy independence, and non-8x8 dimension handling.
 *
 * <p>API ASSUMPTIONS (Task 1 owns these classes; reconcile names at integration):
 * <ul>
 *   <li>{@code new Board(int files, int ranks)} — constructor takes (files, ranks).</li>
 *   <li>{@code Position(int file, int rank)} with value-based {@code equals}.</li>
 *   <li>{@code Piece(PieceType, PieceColor)} with {@code getType()} / {@code getColor()}.</li>
 *   <li>{@code Board#getPiece/setPiece/removePiece/isInBounds/copy} per the shared contract.</li>
 *   <li>An empty square returns {@code null} from {@code getPiece}.</li>
 * </ul>
 */
public class BoardTest {

    private static Board newStandardBoard() {
        // ASSUMPTION: standard board is 8 files x 8 ranks.
        return new Board(8, 8);
    }

    private static Piece whitePawn() {
        return new Piece(PieceType.PAWN, PieceColor.WHITE);
    }

    @Test
    public void cornersAndCenterAreInBounds() {
        Board board = newStandardBoard();
        assertTrue(board.isInBounds(new Position(0, 0)));
        assertTrue(board.isInBounds(new Position(7, 7)));
        assertTrue(board.isInBounds(new Position(0, 7)));
        assertTrue(board.isInBounds(new Position(7, 0)));
        assertTrue(board.isInBounds(new Position(4, 3)));
    }

    @Test
    public void negativeCoordinatesAreOutOfBounds() {
        Board board = newStandardBoard();
        assertFalse(board.isInBounds(new Position(-1, 0)));
        assertFalse(board.isInBounds(new Position(0, -1)));
        assertFalse(board.isInBounds(new Position(-1, -1)));
    }

    @Test
    public void coordinatesAtOrBeyondDimensionAreOutOfBounds() {
        Board board = newStandardBoard();
        assertFalse(board.isInBounds(new Position(8, 0)));
        assertFalse(board.isInBounds(new Position(0, 8)));
        assertFalse(board.isInBounds(new Position(8, 8)));
        assertFalse(board.isInBounds(new Position(100, 100)));
    }

    @Test
    public void setThenGetReturnsSamePiece() {
        Board board = newStandardBoard();
        Position p = new Position(3, 4);
        Piece piece = whitePawn();
        board.setPiece(p, piece);

        Piece got = board.getPiece(p);
        assertNotNull(got);
        assertEquals(PieceType.PAWN, got.getType());
        assertEquals(PieceColor.WHITE, got.getColor());
    }

    @Test
    public void emptySquareReturnsNull() {
        Board board = newStandardBoard();
        assertNull(board.getPiece(new Position(2, 2)));
    }

    @Test
    public void removeClearsTheSquare() {
        Board board = newStandardBoard();
        Position p = new Position(5, 5);
        board.setPiece(p, whitePawn());
        assertNotNull(board.getPiece(p));

        board.removePiece(p);
        assertNull(board.getPiece(p));
    }

    @Test
    public void setOverwritesExistingPiece() {
        Board board = newStandardBoard();
        Position p = new Position(1, 1);
        board.setPiece(p, whitePawn());
        board.setPiece(p, new Piece(PieceType.QUEEN, PieceColor.BLACK));

        Piece got = board.getPiece(p);
        assertEquals(PieceType.QUEEN, got.getType());
        assertEquals(PieceColor.BLACK, got.getColor());
    }

    @Test
    public void copyIsADistinctInstance() {
        Board board = newStandardBoard();
        Board copy = board.copy();
        assertNotNull(copy);
        assertNotSame(board, copy);
    }

    @Test
    public void copyPreservesExistingPieces() {
        Board board = newStandardBoard();
        Position p = new Position(6, 2);
        board.setPiece(p, whitePawn());

        Board copy = board.copy();
        Piece got = copy.getPiece(p);
        assertNotNull(got);
        assertEquals(PieceType.PAWN, got.getType());
        assertEquals(PieceColor.WHITE, got.getColor());
    }

    @Test
    public void mutatingCopyDoesNotAffectOriginal() {
        Board board = newStandardBoard();
        Position p = new Position(4, 4);
        board.setPiece(p, whitePawn());

        Board copy = board.copy();
        copy.setPiece(p, new Piece(PieceType.ROOK, PieceColor.BLACK));

        // Original must be untouched -> deep copy, not shared reference.
        assertEquals(PieceType.PAWN, board.getPiece(p).getType());
        assertEquals(PieceColor.WHITE, board.getPiece(p).getColor());
    }

    @Test
    public void mutatingOriginalDoesNotAffectCopy() {
        Board board = newStandardBoard();
        Position p = new Position(0, 0);
        board.setPiece(p, whitePawn());

        Board copy = board.copy();
        board.removePiece(p);

        assertNull(board.getPiece(p));
        assertNotNull("copy should retain its own piece after original mutates", copy.getPiece(p));
    }

    @Test
    public void nonSquareBoardRespectsItsOwnDimensions() {
        // A wide, short board: 10 files x 4 ranks.
        Board board = new Board(10, 4);
        assertTrue(board.isInBounds(new Position(9, 3)));
        assertTrue(board.isInBounds(new Position(0, 0)));
        assertFalse(board.isInBounds(new Position(10, 0)));
        assertFalse(board.isInBounds(new Position(0, 4)));
        assertFalse(board.isInBounds(new Position(9, 4)));
    }

    @Test
    public void nonSquareBoardStoresAndRetrievesAtExtremes() {
        Board board = new Board(10, 4);
        Position far = new Position(9, 3);
        board.setPiece(far, new Piece(PieceType.KING, PieceColor.BLACK));
        assertEquals(PieceType.KING, board.getPiece(far).getType());
    }

    @Test
    public void tallNarrowBoardHandlesBounds() {
        // 3 files x 12 ranks.
        Board board = new Board(3, 12);
        assertTrue(board.isInBounds(new Position(2, 11)));
        assertFalse(board.isInBounds(new Position(3, 11)));
        assertFalse(board.isInBounds(new Position(2, 12)));
    }
}
