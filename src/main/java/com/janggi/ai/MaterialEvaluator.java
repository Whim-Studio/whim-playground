package com.janggi.ai;

import com.janggi.core.Board;
import com.janggi.core.GameState;
import com.janggi.core.Piece;
import com.janggi.core.PieceType;
import com.janggi.core.Position;
import com.janggi.core.Side;

/**
 * Default Janggi {@link Evaluator}: material + mobility + palace safety.
 *
 * <p>Material uses the contract's suggested centi-values. On top of raw
 * material we apply a few Janggi-specific adjustments:
 *
 * <ul>
 *   <li><b>Cannons need a screen.</b> A Cannon can neither move nor capture
 *       without exactly one intervening piece, and it may not hop a Cannon nor
 *       capture one. On a sparse board it is far less useful, so its value is
 *       scaled down as the total non-cannon piece count drops.</li>
 *   <li><b>Elephants are long-range but leg-blockable.</b> They are worth more
 *       with open lines, so a small mobility component (legal-move count for the
 *       side to move) rewards activity generally — Elephants and Horses, whose
 *       worth is almost entirely positional, benefit most from this.</li>
 *   <li><b>Palace safety.</b> Being in check is penalised; keeping Guards inside
 *       the palace near the General is rewarded.</li>
 * </ul>
 *
 * <p><b>Bikjang.</b> The facing-Generals constraint requires no evaluation term:
 * {@code GameState.legalMoves()} already filters out any move that would create a
 * Bikjang, so the search ({@link MinimaxAI}) never reaches such a position.
 *
 * Scores are returned from {@code perspective}'s point of view (higher better)
 * and are anti-symmetric apart from the side-to-move mobility term.
 */
public final class MaterialEvaluator implements Evaluator {

    /** Centi-value of capturing a General — effectively game over. */
    public static final int GENERAL = 1_000_000;
    public static final int CHARIOT = 1300;
    public static final int CANNON = 700;
    public static final int HORSE = 500;
    public static final int ELEPHANT = 300;
    public static final int GUARD = 300;
    public static final int SOLDIER = 200;

    private static final int BOARD_ROWS = 10;
    private static final int BOARD_COLS = 9;

    /** Weight per legal move available to the side to move. */
    private static final int MOBILITY_WEIGHT = 3;
    /** Penalty for being in check. */
    private static final int CHECK_PENALTY = 60;
    /** Bonus per Guard that is inside its own palace. */
    private static final int GUARD_IN_PALACE_BONUS = 15;

    public int baseValue(PieceType type) {
        switch (type) {
            case GENERAL:
                return GENERAL;
            case CHARIOT:
                return CHARIOT;
            case CANNON:
                return CANNON;
            case HORSE:
                return HORSE;
            case ELEPHANT:
                return ELEPHANT;
            case GUARD:
                return GUARD;
            case SOLDIER:
                return SOLDIER;
            default:
                return 0;
        }
    }

    @Override
    public int evaluate(GameState state, Side perspective) {
        // Terminal positions: decisive.
        if (state.isGameOver()) {
            Side w = state.winner();
            if (w == null) {
                return 0;
            }
            return w == perspective ? GENERAL : -GENERAL;
        }

        Board board = state.board();

        // First pass: count non-cannon material so we can discount cannons on
        // a sparse board (a cannon with no potential screens is weak).
        int nonCannonPieces = 0;
        for (int r = 0; r < BOARD_ROWS; r++) {
            for (int c = 0; c < BOARD_COLS; c++) {
                Piece p = board.pieceAt(r, c);
                if (p != null && p.type() != PieceType.CANNON) {
                    nonCannonPieces++;
                }
            }
        }
        // Fraction (out of 8) of how "screen-rich" the board still is.
        int cannonScaleNum = Math.min(nonCannonPieces, 8);

        int score = 0;
        for (int r = 0; r < BOARD_ROWS; r++) {
            for (int c = 0; c < BOARD_COLS; c++) {
                Piece p = board.pieceAt(r, c);
                if (p == null) {
                    continue;
                }
                int value = baseValue(p.type());
                if (p.type() == PieceType.CANNON) {
                    // Scale a cannon's value with screen availability:
                    // half base + half proportional to remaining screens.
                    value = CANNON / 2 + (CANNON / 2) * cannonScaleNum / 8;
                } else if (p.type() == PieceType.GUARD
                        && Position.of(r, c).isInPalace(p.side())) {
                    value += GUARD_IN_PALACE_BONUS;
                }
                score += (p.side() == perspective) ? value : -value;
            }
        }

        // Palace safety: penalise whichever side is in check.
        if (state.isInCheck(perspective)) {
            score -= CHECK_PENALTY;
        }
        if (state.isInCheck(perspective.opponent())) {
            score += CHECK_PENALTY;
        }

        // Mobility (only computable for the side to move via the contract).
        int mobility = state.legalMoves().size() * MOBILITY_WEIGHT;
        score += (state.sideToMove() == perspective) ? mobility : -mobility;

        return score;
    }
}
