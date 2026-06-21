package com.janggi.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Rule coverage for the Janggi core engine. */
public class JanggiCoreTest {

    private static final SetupChoice MSSM = new SetupChoice(SetupChoice.Arrangement.MSSM);

    /** Empty board with a single piece, side to move = that piece's side. */
    private static GameState stateWith(Side toMove, Object... placements) {
        Board b = new Board();
        for (int i = 0; i < placements.length; i += 2) {
            Position pos = (Position) placements[i];
            Piece pc = (Piece) placements[i + 1];
            b.set(pos, pc);
        }
        return GameState.fromBoard(b, toMove);
    }

    private static boolean canMove(GameState gs, int fr, int fc, int tr, int tc) {
        Move m = new Move(Position.of(fr, fc), Position.of(tr, tc));
        for (Move legal : gs.legalMoves()) {
            if (legal.equals(m)) {
                return true;
            }
        }
        return false;
    }

    // ---- Setup / transposition ----

    @Test
    public void initialSetupPlacesMinorsPerArrangement() {
        GameState gs = GameState.initial(MSSM, MSSM);
        Board b = gs.board();
        // CHO home rank = row 9. MSSM => cols [1,2,6,7] = M,S,S,M.
        assertEquals(PieceType.HORSE, b.pieceAt(9, 1).type());
        assertEquals(PieceType.ELEPHANT, b.pieceAt(9, 2).type());
        assertEquals(PieceType.ELEPHANT, b.pieceAt(9, 6).type());
        assertEquals(PieceType.HORSE, b.pieceAt(9, 7).type());
        assertEquals(PieceType.GENERAL, b.pieceAt(8, 4).type());
        assertEquals(Side.CHO, gs.sideToMove());
    }

    @Test
    public void transpositionChangesMinorOrder() {
        GameState gs = GameState.initial(new SetupChoice(SetupChoice.Arrangement.SMSM), MSSM);
        Board b = gs.board();
        // SMSM => cols [1,2,6,7] = S,M,S,M.
        assertEquals(PieceType.ELEPHANT, b.pieceAt(9, 1).type());
        assertEquals(PieceType.HORSE, b.pieceAt(9, 2).type());
        assertEquals(PieceType.ELEPHANT, b.pieceAt(9, 6).type());
        assertEquals(PieceType.HORSE, b.pieceAt(9, 7).type());
    }

    // ---- Palace diagonals ----

    @Test
    public void generalMovesAlongPalaceDiagonal() {
        // CHO general at palace center (8,4); diagonal to corner (9,5) is legal.
        GameState gs = stateWith(Side.CHO,
                Position.of(8, 4), new Piece(Side.CHO, PieceType.GENERAL),
                Position.of(0, 0), new Piece(Side.HAN, PieceType.GENERAL));
        assertTrue(canMove(gs, 8, 4, 9, 5));
        assertTrue(canMove(gs, 8, 4, 7, 3));
        // Cannot leave the palace.
        assertFalse(canMove(gs, 8, 4, 8, 2));
    }

    @Test
    public void generalCannotMoveDiagonalFromEdgeMidpoint() {
        // From (8,3) (a non-diagonal palace point) a diagonal step is illegal.
        GameState gs = stateWith(Side.CHO,
                Position.of(8, 3), new Piece(Side.CHO, PieceType.GENERAL),
                Position.of(0, 0), new Piece(Side.HAN, PieceType.GENERAL));
        assertFalse(canMove(gs, 8, 3, 9, 4));
        assertFalse(canMove(gs, 8, 3, 7, 4));
        assertTrue(canMove(gs, 8, 3, 8, 4)); // orthogonal toward center is fine
    }

    // ---- Horse / Elephant leg-block ----

    @Test
    public void horseIsLegBlocked() {
        GameState open = stateWith(Side.CHO,
                Position.of(5, 4), new Piece(Side.CHO, PieceType.HORSE),
                Position.of(0, 0), new Piece(Side.HAN, PieceType.GENERAL),
                Position.of(9, 4), new Piece(Side.CHO, PieceType.GENERAL));
        assertTrue(canMove(open, 5, 4, 3, 3));  // via empty north leg (4,4)
        assertTrue(canMove(open, 5, 4, 3, 5));

        GameState blocked = stateWith(Side.CHO,
                Position.of(5, 4), new Piece(Side.CHO, PieceType.HORSE),
                Position.of(4, 4), new Piece(Side.HAN, PieceType.SOLDIER), // blocks north leg
                Position.of(0, 0), new Piece(Side.HAN, PieceType.GENERAL),
                Position.of(9, 4), new Piece(Side.CHO, PieceType.GENERAL));
        assertFalse(canMove(blocked, 5, 4, 3, 3));
        assertFalse(canMove(blocked, 5, 4, 3, 5));
    }

    @Test
    public void elephantIsLegBlocked() {
        GameState open = stateWith(Side.CHO,
                Position.of(5, 4), new Piece(Side.CHO, PieceType.ELEPHANT),
                Position.of(0, 0), new Piece(Side.HAN, PieceType.GENERAL),
                Position.of(9, 4), new Piece(Side.CHO, PieceType.GENERAL));
        assertTrue(canMove(open, 5, 4, 2, 2)); // north then NW-NW
        assertTrue(canMove(open, 5, 4, 2, 6));

        // Block the second leg point (4,3) for the north-west elephant move.
        GameState blocked = stateWith(Side.CHO,
                Position.of(5, 4), new Piece(Side.CHO, PieceType.ELEPHANT),
                Position.of(3, 3), new Piece(Side.HAN, PieceType.SOLDIER),
                Position.of(0, 0), new Piece(Side.HAN, PieceType.GENERAL),
                Position.of(9, 4), new Piece(Side.CHO, PieceType.GENERAL));
        assertFalse(canMove(blocked, 5, 4, 2, 2));
    }

    // ---- Cannon ----

    @Test
    public void cannonNeedsExactlyOneScreen() {
        // No screen between (5,0) and east -> cannot move east.
        GameState noScreen = stateWith(Side.CHO,
                Position.of(5, 0), new Piece(Side.CHO, PieceType.CANNON),
                Position.of(0, 0), new Piece(Side.HAN, PieceType.GENERAL),
                Position.of(9, 4), new Piece(Side.CHO, PieceType.GENERAL));
        assertFalse(canMove(noScreen, 5, 0, 5, 4));

        // One screen (a soldier) at (5,2) -> may jump to empty (5,5) and capture enemy at (5,6).
        GameState withScreen = stateWith(Side.CHO,
                Position.of(5, 0), new Piece(Side.CHO, PieceType.CANNON),
                Position.of(5, 2), new Piece(Side.HAN, PieceType.SOLDIER),
                Position.of(5, 6), new Piece(Side.HAN, PieceType.SOLDIER),
                Position.of(0, 0), new Piece(Side.HAN, PieceType.GENERAL),
                Position.of(9, 4), new Piece(Side.CHO, PieceType.GENERAL));
        assertTrue(canMove(withScreen, 5, 0, 5, 5));
        assertTrue(canMove(withScreen, 5, 0, 5, 6));
        assertFalse(canMove(withScreen, 5, 0, 5, 7)); // blocked beyond the captured piece
    }

    @Test
    public void cannonCannotJumpCannonScreenOrCaptureCannon() {
        // Screen is a Cannon -> illegal to use as screen.
        GameState cannonScreen = stateWith(Side.CHO,
                Position.of(5, 0), new Piece(Side.CHO, PieceType.CANNON),
                Position.of(5, 2), new Piece(Side.HAN, PieceType.CANNON), // cannon screen
                Position.of(5, 6), new Piece(Side.HAN, PieceType.SOLDIER),
                Position.of(0, 0), new Piece(Side.HAN, PieceType.GENERAL),
                Position.of(9, 4), new Piece(Side.CHO, PieceType.GENERAL));
        assertFalse(canMove(cannonScreen, 5, 0, 5, 5));
        assertFalse(canMove(cannonScreen, 5, 0, 5, 6));

        // Valid soldier screen but the target is a Cannon -> cannot capture a Cannon.
        GameState cannonTarget = stateWith(Side.CHO,
                Position.of(5, 0), new Piece(Side.CHO, PieceType.CANNON),
                Position.of(5, 2), new Piece(Side.HAN, PieceType.SOLDIER),
                Position.of(5, 6), new Piece(Side.HAN, PieceType.CANNON),
                Position.of(0, 0), new Piece(Side.HAN, PieceType.GENERAL),
                Position.of(9, 4), new Piece(Side.CHO, PieceType.GENERAL));
        assertTrue(canMove(cannonTarget, 5, 0, 5, 5));  // jump to empty still fine
        assertFalse(canMove(cannonTarget, 5, 0, 5, 6)); // cannot capture the cannon
    }

    // ---- Soldier ----

    @Test
    public void soldierMovesForwardAndSidewaysNotBackward() {
        GameState gs = stateWith(Side.CHO,
                Position.of(5, 4), new Piece(Side.CHO, PieceType.SOLDIER),
                Position.of(0, 0), new Piece(Side.HAN, PieceType.GENERAL),
                Position.of(9, 4), new Piece(Side.CHO, PieceType.GENERAL));
        assertTrue(canMove(gs, 5, 4, 4, 4));  // forward (CHO advances up)
        assertTrue(canMove(gs, 5, 4, 5, 3));  // sideways
        assertTrue(canMove(gs, 5, 4, 5, 5));  // sideways
        assertFalse(canMove(gs, 5, 4, 6, 4)); // backward not allowed
    }

    @Test
    public void soldierGainsPalaceDiagonalForward() {
        // CHO soldier at HAN palace corner (2,3) may step diagonally forward to center (1,4).
        GameState gs = stateWith(Side.CHO,
                Position.of(2, 3), new Piece(Side.CHO, PieceType.SOLDIER),
                Position.of(0, 4), new Piece(Side.HAN, PieceType.GENERAL),
                Position.of(9, 4), new Piece(Side.CHO, PieceType.GENERAL));
        assertTrue(canMove(gs, 2, 3, 1, 4));
    }

    // ---- Pass ----

    @Test
    public void passIsLegalWhenNotInCheck() {
        GameState gs = stateWith(Side.CHO,
                Position.of(8, 4), new Piece(Side.CHO, PieceType.GENERAL),
                Position.of(0, 0), new Piece(Side.HAN, PieceType.GENERAL));
        boolean hasPass = false;
        for (Move m : gs.legalMoves()) {
            if (m.isPass()) {
                hasPass = true;
            }
        }
        assertTrue(hasPass);
    }

    // ---- Repetition ----

    @Test
    public void thirdRepetitionIsExcluded() {
        // Two generals shuffling between two palace squares each reproduce positions.
        GameState gs = stateWith(Side.CHO,
                Position.of(8, 4), new Piece(Side.CHO, PieceType.GENERAL),
                Position.of(1, 4), new Piece(Side.HAN, PieceType.GENERAL));

        Move choA = new Move(Position.of(8, 4), Position.of(8, 3));
        Move choB = new Move(Position.of(8, 3), Position.of(8, 4));
        Move hanA = new Move(Position.of(1, 4), Position.of(1, 3));
        Move hanB = new Move(Position.of(1, 3), Position.of(1, 4));

        // Cycle the start position back to its initial layout twice.
        gs = gs.apply(choA).apply(hanA).apply(choB).apply(hanB); // 2nd occurrence of start
        gs = gs.apply(choA).apply(hanA).apply(choB);             // HAN now to move; back-to-start pending

        // Playing hanB would create the 3rd occurrence of the start position -> illegal.
        assertTrue(gs.wouldRepeat(hanB));
        assertFalse(gs.legalMoves().contains(hanB));
    }

    @Test
    public void checkmateEndsGame() {
        // HAN general at corner (0,3). A CHO chariot on row 0 gives check; two more
        // chariots cover columns 3 and 4, sealing every palace escape. No HAN piece
        // can block or capture an attacker -> checkmate.
        GameState gs = stateWith(Side.HAN,
                Position.of(0, 3), new Piece(Side.HAN, PieceType.GENERAL),
                Position.of(0, 8), new Piece(Side.CHO, PieceType.CHARIOT), // checks along row 0
                Position.of(2, 3), new Piece(Side.CHO, PieceType.CHARIOT), // covers (1,3) & (0,3)
                Position.of(2, 4), new Piece(Side.CHO, PieceType.CHARIOT), // covers (1,4) & (0,4)
                Position.of(9, 4), new Piece(Side.CHO, PieceType.GENERAL));
        assertTrue(gs.isInCheck(Side.HAN));
        assertTrue(gs.isGameOver());
        assertEquals(Side.CHO, gs.winner());
    }
}
