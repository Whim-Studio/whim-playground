package com.xiangqi.ai;

import com.xiangqi.core.Board;
import com.xiangqi.core.GameState;
import com.xiangqi.core.Piece;
import com.xiangqi.core.PieceType;
import com.xiangqi.core.Position;
import com.xiangqi.core.Side;

/**
 * Xiangqi-specific static evaluator.
 *
 * <p>Combines three terms, all measured from {@code perspective}:
 * <ul>
 *   <li><b>Material</b> — the contract's suggested centipoint values, with a
 *       Soldier worth more once it has crossed the River.</li>
 *   <li><b>Mobility / board control</b> — a small bonus per legal move available
 *       to the side to move, plus a positional bonus for advanced Soldiers and
 *       centralised major pieces.</li>
 *   <li><b>Palace safety</b> — bonuses for retained Advisors/Elephants that
 *       shield the General, and a penalty for being in check.</li>
 * </ul>
 *
 * <p>Terminal positions return a large constant so the search prefers faster
 * mates and avoids being mated.
 */
public final class XiangqiEvaluator implements Evaluator {

    /** Score used for a won/lost terminal position (well below GENERAL value). */
    static final int WIN_SCORE = 500_000;

    private static final int MOBILITY_WEIGHT = 2;
    private static final int CHECK_PENALTY = 30;
    private static final int DEFENDER_BONUS = 15; // per Advisor/Elephant still home

    @Override
    public int evaluate(GameState state, Side perspective) {
        if (state.isGameOver()) {
            Side w = state.winner();
            if (w == null) {
                return 0; // draw
            }
            return w == perspective ? WIN_SCORE : -WIN_SCORE;
        }

        Board board = state.board();
        int score = material(board, perspective) - material(board, perspective.opponent());
        score += positional(board, perspective) - positional(board, perspective.opponent());
        score += palaceSafety(state, perspective) - palaceSafety(state, perspective.opponent());

        // Mobility: cheap proxy using the side to move's legal-move count.
        int mob = state.legalMoves().size() * MOBILITY_WEIGHT;
        score += state.sideToMove() == perspective ? mob : -mob;

        return score;
    }

    /** Base material value of a piece type (centipoints), ignoring position. */
    static int baseValue(PieceType type) {
        switch (type) {
            case CHARIOT:
                return 900;
            case CANNON:
                return 450;
            case HORSE:
                return 400;
            case ELEPHANT:
                return 200;
            case ADVISOR:
                return 200;
            case SOLDIER:
                return 100;
            case GENERAL:
                return 1_000_000;
            default:
                return 0;
        }
    }

    /** Total material for {@code side}, with Soldiers worth 200 across the River. */
    private int material(Board board, Side side) {
        int total = 0;
        for (int r = 0; r <= 9; r++) {
            for (int c = 0; c <= 8; c++) {
                Piece p = board.pieceAt(r, c);
                if (p == null || p.side() != side) {
                    continue;
                }
                if (p.type() == PieceType.SOLDIER) {
                    total += Position.of(r, c).isAcrossRiver(side) ? 200 : 100;
                } else {
                    total += baseValue(p.type());
                }
            }
        }
        return total;
    }

    /** Light positional term: centralisation of major pieces, advanced Soldiers. */
    private int positional(Board board, Side side) {
        int score = 0;
        for (int r = 0; r <= 9; r++) {
            for (int c = 0; c <= 8; c++) {
                Piece p = board.pieceAt(r, c);
                if (p == null || p.side() != side) {
                    continue;
                }
                switch (p.type()) {
                    case CHARIOT:
                    case CANNON:
                    case HORSE:
                        // Reward central files (3..5) for the strong pieces.
                        score += 4 - Math.abs(c - 4);
                        break;
                    case SOLDIER:
                        if (Position.of(r, c).isAcrossRiver(side)) {
                            // Reward Soldiers pushed deep into enemy territory.
                            int depth = side == Side.RED ? (4 - r) : (r - 5);
                            score += 2 * Math.max(0, depth);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return score;
    }

    /** Palace safety: defenders retained near the General, minus check penalty. */
    private int palaceSafety(GameState state, Side side) {
        Board board = state.board();
        int score = 0;
        for (int r = 0; r <= 9; r++) {
            for (int c = 0; c <= 8; c++) {
                Piece p = board.pieceAt(r, c);
                if (p == null || p.side() != side) {
                    continue;
                }
                if (p.type() == PieceType.ADVISOR || p.type() == PieceType.ELEPHANT) {
                    if (Position.of(r, c).isInPalace(side) || p.type() == PieceType.ELEPHANT) {
                        score += DEFENDER_BONUS;
                    }
                }
            }
        }
        if (state.isInCheck(side)) {
            score -= CHECK_PENALTY;
        }
        return score;
    }
}
