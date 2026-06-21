package com.xiangqi.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The central engine object: an immutable snapshot of a Xiangqi position plus
 * the side to move and the history of positions reached. {@link #apply(Move)}
 * produces a new state with the move played and the side flipped.
 *
 * <p>Move generation enforces every Xiangqi rule with no shortcuts: palace
 * confinement, the elephant-eye and horse-leg blocks, the elephant's inability
 * to cross the River, the cannon's single-screen capture, the soldier's
 * forward/sideways restrictions, and the Flying General rule. {@link
 * #legalMoves()} additionally excludes any move that would leave the mover's own
 * General in check.
 */
public class GameState {

    private final Board board;
    private final Side sideToMove;
    private final List<String> history;

    private GameState(Board board, Side sideToMove, List<String> history) {
        this.board = board;
        this.sideToMove = sideToMove;
        this.history = history;
    }

    /** The standard opening position with RED to move. */
    public static GameState initial() {
        Board b = new Board();

        // BLACK home rank (row 0) and soldiers/cannons on the top half.
        placeBackRank(b, Side.BLACK, 0);
        b.set(Position.of(2, 1), new Piece(Side.BLACK, PieceType.CANNON));
        b.set(Position.of(2, 7), new Piece(Side.BLACK, PieceType.CANNON));
        for (int c = 0; c <= 8; c += 2) {
            b.set(Position.of(3, c), new Piece(Side.BLACK, PieceType.SOLDIER));
        }

        // RED home rank (row 9) and soldiers/cannons on the bottom half.
        placeBackRank(b, Side.RED, 9);
        b.set(Position.of(7, 1), new Piece(Side.RED, PieceType.CANNON));
        b.set(Position.of(7, 7), new Piece(Side.RED, PieceType.CANNON));
        for (int c = 0; c <= 8; c += 2) {
            b.set(Position.of(6, c), new Piece(Side.RED, PieceType.SOLDIER));
        }

        List<String> hist = new ArrayList<String>();
        GameState state = new GameState(b, Side.RED, hist);
        hist.add(state.signature());
        return state;
    }

    /**
     * Package-private factory used by tests to construct a state around an
     * arbitrary board. History starts with the given position only.
     */
    static GameState fromBoard(Board board, Side sideToMove) {
        List<String> hist = new ArrayList<String>();
        GameState state = new GameState(board, sideToMove, hist);
        hist.add(state.signature());
        return state;
    }

    private static void placeBackRank(Board b, Side side, int row) {
        b.set(Position.of(row, 0), new Piece(side, PieceType.CHARIOT));
        b.set(Position.of(row, 1), new Piece(side, PieceType.HORSE));
        b.set(Position.of(row, 2), new Piece(side, PieceType.ELEPHANT));
        b.set(Position.of(row, 3), new Piece(side, PieceType.ADVISOR));
        b.set(Position.of(row, 4), new Piece(side, PieceType.GENERAL));
        b.set(Position.of(row, 5), new Piece(side, PieceType.ADVISOR));
        b.set(Position.of(row, 6), new Piece(side, PieceType.ELEPHANT));
        b.set(Position.of(row, 7), new Piece(side, PieceType.HORSE));
        b.set(Position.of(row, 8), new Piece(side, PieceType.CHARIOT));
    }

    public Board board() {
        return board;
    }

    public Side sideToMove() {
        return sideToMove;
    }

    /** All fully-legal moves for {@link #sideToMove()}. */
    public List<Move> legalMoves() {
        List<Move> result = new ArrayList<Move>();
        for (Move m : pseudoLegalMoves(sideToMove)) {
            if (!leavesOwnGeneralInCheck(m, sideToMove)) {
                result.add(m);
            }
        }
        return result;
    }

    public boolean isLegal(Move move) {
        if (move == null) {
            return false;
        }
        Piece moving = board.pieceAt(move.from());
        if (moving == null || moving.side() != sideToMove) {
            return false;
        }
        for (Move m : legalMoves()) {
            if (m.equals(move)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A new state with {@code move} applied and the side flipped. The resulting
     * position is appended to the board history.
     *
     * @throws IllegalArgumentException if {@code move} is not legal here
     */
    public GameState apply(Move move) {
        if (!isLegal(move)) {
            throw new IllegalArgumentException("Illegal move: " + move);
        }
        Board next = board.copy();
        Piece moving = next.pieceAt(move.from());
        next.set(move.from(), null);
        next.set(move.to(), moving);

        List<String> nextHistory = new ArrayList<String>(history);
        GameState result = new GameState(next, sideToMove.opponent(), nextHistory);
        nextHistory.add(result.signature());
        return result;
    }

    public boolean isGameOver() {
        return legalMoves().isEmpty();
    }

    /**
     * The winner, or {@code null} if the game is not over. In Xiangqi both
     * checkmate and stalemate are losses for the side that cannot move, so the
     * opponent wins in either case.
     */
    public Side winner() {
        if (legalMoves().isEmpty()) {
            return sideToMove.opponent();
        }
        return null;
    }

    /** True if {@code side}'s General is attacked (including via Flying General). */
    public boolean isInCheck(Side side) {
        Position general = board.findGeneral(side);
        if (general == null) {
            return true;
        }
        if (generalsFaceEachOther(board)) {
            return true;
        }
        return isAttacked(board, general, side.opponent());
    }

    /** True when {@code side} is to move, is in check, and has no legal move. */
    public boolean isCheckmate(Side side) {
        if (side != sideToMove) {
            return false;
        }
        return isInCheck(side) && legalMoves().isEmpty();
    }

    /** True when {@code side} is to move, is NOT in check, and has no legal move. */
    public boolean isStalemate(Side side) {
        if (side != sideToMove) {
            return false;
        }
        return !isInCheck(side) && legalMoves().isEmpty();
    }

    // ------------------------------------------------------------------
    // Pseudo-legal move generation (ignores self-check)
    // ------------------------------------------------------------------

    private List<Move> pseudoLegalMoves(Side side) {
        List<Move> moves = new ArrayList<Move>();
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Piece piece = board.pieceAt(r, c);
                if (piece == null || piece.side() != side) {
                    continue;
                }
                Position from = Position.of(r, c);
                switch (piece.type()) {
                    case GENERAL:
                        generalMoves(from, side, moves);
                        break;
                    case ADVISOR:
                        advisorMoves(from, side, moves);
                        break;
                    case ELEPHANT:
                        elephantMoves(from, side, moves);
                        break;
                    case HORSE:
                        horseMoves(from, side, moves);
                        break;
                    case CHARIOT:
                        chariotMoves(from, side, moves);
                        break;
                    case CANNON:
                        cannonMoves(from, side, moves);
                        break;
                    case SOLDIER:
                        soldierMoves(from, side, moves);
                        break;
                    default:
                        break;
                }
            }
        }
        return moves;
    }

    private void addIfTarget(Position from, int row, int col, Side side, List<Move> moves) {
        Position to = Position.of(row, col);
        if (!to.isOnBoard()) {
            return;
        }
        Piece occupant = board.pieceAt(to);
        if (occupant == null || occupant.side() != side) {
            moves.add(new Move(from, to));
        }
    }

    private void generalMoves(Position from, Side side, List<Move> moves) {
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] s : steps) {
            int r = from.row() + s[0];
            int c = from.col() + s[1];
            Position to = Position.of(r, c);
            if (to.isOnBoard() && to.isInPalace(side)) {
                Piece occupant = board.pieceAt(to);
                if (occupant == null || occupant.side() != side) {
                    moves.add(new Move(from, to));
                }
            }
        }
    }

    private void advisorMoves(Position from, Side side, List<Move> moves) {
        int[][] steps = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] s : steps) {
            int r = from.row() + s[0];
            int c = from.col() + s[1];
            Position to = Position.of(r, c);
            if (to.isOnBoard() && to.isInPalace(side)) {
                Piece occupant = board.pieceAt(to);
                if (occupant == null || occupant.side() != side) {
                    moves.add(new Move(from, to));
                }
            }
        }
    }

    private void elephantMoves(Position from, Side side, List<Move> moves) {
        int[][] steps = {{2, 2}, {2, -2}, {-2, 2}, {-2, -2}};
        for (int[] s : steps) {
            int r = from.row() + s[0];
            int c = from.col() + s[1];
            Position to = Position.of(r, c);
            if (!to.isOnBoard()) {
                continue;
            }
            // Elephant may never cross the River into the enemy half.
            if (to.isAcrossRiver(side)) {
                continue;
            }
            // Elephant-eye: the intervening diagonal point must be empty.
            Position eye = Position.of(from.row() + s[0] / 2, from.col() + s[1] / 2);
            if (board.pieceAt(eye) != null) {
                continue;
            }
            Piece occupant = board.pieceAt(to);
            if (occupant == null || occupant.side() != side) {
                moves.add(new Move(from, to));
            }
        }
    }

    private void horseMoves(Position from, Side side, List<Move> moves) {
        // Each entry: {legRow, legCol} orthogonal step, then the two diagonal
        // destinations reachable past that leg.
        int[][] legs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] leg : legs) {
            Position block = Position.of(from.row() + leg[0], from.col() + leg[1]);
            if (!block.isOnBoard() || board.pieceAt(block) != null) {
                continue; // hobbled
            }
            if (leg[0] != 0) {
                // vertical leg -> two horizontal landings
                addIfTarget(from, from.row() + 2 * leg[0], from.col() + 1, side, moves);
                addIfTarget(from, from.row() + 2 * leg[0], from.col() - 1, side, moves);
            } else {
                // horizontal leg -> two vertical landings
                addIfTarget(from, from.row() + 1, from.col() + 2 * leg[1], side, moves);
                addIfTarget(from, from.row() - 1, from.col() + 2 * leg[1], side, moves);
            }
        }
    }

    private void chariotMoves(Position from, Side side, List<Move> moves) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            int r = from.row() + d[0];
            int c = from.col() + d[1];
            while (Position.of(r, c).isOnBoard()) {
                Piece occupant = board.pieceAt(r, c);
                if (occupant == null) {
                    moves.add(new Move(from, Position.of(r, c)));
                } else {
                    if (occupant.side() != side) {
                        moves.add(new Move(from, Position.of(r, c)));
                    }
                    break;
                }
                r += d[0];
                c += d[1];
            }
        }
    }

    private void cannonMoves(Position from, Side side, List<Move> moves) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            int r = from.row() + d[0];
            int c = from.col() + d[1];
            // Non-capturing slides until the first piece (the screen).
            while (Position.of(r, c).isOnBoard() && board.pieceAt(r, c) == null) {
                moves.add(new Move(from, Position.of(r, c)));
                r += d[0];
                c += d[1];
            }
            // Skip the screen, then look for exactly one capturable target.
            r += d[0];
            c += d[1];
            while (Position.of(r, c).isOnBoard()) {
                Piece occupant = board.pieceAt(r, c);
                if (occupant != null) {
                    if (occupant.side() != side) {
                        moves.add(new Move(from, Position.of(r, c)));
                    }
                    break;
                }
                r += d[0];
                c += d[1];
            }
        }
    }

    private void soldierMoves(Position from, Side side, List<Move> moves) {
        int fwd = side.forward();
        // Forward is always available (board edge aside).
        addIfTarget(from, from.row() + fwd, from.col(), side, moves);
        // Sideways only after crossing the River.
        if (from.isAcrossRiver(side)) {
            addIfTarget(from, from.row(), from.col() + 1, side, moves);
            addIfTarget(from, from.row(), from.col() - 1, side, moves);
        }
    }

    // ------------------------------------------------------------------
    // Attack / check detection
    // ------------------------------------------------------------------

    /** True if any {@code attacker} piece pseudo-attacks {@code target} on {@code b}. */
    private boolean isAttacked(Board b, Position target, Side attacker) {
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Piece piece = b.pieceAt(r, c);
                if (piece == null || piece.side() != attacker) {
                    continue;
                }
                if (attacks(b, Position.of(r, c), piece, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean attacks(Board b, Position from, Piece piece, Position target) {
        int dr = target.row() - from.row();
        int dc = target.col() - from.col();
        switch (piece.type()) {
            case GENERAL:
                // For attack purposes the General threatens one orthogonal step
                // inside its palace (Flying General handled separately).
                return target.isInPalace(piece.side())
                        && ((Math.abs(dr) == 1 && dc == 0) || (dr == 0 && Math.abs(dc) == 1));
            case ADVISOR:
                return target.isInPalace(piece.side()) && Math.abs(dr) == 1 && Math.abs(dc) == 1;
            case ELEPHANT:
                if (Math.abs(dr) != 2 || Math.abs(dc) != 2) {
                    return false;
                }
                if (target.isAcrossRiver(piece.side())) {
                    return false;
                }
                return b.pieceAt(from.row() + dr / 2, from.col() + dc / 2) == null;
            case HORSE:
                if (!((Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2))) {
                    return false;
                }
                int legR = from.row() + (Math.abs(dr) == 2 ? dr / 2 : 0);
                int legC = from.col() + (Math.abs(dc) == 2 ? dc / 2 : 0);
                return b.pieceAt(legR, legC) == null;
            case CHARIOT:
                return (dr == 0 || dc == 0) && countBetween(b, from, target) == 0;
            case CANNON:
                return (dr == 0 || dc == 0) && countBetween(b, from, target) == 1;
            case SOLDIER:
                int fwd = piece.side().forward();
                if (dr == fwd && dc == 0) {
                    return true;
                }
                return from.isAcrossRiver(piece.side()) && dr == 0 && Math.abs(dc) == 1;
            default:
                return false;
        }
    }

    /** Number of pieces strictly between two orthogonally-aligned points, or -1 if not aligned. */
    private int countBetween(Board b, Position a, Position c) {
        if (a.row() != c.row() && a.col() != c.col()) {
            return -1;
        }
        int stepR = Integer.signum(c.row() - a.row());
        int stepC = Integer.signum(c.col() - a.col());
        int r = a.row() + stepR;
        int col = a.col() + stepC;
        int count = 0;
        while (r != c.row() || col != c.col()) {
            if (b.pieceAt(r, col) != null) {
                count++;
            }
            r += stepR;
            col += stepC;
        }
        return count;
    }

    /** True when the two Generals share a file with nothing between them. */
    private boolean generalsFaceEachOther(Board b) {
        Position red = b.findGeneral(Side.RED);
        Position black = b.findGeneral(Side.BLACK);
        if (red == null || black == null) {
            return false;
        }
        if (red.col() != black.col()) {
            return false;
        }
        return countBetween(b, red, black) == 0;
    }

    private boolean leavesOwnGeneralInCheck(Move move, Side side) {
        Board next = board.copy();
        Piece moving = next.pieceAt(move.from());
        next.set(move.from(), null);
        next.set(move.to(), moving);

        if (generalsFaceEachOther(next)) {
            return true;
        }
        Position general = next.findGeneral(side);
        if (general == null) {
            return true;
        }
        return isAttacked(next, general, side.opponent());
    }

    // ------------------------------------------------------------------
    // History
    // ------------------------------------------------------------------

    /** Immutable view of the recorded position signatures, oldest first. */
    public List<String> history() {
        return Collections.unmodifiableList(history);
    }

    /** A compact string encoding of board + side to move, used for history. */
    private String signature() {
        StringBuilder sb = new StringBuilder(100);
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Piece p = board.pieceAt(r, c);
                if (p == null) {
                    sb.append('.');
                } else {
                    char ch = pieceChar(p.type());
                    sb.append(p.side() == Side.RED ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
                }
            }
        }
        sb.append('|').append(sideToMove == Side.RED ? 'R' : 'B');
        return sb.toString();
    }

    private static char pieceChar(PieceType type) {
        switch (type) {
            case GENERAL: return 'g';
            case ADVISOR: return 'a';
            case ELEPHANT: return 'e';
            case HORSE: return 'h';
            case CHARIOT: return 'r';
            case CANNON: return 'c';
            case SOLDIER: return 's';
            default: return '?';
        }
    }
}
