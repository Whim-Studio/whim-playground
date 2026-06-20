package com.finesse.variant;

import com.finesse.core.Board;
import com.finesse.core.GameState;
import com.finesse.core.MoveGenerator;
import com.finesse.core.Piece;
import com.finesse.core.PieceColor;
import com.finesse.core.PieceType;
import com.finesse.core.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Movement and capture logic for Finesse Chess.
 *
 * <p>{@link #legalMoves(GameState, Position)} returns the destination
 * {@link Position}s reachable by the piece on {@code from}. Rules are documented
 * in {@code RULES.md}; this class is the executable form of that contract.
 *
 * <p>Scope note (per RULES.md §5): moves are legal by movement, board-bounds and
 * friendly-occupancy rules only. Self-check / castling / en passant are not
 * modeled.
 */
public class FinesseMoveGenerator implements MoveGenerator {

    private static final int[][] ROOK_DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] BISHOP_DIRS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] QUEEN_DIRS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };
    private static final int[][] KNIGHT_HOPS = {
        {1, 2}, {2, 1}, {-1, 2}, {-2, 1}, {1, -2}, {2, -1}, {-1, -2}, {-2, -1}
    };

    @Override
    public List<Position> legalMoves(GameState state, Position from) {
        List<Position> moves = new ArrayList<>();
        if (state == null || from == null) {
            return moves;
        }
        Board board = state.getBoard();
        if (!board.isInBounds(from)) {
            return moves;
        }
        Piece piece = board.getPiece(from);
        if (piece == null || piece.getColor() != state.getSideToMove()) {
            return moves;
        }

        switch (piece.getType()) {
            case PAWN:
                addPawnMoves(board, from, piece, moves);
                break;
            case ROOK:
                addSliding(board, from, piece, ROOK_DIRS, moves);
                break;
            case QUEEN:
                addSliding(board, from, piece, QUEEN_DIRS, moves);
                break;
            case KNIGHT:
                addLeaper(board, from, piece, KNIGHT_HOPS, moves, true);
                break;
            case KING:
                addLeaper(board, from, piece, QUEEN_DIRS, moves, true);
                break;
            case FINESSE:
                addFinesseMoves(board, from, piece, moves);
                break;
            default:
                break;
        }
        return moves;
    }

    // --- piece logic -------------------------------------------------------

    /** Sliding pieces: travel along rays until blocked; capture first enemy. */
    private void addSliding(Board board, Position from, Piece piece,
                            int[][] dirs, List<Position> out) {
        int f = from.getFile();
        int r = from.getRank();
        for (int[] d : dirs) {
            int nf = f + d[0];
            int nr = r + d[1];
            while (true) {
                Position to = new Position(nf, nr);
                if (!board.isInBounds(to)) {
                    break;
                }
                Piece occupant = board.getPiece(to);
                if (occupant == null) {
                    out.add(to);
                } else {
                    if (occupant.getColor() != piece.getColor()) {
                        out.add(to); // capture
                    }
                    break; // blocked either way
                }
                nf += d[0];
                nr += d[1];
            }
        }
    }

    /**
     * Single-step leapers (knight, king).
     *
     * @param mayCapture whether landing on an enemy is allowed (true for
     *                   knight/king quiet+capture moves).
     */
    private void addLeaper(Board board, Position from, Piece piece,
                           int[][] hops, List<Position> out, boolean mayCapture) {
        int f = from.getFile();
        int r = from.getRank();
        for (int[] h : hops) {
            Position to = new Position(f + h[0], r + h[1]);
            if (!board.isInBounds(to)) {
                continue;
            }
            Piece occupant = board.getPiece(to);
            if (occupant == null) {
                out.add(to);
            } else if (mayCapture && occupant.getColor() != piece.getColor()) {
                out.add(to);
            }
        }
    }

    /**
     * The Finesse: quiet moves slide along the diagonals (never capturing),
     * captures happen only via a knight's leap onto an enemy.
     */
    private void addFinesseMoves(Board board, Position from, Piece piece,
                                 List<Position> out) {
        int f = from.getFile();
        int r = from.getRank();

        // Quiet diagonal slide: stop at (and exclude) the first occupied square.
        for (int[] d : BISHOP_DIRS) {
            int nf = f + d[0];
            int nr = r + d[1];
            while (true) {
                Position to = new Position(nf, nr);
                if (!board.isInBounds(to)) {
                    break;
                }
                if (board.getPiece(to) != null) {
                    break; // cannot capture along the diagonal
                }
                out.add(to);
                nf += d[0];
                nr += d[1];
            }
        }

        // Capture only: knight leap onto an enemy piece.
        for (int[] h : KNIGHT_HOPS) {
            Position to = new Position(f + h[0], r + h[1]);
            if (!board.isInBounds(to)) {
                continue;
            }
            Piece occupant = board.getPiece(to);
            if (occupant != null && occupant.getColor() != piece.getColor()) {
                out.add(to);
            }
        }
    }

    /** Pawns: forward push (1, or 2 from start), diagonal captures. */
    private void addPawnMoves(Board board, Position from, Piece piece,
                              List<Position> out) {
        int f = from.getFile();
        int r = from.getRank();
        boolean white = piece.getColor() == PieceColor.WHITE;
        int dir = white ? 1 : -1;
        int startRank = white ? 1 : 6;

        // Single push.
        Position one = new Position(f, r + dir);
        if (board.isInBounds(one) && board.getPiece(one) == null) {
            out.add(one);
            // Double push from the starting rank.
            if (r == startRank) {
                Position two = new Position(f, r + 2 * dir);
                if (board.isInBounds(two) && board.getPiece(two) == null) {
                    out.add(two);
                }
            }
        }

        // Diagonal captures.
        for (int df : new int[]{-1, 1}) {
            Position cap = new Position(f + df, r + dir);
            if (!board.isInBounds(cap)) {
                continue;
            }
            Piece occupant = board.getPiece(cap);
            if (occupant != null && occupant.getColor() != piece.getColor()) {
                out.add(cap);
            }
        }
    }
}
