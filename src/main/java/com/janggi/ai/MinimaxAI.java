package com.janggi.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.janggi.core.Board;
import com.janggi.core.GameState;
import com.janggi.core.Move;
import com.janggi.core.Piece;
import com.janggi.core.Position;
import com.janggi.core.Side;

/**
 * Native Minimax search with alpha-beta pruning for Janggi.
 *
 * <p>Search is expressed in negamax form: the value of a position is always
 * computed from the point of view of the side to move in that position, and
 * scores are negated when descending a ply. Leaf positions are scored by an
 * {@link Evaluator} (default {@link MaterialEvaluator}).
 *
 * <p>Light move ordering (most-valuable captures first) is applied at every node
 * to improve alpha-beta cutoffs. The whole search is driven purely through the
 * {@code com.janggi.core} contract: {@link GameState#legalMoves()},
 * {@link GameState#apply(Move)}, {@link GameState#isGameOver()} and
 * {@link GameState#winner()}.
 */
public final class MinimaxAI implements JanggiAI {

    private static final int INFINITY = 1_000_000_000;

    private final int depth;
    private final Evaluator evaluator;
    private final MaterialEvaluator captureValues;

    /** Alpha-beta minimax to the given depth with the default evaluator. */
    public MinimaxAI(int depth) {
        this(depth, new MaterialEvaluator());
    }

    /** Alpha-beta minimax to the given depth with a custom evaluator. */
    public MinimaxAI(int depth, Evaluator evaluator) {
        if (depth < 1) {
            throw new IllegalArgumentException("depth must be >= 1, was " + depth);
        }
        if (evaluator == null) {
            throw new IllegalArgumentException("evaluator must not be null");
        }
        this.depth = depth;
        this.evaluator = evaluator;
        // Used only for move ordering by captured-piece value.
        this.captureValues = new MaterialEvaluator();
    }

    public int depth() {
        return depth;
    }

    @Override
    public Move chooseMove(GameState state) {
        List<Move> moves = orderedMoves(state);
        if (moves.isEmpty()) {
            // Contract guarantees a pass is always legal; fall back defensively.
            return passMove(state);
        }

        Side me = state.sideToMove();
        Move best = moves.get(0);
        int bestScore = -INFINITY;
        int alpha = -INFINITY;
        int beta = INFINITY;

        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);
            GameState child = state.apply(move);
            // Negamax: opponent's best reply, negated to our perspective.
            int score = -negamax(child, depth - 1, -beta, -alpha, me.opponent());
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
            if (score > alpha) {
                alpha = score;
            }
            // No cutoff at the root: we need to keep the chosen move.
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

    /** Value of the piece (if any) captured by {@code move}; 0 for quiet/pass. */
    private int captureScore(Board board, Move move) {
        if (move.isPass()) {
            return 0;
        }
        Piece target = board.pieceAt(move.to());
        if (target == null) {
            return 0;
        }
        return captureValues.baseValue(target.type());
    }

    /** A defensive pass move (from == to) on the side-to-move's General. */
    private Move passMove(GameState state) {
        Position g = state.board().findGeneral(state.sideToMove());
        return new Move(g, g);
    }
}
