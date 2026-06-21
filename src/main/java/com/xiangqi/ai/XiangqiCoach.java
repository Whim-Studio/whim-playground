package com.xiangqi.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.xiangqi.core.Board;
import com.xiangqi.core.GameState;
import com.xiangqi.core.Move;
import com.xiangqi.core.Piece;
import com.xiangqi.core.PieceType;
import com.xiangqi.core.Position;
import com.xiangqi.core.Side;

/**
 * The educational tutor for Xiangqi.
 *
 * <p>{@link #topMoves(GameState, int)} scores every legal move for the side to
 * move with the same negamax/alpha-beta search the engine uses, ranks them
 * best-first, and runs each through a <em>translator</em> that turns the change
 * in the position into a concise human sentence — detecting checkmate, checks,
 * captures, forks (one move attacking two valuable pieces), Soldiers crossing the
 * River, and palace-safety changes.
 *
 * <p>Pure logic: no Swing, no I/O.
 */
public final class XiangqiCoach implements Coach {

    /** Default search depth, tuned to stay responsive in the Swing UI. */
    public static final int DEFAULT_DEPTH = 3;

    private static final int INFINITY = 1_000_000_000;

    private final int depth;
    private final Evaluator evaluator;

    public XiangqiCoach() {
        this(DEFAULT_DEPTH);
    }

    public XiangqiCoach(int depth) {
        if (depth < 1) {
            throw new IllegalArgumentException("depth must be >= 1, was " + depth);
        }
        this.depth = depth;
        this.evaluator = new XiangqiEvaluator();
    }

    @Override
    public List<MoveAdvice> topMoves(GameState state, int n) {
        List<MoveAdvice> advice = new ArrayList<MoveAdvice>();
        if (n <= 0) {
            return advice;
        }
        final Side me = state.sideToMove();
        List<Move> moves = state.legalMoves();
        if (moves.isEmpty()) {
            return advice;
        }

        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);
            GameState child = state.apply(move);
            // Value of this move for `me`: opponent's best reply, negated.
            int score = -negamax(child, depth - 1, -INFINITY, INFINITY, me.opponent());
            String text = explain(state, move, child, me);
            advice.add(new MoveAdvice(move, score, text));
        }

        Collections.sort(advice, new Comparator<MoveAdvice>() {
            @Override
            public int compare(MoveAdvice a, MoveAdvice b) {
                return b.score() - a.score();
            }
        });

        if (advice.size() > n) {
            return new ArrayList<MoveAdvice>(advice.subList(0, n));
        }
        return advice;
    }

    // ------------------------------------------------------------------
    // Search (same negamax/alpha-beta as MinimaxAI, used here for ranking)
    // ------------------------------------------------------------------

    private int negamax(GameState state, int remaining, int alpha, int beta, Side sideToMove) {
        if (state.isGameOver() || remaining == 0) {
            return evaluator.evaluate(state, sideToMove);
        }
        List<Move> moves = state.legalMoves();
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
                break;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // Translator: turn the move's effect into a plain-English sentence
    // ------------------------------------------------------------------

    /**
     * Build a concise explanation of {@code move} (played by {@code me}), given
     * the position {@code before} it was played and the position {@code after}.
     */
    String explain(GameState before, Move move, GameState after, Side me) {
        Board board = before.board();
        Piece mover = board.pieceAt(move.from());
        String moverName = mover == null ? "piece" : pieceName(mover.type());
        Piece captured = board.pieceAt(move.to());
        Side enemy = me.opponent();

        boolean givesCheck = after.isInCheck(enemy);
        boolean isMate = givesCheck && after.isCheckmate(enemy);
        List<Position> forked = forkTargets(after.board(), move.to(), me);

        String lead = "Moving your " + moverName + " to " + coord(move.to());

        if (isMate) {
            return lead + " delivers checkmate!";
        }
        if (captured != null && givesCheck) {
            return lead + " captures the " + pieceName(captured.type())
                    + " and checks the General.";
        }
        if (forked.size() >= 2) {
            return lead + " forks the " + pieceName(typeAt(after.board(), forked.get(0)))
                    + " and " + pieceName(typeAt(after.board(), forked.get(1))) + ".";
        }
        if (captured != null) {
            return lead + " captures the " + pieceName(captured.type()) + ".";
        }
        if (givesCheck) {
            return lead + " puts the enemy General in check.";
        }

        // Escaping check is a palace-safety improvement worth calling out.
        if (before.isInCheck(me) && !after.isInCheck(me)) {
            return lead + " escapes check and shields your Palace.";
        }

        // Soldier crossing the River is a notable advance.
        if (mover != null && mover.type() == PieceType.SOLDIER
                && !move.from().isAcrossRiver(me) && move.to().isAcrossRiver(me)) {
            return "Advancing your Soldier across the River to " + coord(move.to())
                    + " gains attacking range.";
        }

        // A single new threat on a valuable piece.
        if (forked.size() == 1) {
            return lead + " threatens the " + pieceName(typeAt(after.board(), forked.get(0))) + ".";
        }

        // Generic development / safety message based on the piece moved.
        if (mover != null && (mover.type() == PieceType.ADVISOR
                || mover.type() == PieceType.ELEPHANT)) {
            return lead + " strengthens your Palace defence.";
        }
        return lead + " develops your " + moverName + " and improves your position.";
    }

    /**
     * Enemy pieces of meaningful value (>= ELEPHANT/ADVISOR) attacked by the
     * piece that just landed on {@code from}, for the moving side {@code me}.
     */
    private List<Position> forkTargets(Board board, Position from, Side me) {
        List<Position> valuable = new ArrayList<Position>();
        List<Position> all = Attacks.capturesFrom(board, from);
        for (int i = 0; i < all.size(); i++) {
            Piece p = board.pieceAt(all.get(i));
            if (p == null || p.side() == me) {
                continue;
            }
            if (p.type() == PieceType.GENERAL || p.type() == PieceType.SOLDIER) {
                continue; // general handled as check; soldiers are minor
            }
            if (XiangqiEvaluator.baseValue(p.type()) >= 200) {
                valuable.add(all.get(i));
            }
        }
        return valuable;
    }

    private PieceType typeAt(Board board, Position p) {
        Piece piece = board.pieceAt(p);
        return piece == null ? PieceType.SOLDIER : piece.type();
    }

    private static String coord(Position p) {
        return "(" + p.row() + "," + p.col() + ")";
    }

    private static String pieceName(PieceType type) {
        switch (type) {
            case GENERAL:
                return "General";
            case ADVISOR:
                return "Advisor";
            case ELEPHANT:
                return "Elephant";
            case HORSE:
                return "Horse";
            case CHARIOT:
                return "Chariot";
            case CANNON:
                return "Cannon";
            case SOLDIER:
                return "Soldier";
            default:
                return "piece";
        }
    }
}
