package com.finesse.variant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.finesse.core.Board;
import com.finesse.core.GameState;
import com.finesse.core.Piece;
import com.finesse.core.PieceColor;
import com.finesse.core.PieceType;
import com.finesse.core.Position;
import java.util.EnumMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that {@link FinesseSetup} produces the documented starting position.
 *
 * <p>The authoritative source is {@code RULES.md} (owned by Task 2). RULES.md was
 * not yet present on this branch when these tests were authored, so every
 * board-specific expected value below is marked {@code // ASSUMPTION:} and must be
 * reconciled against RULES.md at integration. The structural invariants
 * (balanced colors, exactly one king per side, fully-populated back ranks) should
 * hold for essentially any chess-like variant and are asserted unconditionally.
 *
 * <p>API ASSUMPTIONS (Task 1/2 own these; reconcile names at integration):
 * <ul>
 *   <li>{@code new FinesseSetup().createInitialState()} returns the starting {@link GameState}.</li>
 *   <li>{@code GameState#getBoard()} and {@code GameState#getSideToMove()}.</li>
 *   <li>{@code Piece#getType()} / {@code Piece#getColor()}.</li>
 * </ul>
 */
public class FinesseSetupTest {

    // ASSUMPTION: the Finesse board is 8x8. Replace with RULES.md dimensions if different.
    private static final int MAX_SCAN = 32;

    private GameState state;
    private Board board;

    @Before
    public void setUp() {
        state = new FinesseSetup().createInitialState();
        assertNotNull("setup must produce a GameState", state);
        board = state.getBoard();
        assertNotNull("GameState must wrap a Board", board);
    }

    /** Counts pieces of a given color across the whole in-bounds board. */
    private int countByColor(PieceColor color) {
        int count = 0;
        for (int f = 0; f < MAX_SCAN; f++) {
            for (int r = 0; r < MAX_SCAN; r++) {
                Position p = new Position(f, r);
                if (!board.isInBounds(p)) {
                    continue;
                }
                Piece piece = board.getPiece(p);
                if (piece != null && piece.getColor() == color) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Tallies pieces of a color by type across the in-bounds board. */
    private Map<PieceType, Integer> tallyByType(PieceColor color) {
        Map<PieceType, Integer> tally = new EnumMap<>(PieceType.class);
        for (int f = 0; f < MAX_SCAN; f++) {
            for (int r = 0; r < MAX_SCAN; r++) {
                Position p = new Position(f, r);
                if (!board.isInBounds(p)) {
                    continue;
                }
                Piece piece = board.getPiece(p);
                if (piece != null && piece.getColor() == color) {
                    tally.merge(piece.getType(), 1, Integer::sum);
                }
            }
        }
        return tally;
    }

    @Test
    public void whiteMovesFirst() {
        // ASSUMPTION: White is the side to move in the initial position.
        assertEquals(PieceColor.WHITE, state.getSideToMove());
    }

    @Test
    public void bothColorsArePresent() {
        assertTrue("white pieces must exist", countByColor(PieceColor.WHITE) > 0);
        assertTrue("black pieces must exist", countByColor(PieceColor.BLACK) > 0);
    }

    @Test
    public void pieceCountsAreBalancedBetweenColors() {
        assertEquals(
                "white and black should start with equal material",
                countByColor(PieceColor.WHITE),
                countByColor(PieceColor.BLACK));
    }

    @Test
    public void exactlyOneKingPerColor() {
        assertEquals(
                "white must have exactly one king",
                Integer.valueOf(1),
                tallyByType(PieceColor.WHITE).getOrDefault(PieceType.KING, 0));
        assertEquals(
                "black must have exactly one king",
                Integer.valueOf(1),
                tallyByType(PieceColor.BLACK).getOrDefault(PieceType.KING, 0));
    }

    @Test
    public void standardPieceComplementPerColor() {
        // ASSUMPTION: Finesse uses the standard FIDE complement per color:
        // 8 pawns, 2 rooks, 2 knights, 2 bishops, 1 queen, 1 king (16 total).
        // RULES.md may define a different roster — reconcile these expected counts.
        Map<PieceType, Integer> white = tallyByType(PieceColor.WHITE);
        assertEquals(Integer.valueOf(8), white.getOrDefault(PieceType.PAWN, 0));
        assertEquals(Integer.valueOf(2), white.getOrDefault(PieceType.ROOK, 0));
        assertEquals(Integer.valueOf(2), white.getOrDefault(PieceType.KNIGHT, 0));
        assertEquals(Integer.valueOf(2), white.getOrDefault(PieceType.BISHOP, 0));
        assertEquals(Integer.valueOf(1), white.getOrDefault(PieceType.QUEEN, 0));
        assertEquals(Integer.valueOf(1), white.getOrDefault(PieceType.KING, 0));
    }

    @Test
    public void totalStartingMaterialIsSixteenPerSide() {
        // ASSUMPTION: 16 pieces per side (standard complement). Reconcile with RULES.md.
        assertEquals(16, countByColor(PieceColor.WHITE));
        assertEquals(16, countByColor(PieceColor.BLACK));
    }

    @Test
    public void documentedBackRankSquaresHoldExpectedPieces() {
        // ASSUMPTION: standard chess back-rank layout on rank 0 (White) /
        // rank 7 (Black), files a..h = 0..7:
        //   rook,knight,bishop,queen,king,bishop,knight,rook.
        // This is a placeholder reflecting standard chess; replace each square's
        // expected piece with the Finesse layout documented in RULES.md.
        assertPiece(0, 0, PieceType.ROOK, PieceColor.WHITE);
        assertPiece(1, 0, PieceType.KNIGHT, PieceColor.WHITE);
        assertPiece(2, 0, PieceType.BISHOP, PieceColor.WHITE);
        assertPiece(3, 0, PieceType.QUEEN, PieceColor.WHITE);
        assertPiece(4, 0, PieceType.KING, PieceColor.WHITE);
        assertPiece(5, 0, PieceType.BISHOP, PieceColor.WHITE);
        assertPiece(6, 0, PieceType.KNIGHT, PieceColor.WHITE);
        assertPiece(7, 0, PieceType.ROOK, PieceColor.WHITE);

        assertPiece(4, 7, PieceType.KING, PieceColor.BLACK);
        assertPiece(3, 7, PieceType.QUEEN, PieceColor.BLACK);
    }

    @Test
    public void documentedPawnRanksAreFilled() {
        // ASSUMPTION: White pawns on rank 1, Black pawns on rank 6 (standard).
        // Reconcile pawn ranks with RULES.md.
        for (int file = 0; file < 8; file++) {
            assertPiece(file, 1, PieceType.PAWN, PieceColor.WHITE);
            assertPiece(file, 6, PieceType.PAWN, PieceColor.BLACK);
        }
    }

    private void assertPiece(int file, int rank, PieceType type, PieceColor color) {
        Position p = new Position(file, rank);
        Piece piece = board.getPiece(p);
        assertNotNull("expected a piece at (" + file + "," + rank + ")", piece);
        assertEquals("piece type at (" + file + "," + rank + ")", type, piece.getType());
        assertEquals("piece color at (" + file + "," + rank + ")", color, piece.getColor());
    }
}
