package com.xiangqi.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.xiangqi.core.Board;
import com.xiangqi.core.GameState;
import com.xiangqi.core.Move;
import com.xiangqi.core.Piece;
import com.xiangqi.core.Side;

/**
 * Native Minimax search with alpha-beta pruning for Xiangqi.
 *
 * <p>Search is written in negamax form: a position's value is always computed
 * from the point of view of the side to move there, and scores are negated when
 * descending a ply. Leaf positions are scored by an {@link Evaluator} (default
 * {@link XiangqiEvaluator}).
 *
 * <p>Light move ordering (most-valuable captures first) is applied at every node
 * to sharpen alpha-beta cutoffs, keeping the default depth (3-4) responsive for a
 * Swing UI. The search is driven purely through the {@code com.xiangqi.core}
 * contract: {@link GameState#legalMoves()}, {@link GameState#apply(Move)},
 * {@link GameState#isGameOver()} and {@link GameState#winner()}.
 */
public final class MinimaxAI implements XiangqiAI {

    private static final int INFINITY = 1_000_000_000;

    private final int depth;
    private final Evaluator evaluator;

    /** Alpha-beta minimax to the given depth with the default evaluator. */
    public MinimaxAI(int depth) {
        this(depth, new XiangqiEvaluator());
    }

    /** Alpha-beta minimax to the given depth with a custom evaluator. */
    public MinimaxAI(int depth, Evaluator eval) {
        if (depth < 1) {
            throw new IllegalArgumentException("depth must be >= 1, was " + depth);
        }
        if (eval == null) {
            throw new IllegalArgumentException("evaluator must not be null");
        }
        this.depth = depth;
        this.evaluator = eval;
    }

    public int depth() {
        return depth;
    }

    @Override
    public Move chooseMove(GameState state) {
        List<Move> moves = orderedMoves(state);
        if (moves.isEmpty()) {
            return null; // no legal move (checkmate/stalemate): caller decides
        }

        Side me = state.sideToMove();
        Move best = moves.get(0);
        int bestScore = -INFINITY;
        int alpha = -INFINITY;
        int beta = INFINITY;

        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);
            GameState child = state.apply(move);
            int score = -negamax(child, depth - 1, -beta, -alpha, me.opponent());
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
            if (score > alpha) {
                alpha = score;
            }
        }
        return best;
    }

    /**
     * Negamax with alpha-beta. Returns the value of {@code state} from the point
     * of view of {@code sideToMove} (which equals {@code state.sideToMove()}).
     */
    private int negamax(GameState state, int remaining, int alpha, int beta, Side sideToMove) {
        if (state.isGameOver() || remaining == 0) {
            return evaluator.evaluate(state, sideToMove);
        }

        List<Move> moves = orderedMoves(state);
        if (moves.isEmpty()) {
            // No legal move: terminal for this side. Score from its perspective.
            return evaluator.evaluate(state, sideToMove);
        }

        int best = -INFINITY;
        for (int i = 0; i < moves.size(); i++) {
            GameState child = state.apply(moves.get(i));
            int score = -negamax(child, remaining - 1, -beta, -alpha, sideToMove.opponent());
            if (score > best) {
                best = score;
            }
            if (best > alpha) {
                alpha = best;
            }
            if (alpha >= beta) {
                break; // alpha-beta cutoff
            }
        }
        return best;
    }

    /** Legal moves ordered most-valuable-capture first for better pruning. */
    private List<Move> orderedMoves(GameState state) {
        List<Move> moves = new ArrayList<Move>(state.legalMoves());
        final Board board = state.board();
        Collections.sort(moves, new Comparator<Move>() {
            @Override
            public int compare(Move a, Move b) {
                return captureScore(board, b) - captureScore(board, a);
            }
        });
        return moves;
    }

    /** Value of the piece (if any) captured by {@code move}; 0 for a quiet move. */
    private int captureScore(Board board, Move move) {
        Piece target = board.pieceAt(move.to());
        if (target == null) {
            return 0;
        }
        return XiangqiEvaluator.baseValue(target.type());
    }
}
