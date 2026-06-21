package com.xiangqi.ai;

import java.util.ArrayList;
import java.util.List;

import com.xiangqi.core.Board;
import com.xiangqi.core.Piece;
import com.xiangqi.core.Position;
import com.xiangqi.core.Side;

/**
 * Self-contained Xiangqi attack geometry, used by the Coach's translator to
 * detect forks and threats and (lightly) by the evaluator.
 *
 * <p>This mirrors the movement rules in the core contract but operates purely on
 * a {@link Board} snapshot; it depends only on {@code com.xiangqi.core} value
 * types and never mutates the board. It is intentionally limited to <em>capture
 * targets</em>: the enemy-occupied intersections a piece could move onto.
 */
final class Attacks {

    private Attacks() {
    }

    /**
     * The enemy pieces that the piece standing on {@code from} attacks on
     * {@code board} (intersections it could capture on its next move). Returns an
     * empty list if {@code from} is empty.
     */
    static List<Position> capturesFrom(Board board, Position from) {
        List<Position> out = new ArrayList<Position>();
        Piece p = board.pieceAt(from);
        if (p == null) {
            return out;
        }
        Side me = p.side();
        int r = from.row();
        int c = from.col();
        switch (p.type()) {
            case GENERAL:
                addStep(board, me, out, r - 1, c, true);
                addStep(board, me, out, r + 1, c, true);
                addStep(board, me, out, r, c - 1, true);
                addStep(board, me, out, r, c + 1, true);
                addFlyingGeneral(board, me, from, out);
                break;
            case ADVISOR:
                addStep(board, me, out, r - 1, c - 1, true);
                addStep(board, me, out, r - 1, c + 1, true);
                addStep(board, me, out, r + 1, c - 1, true);
                addStep(board, me, out, r + 1, c + 1, true);
                break;
            case ELEPHANT:
                addElephant(board, me, out, r, c, -2, -2);
                addElephant(board, me, out, r, c, -2, 2);
                addElephant(board, me, out, r, c, 2, -2);
                addElephant(board, me, out, r, c, 2, 2);
                break;
            case HORSE:
                addHorse(board, me, out, r, c, -2, -1, -1, 0);
                addHorse(board, me, out, r, c, -2, 1, -1, 0);
                addHorse(board, me, out, r, c, 2, -1, 1, 0);
                addHorse(board, me, out, r, c, 2, 1, 1, 0);
                addHorse(board, me, out, r, c, -1, -2, 0, -1);
                addHorse(board, me, out, r, c, 1, -2, 0, -1);
                addHorse(board, me, out, r, c, -1, 2, 0, 1);
                addHorse(board, me, out, r, c, 1, 2, 0, 1);
                break;
            case CHARIOT:
                addSlide(board, me, out, r, c, -1, 0);
                addSlide(board, me, out, r, c, 1, 0);
                addSlide(board, me, out, r, c, 0, -1);
                addSlide(board, me, out, r, c, 0, 1);
                break;
            case CANNON:
                addCannon(board, me, out, r, c, -1, 0);
                addCannon(board, me, out, r, c, 1, 0);
                addCannon(board, me, out, r, c, 0, -1);
                addCannon(board, me, out, r, c, 0, 1);
                break;
            case SOLDIER:
                int fwd = me.forward();
                addStep(board, me, out, r + fwd, c, true);
                Position here = Position.of(r, c);
                if (here.isAcrossRiver(me)) {
                    addStep(board, me, out, r, c - 1, true);
                    addStep(board, me, out, r, c + 1, true);
                }
                break;
            default:
                break;
        }
        return out;
    }

    /** Add (r,c) as a capture target if it is on-board and holds an enemy piece. */
    private static void addStep(Board board, Side me, List<Position> out, int r, int c, boolean captureOnly) {
        Position t = Position.of(r, c);
        if (!t.isOnBoard()) {
            return;
        }
        Piece occ = board.pieceAt(t);
        if (occ != null && occ.side() != me) {
            out.add(t);
        }
    }

    private static void addElephant(Board board, Side me, List<Position> out, int r, int c, int dr, int dc) {
        Position t = Position.of(r + dr, c + dc);
        if (!t.isOnBoard() || t.isAcrossRiver(me)) {
            return;
        }
        Position eye = Position.of(r + dr / 2, c + dc / 2);
        if (board.pieceAt(eye) != null) {
            return; // elephant eye blocked
        }
        Piece occ = board.pieceAt(t);
        if (occ != null && occ.side() != me) {
            out.add(t);
        }
    }

    private static void addHorse(Board board, Side me, List<Position> out,
                                 int r, int c, int dr, int dc, int legDr, int legDc) {
        Position leg = Position.of(r + legDr, c + legDc);
        if (!leg.isOnBoard() || board.pieceAt(leg) != null) {
            return; // hobbled
        }
        Position t = Position.of(r + dr, c + dc);
        if (!t.isOnBoard()) {
            return;
        }
        Piece occ = board.pieceAt(t);
        if (occ != null && occ.side() != me) {
            out.add(t);
        }
    }

    private static void addSlide(Board board, Side me, List<Position> out, int r, int c, int dr, int dc) {
        int nr = r + dr;
        int nc = c + dc;
        while (true) {
            Position t = Position.of(nr, nc);
            if (!t.isOnBoard()) {
                return;
            }
            Piece occ = board.pieceAt(t);
            if (occ != null) {
                if (occ.side() != me) {
                    out.add(t);
                }
                return;
            }
            nr += dr;
            nc += dc;
        }
    }

    private static void addCannon(Board board, Side me, List<Position> out, int r, int c, int dr, int dc) {
        int nr = r + dr;
        int nc = c + dc;
        boolean screenFound = false;
        while (true) {
            Position t = Position.of(nr, nc);
            if (!t.isOnBoard()) {
                return;
            }
            Piece occ = board.pieceAt(t);
            if (occ != null) {
                if (!screenFound) {
                    screenFound = true; // first piece is the screen
                } else {
                    if (occ.side() != me) {
                        out.add(t); // capture over exactly one screen
                    }
                    return;
                }
            }
            nr += dr;
            nc += dc;
        }
    }

    /** The "flying general" capture: enemy general on the same clear file. */
    private static void addFlyingGeneral(Board board, Side me, Position from, List<Position> out) {
        int c = from.col();
        int dr = me.forward(); // toward the enemy home rank
        int nr = from.row() + dr;
        while (nr >= 0 && nr <= 9) {
            Position t = Position.of(nr, c);
            Piece occ = board.pieceAt(t);
            if (occ != null) {
                if (occ.side() != me && occ.type() == com.xiangqi.core.PieceType.GENERAL) {
                    out.add(t);
                }
                return;
            }
            nr += dr;
        }
    }
}
