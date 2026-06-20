package com.finesse.variant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.finesse.core.Board;
import com.finesse.core.GameState;
import com.finesse.core.MoveGenerator;
import com.finesse.core.Piece;
import com.finesse.core.PieceColor;
import com.finesse.core.PieceType;
import com.finesse.core.Position;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link FinesseMoveGenerator}: one focused test per piece type covering
 * normal moves, captures, blocked paths and board-edge constraints, plus a slot for
 * the finesse-specific mechanic.
 *
 * <p>RULES.md (owned by Task 2) defines the variant's UNIQUE movement rules and was
 * not present on this branch when these tests were written. The per-piece expected
 * move sets below therefore encode STANDARD chess movement as a documented baseline
 * and are each marked {@code // ASSUMPTION:}. Where Finesse alters a piece's movement,
 * replace the expected set for that piece. The {@code finesseSpecificMechanic} test is
 * an explicit placeholder to be filled in from RULES.md.
 *
 * <p>API ASSUMPTIONS (Task 1/2 own these; reconcile names at integration):
 * <ul>
 *   <li>{@code new Board(int files, int ranks)}; standard board is 8x8.</li>
 *   <li>{@code new GameState(Board board, PieceColor sideToMove)}.</li>
 *   <li>{@code MoveGenerator#legalMoves(GameState, Position)} returns
 *       {@code List<Position>} of destination squares (not a Move wrapper).</li>
 *   <li>{@code new FinesseMoveGenerator()} (no-arg).</li>
 * </ul>
 */
public class FinesseMoveGeneratorTest {

    private MoveGenerator gen;

    @Before
    public void setUp() {
        gen = new FinesseMoveGenerator();
    }

    private static Position pos(int file, int rank) {
        return new Position(file, rank);
    }

    /** Empty 8x8 board wrapped in a GameState for the given side. */
    private static GameState emptyState(PieceColor sideToMove) {
        return new GameState(new Board(8, 8), sideToMove);
    }

    private Set<Position> moves(GameState state, Position from) {
        List<Position> list = gen.legalMoves(state, from);
        assertNotNull("legalMoves must never return null", list);
        return new HashSet<>(list);
    }

    // ---- ROOK -----------------------------------------------------------------

    @Test
    public void rookOnEmptyBoardSlidesRankAndFile() {
        GameState state = emptyState(PieceColor.WHITE);
        Position from = pos(3, 3);
        state.getBoard().setPiece(from, new Piece(PieceType.ROOK, PieceColor.WHITE));

        // ASSUMPTION: standard rook movement (orthogonal sliding, full rays).
        Set<Position> expected = new HashSet<>();
        for (int f = 0; f < 8; f++) {
            if (f != 3) {
                expected.add(pos(f, 3));
            }
        }
        for (int r = 0; r < 8; r++) {
            if (r != 3) {
                expected.add(pos(3, r));
            }
        }
        assertEquals(expected, moves(state, from));
    }

    @Test
    public void rookStopsAtFriendlyAndCapturesEnemy() {
        GameState state = emptyState(PieceColor.WHITE);
        Position from = pos(0, 0);
        Board b = state.getBoard();
        b.setPiece(from, new Piece(PieceType.ROOK, PieceColor.WHITE));
        // Friendly blocker up the file at (0,3): rook may reach (0,1),(0,2) only.
        b.setPiece(pos(0, 3), new Piece(PieceType.PAWN, PieceColor.WHITE));
        // Enemy along the rank at (3,0): rook may reach (1,0),(2,0) and capture (3,0).
        b.setPiece(pos(3, 0), new Piece(PieceType.PAWN, PieceColor.BLACK));

        Set<Position> result = moves(state, from);
        // ASSUMPTION: standard blocking/capture semantics.
        assertTrue(result.contains(pos(0, 1)));
        assertTrue(result.contains(pos(0, 2)));
        assertFalse("cannot land on friendly piece", result.contains(pos(0, 3)));
        assertFalse("cannot pass through friendly piece", result.contains(pos(0, 4)));
        assertTrue(result.contains(pos(1, 0)));
        assertTrue(result.contains(pos(2, 0)));
        assertTrue("may capture enemy", result.contains(pos(3, 0)));
        assertFalse("cannot pass through captured enemy", result.contains(pos(4, 0)));
    }

    // ---- BISHOP ---------------------------------------------------------------

    @Test
    public void bishopOnEmptyBoardSlidesDiagonals() {
        GameState state = emptyState(PieceColor.WHITE);
        Position from = pos(2, 2);
        state.getBoard().setPiece(from, new Piece(PieceType.BISHOP, PieceColor.WHITE));

        // ASSUMPTION: standard bishop movement (diagonal sliding).
        Set<Position> result = moves(state, from);
        assertTrue(result.contains(pos(3, 3)));
        assertTrue(result.contains(pos(7, 7)));
        assertTrue(result.contains(pos(0, 0)));
        assertTrue(result.contains(pos(1, 3)));
        assertTrue(result.contains(pos(3, 1)));
        assertFalse("bishop does not move orthogonally", result.contains(pos(2, 3)));
        assertFalse("bishop does not move orthogonally", result.contains(pos(3, 2)));
    }

    @Test
    public void bishopBlockedByFriendlyOnDiagonal() {
        GameState state = emptyState(PieceColor.WHITE);
        Position from = pos(0, 0);
        Board b = state.getBoard();
        b.setPiece(from, new Piece(PieceType.BISHOP, PieceColor.WHITE));
        b.setPiece(pos(2, 2), new Piece(PieceType.PAWN, PieceColor.WHITE));

        Set<Position> result = moves(state, from);
        // ASSUMPTION: standard sliding blocked by friendly piece.
        assertTrue(result.contains(pos(1, 1)));
        assertFalse("cannot land on friendly", result.contains(pos(2, 2)));
        assertFalse("cannot pass through friendly", result.contains(pos(3, 3)));
    }

    // ---- KNIGHT ---------------------------------------------------------------

    @Test
    public void knightInCenterHasEightMoves() {
        GameState state = emptyState(PieceColor.WHITE);
        Position from = pos(3, 3);
        state.getBoard().setPiece(from, new Piece(PieceType.KNIGHT, PieceColor.WHITE));

        // ASSUMPTION: standard knight L-shaped movement.
        Set<Position> expected = new HashSet<>();
        int[][] deltas = {{1, 2}, {2, 1}, {-1, 2}, {-2, 1}, {1, -2}, {2, -1}, {-1, -2}, {-2, -1}};
        for (int[] d : deltas) {
            expected.add(pos(3 + d[0], 3 + d[1]));
        }
        assertEquals(expected, moves(state, from));
    }

    @Test
    public void knightInCornerIsConstrainedByEdges() {
        GameState state = emptyState(PieceColor.WHITE);
        Position from = pos(0, 0);
        state.getBoard().setPiece(from, new Piece(PieceType.KNIGHT, PieceColor.WHITE));

        // ASSUMPTION: standard knight; from a1 only b3 and c2 stay in bounds.
        Set<Position> expected = new HashSet<>();
        expected.add(pos(1, 2));
        expected.add(pos(2, 1));
        assertEquals(expected, moves(state, from));
    }

    @Test
    public void knightCanCaptureEnemyButNotFriendly() {
        GameState state = emptyState(PieceColor.WHITE);
        Position from = pos(3, 3);
        Board b = state.getBoard();
        b.setPiece(from, new Piece(PieceType.KNIGHT, PieceColor.WHITE));
        b.setPiece(pos(4, 5), new Piece(PieceType.PAWN, PieceColor.BLACK)); // enemy target
        b.setPiece(pos(5, 4), new Piece(PieceType.PAWN, PieceColor.WHITE)); // friendly target

        Set<Position> result = moves(state, from);
        assertTrue("knight may capture enemy on its target square", result.contains(pos(4, 5)));
        assertFalse("knight may not land on a friendly piece", result.contains(pos(5, 4)));
    }

    // ---- QUEEN ----------------------------------------------------------------

    @Test
    public void queenCombinesRookAndBishopRays() {
        GameState state = emptyState(PieceColor.WHITE);
        Position from = pos(3, 3);
        state.getBoard().setPiece(from, new Piece(PieceType.QUEEN, PieceColor.WHITE));

        // ASSUMPTION: standard queen movement (orthogonal + diagonal sliding).
        Set<Position> result = moves(state, from);
        assertTrue(result.contains(pos(3, 7))); // file
        assertTrue(result.contains(pos(7, 3))); // rank
        assertTrue(result.contains(pos(7, 7))); // diagonal
        assertTrue(result.contains(pos(0, 0))); // diagonal
        assertFalse("queen does not jump like a knight", result.contains(pos(4, 5)));
    }

    @Test
    public void queenIsBlockedAndCaptures() {
        GameState state = emptyState(PieceColor.WHITE);
        Position from = pos(3, 3);
        Board b = state.getBoard();
        b.setPiece(from, new Piece(PieceType.QUEEN, PieceColor.WHITE));
        b.setPiece(pos(3, 5), new Piece(PieceType.PAWN, PieceColor.BLACK)); // enemy up the file
        b.setPiece(pos(5, 3), new Piece(PieceType.PAWN, PieceColor.WHITE)); // friendly along rank

        Set<Position> result = moves(state, from);
        // ASSUMPTION: standard sliding semantics.
        assertTrue(result.contains(pos(3, 4)));
        assertTrue("captures enemy", result.contains(pos(3, 5)));
        assertFalse("stops past captured enemy", result.contains(pos(3, 6)));
        assertTrue(result.contains(pos(4, 3)));
        assertFalse("stops before friendly", result.contains(pos(5, 3)));
    }

    // ---- KING -----------------------------------------------------------------

    @Test
    public void kingInCenterHasEightSteps() {
        GameState state = emptyState(PieceColor.WHITE);
        Position from = pos(4, 4);
        state.getBoard().setPiece(from, new Piece(PieceType.KING, PieceColor.WHITE));

        // ASSUMPTION: standard king single-step movement (ignoring check rules here).
        Set<Position> expected = new HashSet<>();
        for (int df = -1; df <= 1; df++) {
            for (int dr = -1; dr <= 1; dr++) {
                if (df != 0 || dr != 0) {
                    expected.add(pos(4 + df, 4 + dr));
                }
            }
        }
        assertEquals(expected, moves(state, from));
    }

    @Test
    public void kingInCornerIsConstrainedByEdges() {
        GameState state = emptyState(PieceColor.WHITE);
        Position from = pos(0, 0);
        state.getBoard().setPiece(from, new Piece(PieceType.KING, PieceColor.WHITE));

        // ASSUMPTION: standard king; from a1 only b1, a2, b2 stay in bounds.
        Set<Position> expected = new HashSet<>();
        expected.add(pos(1, 0));
        expected.add(pos(0, 1));
        expected.add(pos(1, 1));
        assertEquals(expected, moves(state, from));
    }

    // ---- PAWN -----------------------------------------------------------------

    @Test
    public void whitePawnAdvancesOneOrTwoFromStartRank() {
        GameState state = emptyState(PieceColor.WHITE);
        Position from = pos(4, 1);
        state.getBoard().setPiece(from, new Piece(PieceType.PAWN, PieceColor.WHITE));

        Set<Position> result = moves(state, from);
        // ASSUMPTION: White pawns advance toward higher ranks; double-step from rank 1.
        assertTrue(result.contains(pos(4, 2)));
        assertTrue(result.contains(pos(4, 3)));
        assertFalse("pawn does not capture straight ahead onto empty diagonals", result.contains(pos(3, 2)));
        assertFalse(result.contains(pos(5, 2)));
    }

    @Test
    public void whitePawnCapturesDiagonallyOnly() {
        GameState state = emptyState(PieceColor.WHITE);
        Position from = pos(4, 1);
        Board b = state.getBoard();
        b.setPiece(from, new Piece(PieceType.PAWN, PieceColor.WHITE));
        b.setPiece(pos(3, 2), new Piece(PieceType.PAWN, PieceColor.BLACK)); // capturable
        b.setPiece(pos(5, 2), new Piece(PieceType.PAWN, PieceColor.BLACK)); // capturable

        Set<Position> result = moves(state, from);
        // ASSUMPTION: standard pawn diagonal capture.
        assertTrue(result.contains(pos(3, 2)));
        assertTrue(result.contains(pos(5, 2)));
        assertTrue("forward advance still available", result.contains(pos(4, 2)));
    }

    @Test
    public void pawnIsBlockedByPieceDirectlyAhead() {
        GameState state = emptyState(PieceColor.WHITE);
        Position from = pos(4, 1);
        Board b = state.getBoard();
        b.setPiece(from, new Piece(PieceType.PAWN, PieceColor.WHITE));
        b.setPiece(pos(4, 2), new Piece(PieceType.PAWN, PieceColor.BLACK)); // blocks both steps

        Set<Position> result = moves(state, from);
        // ASSUMPTION: a pawn cannot advance (single or double) through a piece directly ahead.
        assertFalse("blocked single step", result.contains(pos(4, 2)));
        assertFalse("blocked double step", result.contains(pos(4, 3)));
    }

    // ---- Edge / robustness ----------------------------------------------------

    @Test
    public void emptySquareYieldsNoMoves() {
        GameState state = emptyState(PieceColor.WHITE);
        assertTrue("no piece -> no moves", moves(state, pos(4, 4)).isEmpty());
    }

    @Test
    public void noGeneratedMoveLeavesTheBoard() {
        GameState state = emptyState(PieceColor.WHITE);
        Position from = pos(7, 7);
        state.getBoard().setPiece(from, new Piece(PieceType.QUEEN, PieceColor.WHITE));

        Board b = state.getBoard();
        for (Position to : moves(state, from)) {
            assertTrue("every generated move must be in bounds: " + to, b.isInBounds(to));
        }
    }

    // ---- Finesse-specific mechanic -------------------------------------------

    @Test
    public void finesseSpecificMechanic() {
        // ASSUMPTION / PLACEHOLDER: RULES.md was not available on this branch, so the
        // variant's signature "finesse" mechanic could not be encoded. Once RULES.md is
        // integrated, replace this body with a focused test of that mechanic (e.g. the
        // special move/capture rule that distinguishes Finesse from standard chess).
        //
        // It currently asserts only that the generator is wired up and returns a
        // non-null result for a representative position, so the suite compiles and runs.
        GameState state = emptyState(PieceColor.WHITE);
        Position from = pos(4, 1);
        state.getBoard().setPiece(from, new Piece(PieceType.PAWN, PieceColor.WHITE));
        assertNotNull(gen.legalMoves(state, from));
    }
}
