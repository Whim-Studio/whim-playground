package com.xiangqi.core;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Tests for the {@code com.xiangqi.core} Xiangqi engine. */
public class CoreTest {

    // --- helpers ------------------------------------------------------

    /** An empty board wrapped in a GameState with no pieces but the given side to move. */
    private GameState empty(Side toMove) {
        return state(new Board(), toMove);
    }

    /** Build a GameState around a given board via reflection-free construction. */
    private GameState state(Board b, Side toMove) {
        // Place generals if missing is the caller's responsibility; we expose the
        // package-private path by going through a fresh initial() then swapping.
        return GameStateTestAccess.of(b, toMove);
    }

    private boolean containsMove(List<Move> moves, int fr, int fc, int tr, int tc) {
        Move target = new Move(Position.of(fr, fc), Position.of(tr, tc));
        for (Move m : moves) {
            if (m.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private List<Move> movesFrom(GameState s, int r, int c) {
        List<Move> out = new ArrayList<Move>();
        for (Move m : s.legalMoves()) {
            if (m.from().equals(Position.of(r, c))) {
                out.add(m);
            }
        }
        return out;
    }

    // --- value types --------------------------------------------------

    @Test
    public void sideForwardAndOpponent() {
        assertEquals(-1, Side.RED.forward());
        assertEquals(1, Side.BLACK.forward());
        assertEquals(Side.BLACK, Side.RED.opponent());
        assertEquals(Side.RED, Side.BLACK.opponent());
    }

    @Test
    public void positionPalaceAndRiver() {
        assertTrue(Position.of(9, 4).isInPalace(Side.RED));
        assertTrue(Position.of(0, 4).isInPalace(Side.BLACK));
        assertFalse(Position.of(6, 4).isInPalace(Side.RED));
        assertFalse(Position.of(9, 2).isInPalace(Side.RED));
        // RED crosses river when row <= 4; BLACK when row >= 5.
        assertTrue(Position.of(4, 0).isAcrossRiver(Side.RED));
        assertFalse(Position.of(5, 0).isAcrossRiver(Side.RED));
        assertTrue(Position.of(5, 0).isAcrossRiver(Side.BLACK));
        assertFalse(Position.of(4, 0).isAcrossRiver(Side.BLACK));
    }

    @Test
    public void valueEquality() {
        assertEquals(Position.of(3, 4), Position.of(3, 4));
        assertEquals(Position.of(3, 4).hashCode(), Position.of(3, 4).hashCode());
        assertEquals(new Piece(Side.RED, PieceType.HORSE), new Piece(Side.RED, PieceType.HORSE));
        assertEquals(new Move(Position.of(0, 0), Position.of(1, 0)),
                new Move(Position.of(0, 0), Position.of(1, 0)));
    }

    // --- opening ------------------------------------------------------

    @Test
    public void redOpeningMoveCount() {
        GameState s = GameState.initial();
        assertEquals(Side.RED, s.sideToMove());
        // Standard Xiangqi opening: RED has exactly 44 legal moves.
        assertEquals(44, s.legalMoves().size());
    }

    @Test
    public void openingHasNoCheck() {
        GameState s = GameState.initial();
        assertFalse(s.isInCheck(Side.RED));
        assertFalse(s.isInCheck(Side.BLACK));
        assertFalse(s.isGameOver());
        assertNull(s.winner());
    }

    // --- General ------------------------------------------------------

    @Test
    public void generalConfinedToPalace() {
        Board b = new Board();
        b.set(Position.of(9, 4), new Piece(Side.RED, PieceType.GENERAL));
        b.set(Position.of(0, 8), new Piece(Side.BLACK, PieceType.GENERAL)); // off file 4: no facing
        GameState s = state(b, Side.RED);
        List<Move> gm = movesFrom(s, 9, 4);
        // From the center of the back palace edge: up, left, right (down is off-board).
        assertTrue(containsMove(gm, 9, 4, 8, 4));
        assertTrue(containsMove(gm, 9, 4, 9, 3));
        assertTrue(containsMove(gm, 9, 4, 9, 5));
        assertEquals(3, gm.size());
    }

    @Test
    public void flyingGeneralForbidsFacing() {
        Board b = new Board();
        // Generals on the same file, nothing between -> RED may not step onto file 4 clear.
        b.set(Position.of(9, 3), new Piece(Side.RED, PieceType.GENERAL));
        b.set(Position.of(0, 4), new Piece(Side.BLACK, PieceType.GENERAL));
        GameState s = state(b, Side.RED);
        // Moving RED general from (9,3) to (9,4) would face the BLACK general directly.
        assertFalse(containsMove(s.legalMoves(), 9, 3, 9, 4));
        // But sliding sideways stays legal.
        assertTrue(containsMove(s.legalMoves(), 9, 3, 8, 3));
    }

    @Test
    public void flyingGeneralCountsAsCheck() {
        Board b = new Board();
        b.set(Position.of(9, 4), new Piece(Side.RED, PieceType.GENERAL));
        b.set(Position.of(0, 4), new Piece(Side.BLACK, PieceType.GENERAL));
        GameState s = state(b, Side.RED);
        assertTrue(s.isInCheck(Side.RED));
        assertTrue(s.isInCheck(Side.BLACK));
    }

    // --- Advisor ------------------------------------------------------

    @Test
    public void advisorDiagonalInPalace() {
        Board b = new Board();
        b.set(Position.of(8, 4), new Piece(Side.RED, PieceType.ADVISOR)); // palace center: 4 diagonals
        b.set(Position.of(9, 4), new Piece(Side.RED, PieceType.GENERAL));
        b.set(Position.of(0, 8), new Piece(Side.BLACK, PieceType.GENERAL));
        GameState s = state(b, Side.RED);
        List<Move> am = movesFrom(s, 8, 4);
        assertEquals(4, am.size());
        assertTrue(containsMove(am, 8, 4, 9, 3));
        assertTrue(containsMove(am, 8, 4, 7, 5));
    }

    // --- Elephant -----------------------------------------------------

    @Test
    public void elephantCannotCrossRiver() {
        Board b = new Board();
        b.set(Position.of(5, 2), new Piece(Side.RED, PieceType.ELEPHANT));
        b.set(Position.of(9, 0), new Piece(Side.RED, PieceType.GENERAL));
        b.set(Position.of(0, 8), new Piece(Side.BLACK, PieceType.GENERAL));
        GameState s = state(b, Side.RED);
        List<Move> em = movesFrom(s, 5, 2);
        // From (5,2): (7,0),(7,4) are valid; (3,0),(3,4) cross the river -> forbidden.
        assertTrue(containsMove(em, 5, 2, 7, 0));
        assertTrue(containsMove(em, 5, 2, 7, 4));
        assertFalse(containsMove(em, 5, 2, 3, 0));
        assertFalse(containsMove(em, 5, 2, 3, 4));
    }

    @Test
    public void elephantEyeBlock() {
        Board b = new Board();
        b.set(Position.of(7, 2), new Piece(Side.RED, PieceType.ELEPHANT));
        b.set(Position.of(8, 3), new Piece(Side.RED, PieceType.SOLDIER)); // blocks the eye toward (9,4)/(5,4)... toward (5,4) eye is (6,3)
        b.set(Position.of(6, 3), new Piece(Side.BLACK, PieceType.SOLDIER)); // eye toward (5,4)
        b.set(Position.of(9, 5), new Piece(Side.RED, PieceType.GENERAL)); // off elephant targets
        b.set(Position.of(0, 8), new Piece(Side.BLACK, PieceType.GENERAL));
        GameState s = state(b, Side.RED);
        // toward (5,4): eye (6,3) occupied -> blocked.
        assertFalse(containsMove(s.legalMoves(), 7, 2, 5, 4));
        // toward (9,4): eye (8,3) occupied -> blocked.
        assertFalse(containsMove(s.legalMoves(), 7, 2, 9, 4));
        // toward (9,0): eye (8,1) empty -> allowed.
        assertTrue(containsMove(s.legalMoves(), 7, 2, 9, 0));
    }

    // --- Horse --------------------------------------------------------

    @Test
    public void horseLegBlock() {
        Board b = new Board();
        b.set(Position.of(5, 4), new Piece(Side.RED, PieceType.HORSE));
        b.set(Position.of(9, 0), new Piece(Side.RED, PieceType.GENERAL));
        b.set(Position.of(0, 8), new Piece(Side.BLACK, PieceType.GENERAL));
        GameState open = state(b, Side.RED);
        assertEquals(8, movesFrom(open, 5, 4).size());

        // Block the leg directly above (4,4): removes the two landings (3,3),(3,5).
        b.set(Position.of(4, 4), new Piece(Side.RED, PieceType.SOLDIER));
        GameState blocked = state(b, Side.RED);
        assertFalse(containsMove(blocked.legalMoves(), 5, 4, 3, 3));
        assertFalse(containsMove(blocked.legalMoves(), 5, 4, 3, 5));
        // Other directions remain.
        assertTrue(containsMove(blocked.legalMoves(), 5, 4, 7, 3));
    }

    // --- Chariot ------------------------------------------------------

    @Test
    public void chariotSlidesAndStopsAtCapture() {
        Board b = new Board();
        b.set(Position.of(5, 0), new Piece(Side.RED, PieceType.CHARIOT));
        b.set(Position.of(5, 4), new Piece(Side.BLACK, PieceType.SOLDIER));
        b.set(Position.of(9, 8), new Piece(Side.RED, PieceType.GENERAL));
        b.set(Position.of(0, 4), new Piece(Side.BLACK, PieceType.GENERAL));
        GameState s = state(b, Side.RED);
        assertTrue(containsMove(s.legalMoves(), 5, 0, 5, 3));   // slide up to enemy
        assertTrue(containsMove(s.legalMoves(), 5, 0, 5, 4));   // capture enemy
        assertFalse(containsMove(s.legalMoves(), 5, 0, 5, 5));  // cannot pass through
    }

    // --- Cannon -------------------------------------------------------

    @Test
    public void cannonNeedsExactlyOneScreenToCapture() {
        Board b = new Board();
        b.set(Position.of(5, 0), new Piece(Side.RED, PieceType.CANNON));
        b.set(Position.of(5, 2), new Piece(Side.RED, PieceType.SOLDIER));   // screen
        b.set(Position.of(5, 5), new Piece(Side.BLACK, PieceType.SOLDIER)); // target
        b.set(Position.of(5, 7), new Piece(Side.BLACK, PieceType.HORSE));   // beyond -> two screens
        b.set(Position.of(9, 4), new Piece(Side.RED, PieceType.GENERAL));
        b.set(Position.of(0, 8), new Piece(Side.BLACK, PieceType.GENERAL));
        GameState s = state(b, Side.RED);
        // Non-capturing slide stops before screen.
        assertTrue(containsMove(s.legalMoves(), 5, 0, 5, 1));
        assertFalse(containsMove(s.legalMoves(), 5, 0, 5, 2)); // cannot land on own screen
        // Captures the first enemy past exactly one screen.
        assertTrue(containsMove(s.legalMoves(), 5, 0, 5, 5));
        // Cannot capture the one beyond two pieces.
        assertFalse(containsMove(s.legalMoves(), 5, 0, 5, 7));
    }

    // --- Soldier ------------------------------------------------------

    @Test
    public void soldierForwardOnlyBeforeRiver() {
        Board b = new Board();
        b.set(Position.of(6, 4), new Piece(Side.RED, PieceType.SOLDIER));
        b.set(Position.of(9, 0), new Piece(Side.RED, PieceType.GENERAL));
        b.set(Position.of(0, 8), new Piece(Side.BLACK, PieceType.GENERAL));
        GameState s = state(b, Side.RED);
        List<Move> sm = movesFrom(s, 6, 4);
        assertEquals(1, sm.size());
        assertTrue(containsMove(sm, 6, 4, 5, 4));
    }

    @Test
    public void soldierSidewaysAfterRiver() {
        Board b = new Board();
        b.set(Position.of(4, 4), new Piece(Side.RED, PieceType.SOLDIER)); // crossed for RED
        b.set(Position.of(9, 0), new Piece(Side.RED, PieceType.GENERAL));
        b.set(Position.of(0, 8), new Piece(Side.BLACK, PieceType.GENERAL));
        GameState s = state(b, Side.RED);
        List<Move> sm = movesFrom(s, 4, 4);
        assertEquals(3, sm.size());
        assertTrue(containsMove(sm, 4, 4, 3, 4)); // forward
        assertTrue(containsMove(sm, 4, 4, 4, 3)); // sideways
        assertTrue(containsMove(sm, 4, 4, 4, 5)); // sideways
        assertFalse(containsMove(sm, 4, 4, 5, 4)); // never backward
    }

    // --- check / checkmate --------------------------------------------

    @Test
    public void detectsCheck() {
        Board b = new Board();
        b.set(Position.of(9, 4), new Piece(Side.RED, PieceType.GENERAL));
        b.set(Position.of(0, 8), new Piece(Side.BLACK, PieceType.GENERAL));
        b.set(Position.of(9, 0), new Piece(Side.BLACK, PieceType.CHARIOT)); // attacks along row 9
        GameState s = state(b, Side.RED);
        assertTrue(s.isInCheck(Side.RED));
        // Every legal RED move must resolve the check.
        for (Move m : s.legalMoves()) {
            assertFalse(s.apply(m).isInCheck(Side.RED));
        }
    }

    @Test
    public void checkmate() {
        Board b = new Board();
        b.set(Position.of(9, 4), new Piece(Side.RED, PieceType.GENERAL));
        b.set(Position.of(0, 0), new Piece(Side.BLACK, PieceType.GENERAL));
        // Two chariots deliver a back-rank style mate in the palace.
        b.set(Position.of(8, 3), new Piece(Side.BLACK, PieceType.CHARIOT)); // guards file 3 & row 8
        b.set(Position.of(0, 4), new Piece(Side.BLACK, PieceType.CHARIOT)); // checks down file 4
        b.set(Position.of(0, 5), new Piece(Side.BLACK, PieceType.CHARIOT)); // guards file 5
        GameState s = state(b, Side.RED);
        assertTrue(s.isInCheck(Side.RED));
        assertTrue(s.isCheckmate(Side.RED));
        assertTrue(s.isGameOver());
        assertEquals(Side.BLACK, s.winner());
    }

    @Test
    public void stalemateIsLossForSideToMove() {
        Board b = new Board();
        // RED general boxed with no legal move but NOT in check.
        b.set(Position.of(9, 4), new Piece(Side.RED, PieceType.GENERAL));
        b.set(Position.of(0, 0), new Piece(Side.BLACK, PieceType.GENERAL));
        // Black chariots cover the escape squares (8,4),(9,3),(9,5) without checking (9,4).
        b.set(Position.of(8, 0), new Piece(Side.BLACK, PieceType.CHARIOT)); // covers row 8 -> (8,4)
        b.set(Position.of(0, 3), new Piece(Side.BLACK, PieceType.CHARIOT)); // covers file 3 -> (9,3)
        b.set(Position.of(0, 5), new Piece(Side.BLACK, PieceType.CHARIOT)); // covers file 5 -> (9,5)
        GameState s = state(b, Side.RED);
        assertFalse(s.isInCheck(Side.RED));
        assertTrue(s.legalMoves().isEmpty());
        assertTrue(s.isStalemate(Side.RED));
        assertEquals(Side.BLACK, s.winner());
    }

    // --- apply / history ----------------------------------------------

    @Test
    public void applyProducesNewStateAndHistory() {
        GameState s = GameState.initial();
        Move m = new Move(Position.of(6, 4), Position.of(5, 4)); // central soldier up
        GameState next = s.apply(m);
        assertEquals(Side.BLACK, next.sideToMove());
        assertNull(next.board().pieceAt(Position.of(6, 4)));
        assertEquals(PieceType.SOLDIER, next.board().pieceAt(Position.of(5, 4)).type());
        // Original unchanged (immutability).
        assertEquals(PieceType.SOLDIER, s.board().pieceAt(Position.of(6, 4)).type());
        assertNull(s.board().pieceAt(Position.of(5, 4)));
        // History grew by one.
        assertEquals(s.history().size() + 1, next.history().size());
    }
}
