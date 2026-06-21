package com.janggi.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central engine object. Immutable from the caller's perspective: {@link #apply}
 * returns a new state with the move played, side flipped, and the resulting
 * position recorded into repetition history.
 *
 * <p>Movement rules implemented (Janggi / Korean Chess):
 * <ul>
 *   <li>General &amp; Guard confined to the palace, including the palace diagonal lines.</li>
 *   <li>Horse and Elephant are leg-blockable (intermediate points must be empty).</li>
 *   <li>Chariot slides orthogonally and along palace diagonals.</li>
 *   <li>Cannon needs exactly one non-Cannon screen to move/capture and may not
 *       capture a Cannon.</li>
 *   <li>Soldier moves forward/sideways (never backward) and gains palace diagonal steps.</li>
 *   <li>"Pass" ({@code new Move(p, p)}) is legal unless it would leave the General in check.</li>
 *   <li>No draws: a move creating a 3rd occurrence of a position is illegal.</li>
 *   <li>Bikjang: a move producing an unobstructed vertical alignment of the two
 *       Generals (same file, no intervening pieces) is illegal and filtered out.</li>
 * </ul>
 */
public class GameState {

    private static final int[][] ORTHO = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private static final int[][] DIAG = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    private final Board board;
    private final Side sideToMove;
    /** position-key (board + side to move) -> number of times it has occurred. */
    private final Map<String, Integer> history;

    private GameState(Board board, Side sideToMove, Map<String, Integer> history) {
        this.board = board;
        this.sideToMove = sideToMove;
        this.history = history;
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    /** Build the opening position from each side's chosen transposition. CHO moves first. */
    public static GameState initial(SetupChoice choSetup, SetupChoice hanSetup) {
        Board b = new Board();
        placeSide(b, Side.HAN, hanSetup);
        placeSide(b, Side.CHO, choSetup);
        Map<String, Integer> hist = new HashMap<String, Integer>();
        hist.put(keyOf(b, Side.CHO), 1);
        return new GameState(b, Side.CHO, hist);
    }

    /**
     * Package-private factory for building a state from an arbitrary board (test
     * fixtures and engine internals). The supplied board is recorded as the first
     * occurrence in repetition history.
     */
    static GameState fromBoard(Board board, Side sideToMove) {
        Map<String, Integer> hist = new HashMap<String, Integer>();
        hist.put(keyOf(board, sideToMove), 1);
        return new GameState(board, sideToMove, hist);
    }

    private static void placeSide(Board b, Side side, SetupChoice setup) {
        int home = (side == Side.HAN) ? 0 : 9;
        int genRow = (side == Side.HAN) ? 1 : 8;
        int cannonRow = (side == Side.HAN) ? 2 : 7;
        int soldierRow = (side == Side.HAN) ? 3 : 6;

        b.set(Position.of(home, 0), new Piece(side, PieceType.CHARIOT));
        b.set(Position.of(home, 8), new Piece(side, PieceType.CHARIOT));
        b.set(Position.of(home, 3), new Piece(side, PieceType.GUARD));
        b.set(Position.of(home, 5), new Piece(side, PieceType.GUARD));

        // Minor pieces on columns 1, 2, 6, 7 per the arrangement (M=HORSE, S=ELEPHANT).
        char[] letters = setup.arrangement().name().toCharArray();
        int[] cols = {1, 2, 6, 7};
        for (int i = 0; i < 4; i++) {
            PieceType t = (letters[i] == 'M') ? PieceType.HORSE : PieceType.ELEPHANT;
            b.set(Position.of(home, cols[i]), new Piece(side, t));
        }

        b.set(Position.of(genRow, 4), new Piece(side, PieceType.GENERAL));
        b.set(Position.of(cannonRow, 1), new Piece(side, PieceType.CANNON));
        b.set(Position.of(cannonRow, 7), new Piece(side, PieceType.CANNON));
        int[] soldierCols = {0, 2, 4, 6, 8};
        for (int c : soldierCols) {
            b.set(Position.of(soldierRow, c), new Piece(side, PieceType.SOLDIER));
        }
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public Board board() {
        return board;
    }

    public Side sideToMove() {
        return sideToMove;
    }

    // ------------------------------------------------------------------
    // Legal moves
    // ------------------------------------------------------------------

    /** All legal moves for the side to move, including a legal pass. */
    public List<Move> legalMoves() {
        List<Move> result = new ArrayList<Move>();
        for (Move m : pseudoLegalMoves(board, sideToMove)) {
            if (leavesOwnGeneralSafe(m) && !createsBikjang(m) && !wouldRepeat(m)) {
                result.add(m);
            }
        }
        // Pass (canonical representation: from == to == own general's square).
        Position gen = board.findGeneral(sideToMove);
        if (gen != null) {
            Move pass = new Move(gen, gen);
            if (!checkOnBoard(board, sideToMove) && !createsBikjang(pass) && !wouldRepeat(pass)) {
                result.add(pass);
            }
        }
        return result;
    }

    public boolean isLegal(Move move) {
        if (move.isPass()) {
            if (board.findGeneral(sideToMove) == null) {
                return false;
            }
            return !checkOnBoard(board, sideToMove) && !createsBikjang(move) && !wouldRepeat(move);
        }
        return legalMoves().contains(move);
    }

    /** A move whose resulting position already occurs twice (i.e. would be its 3rd occurrence). */
    public boolean wouldRepeat(Move move) {
        Board nb = applyToBoard(move);
        String key = keyOf(nb, sideToMove.opponent());
        Integer existing = history.get(key);
        return existing != null && existing >= 2;
    }

    private boolean leavesOwnGeneralSafe(Move move) {
        Board nb = applyToBoard(move);
        return !checkOnBoard(nb, sideToMove);
    }

    /** A move is illegal if the resulting position is a Bikjang (facing Generals). */
    private boolean createsBikjang(Move move) {
        return isBikjang(applyToBoard(move));
    }

    /**
     * Bikjang ("facing Generals"): both Generals occupy the same column with zero
     * intervening pieces on the intermediate intersections between them, giving an
     * unobstructed vertical line of sight. In this variant Bikjang is forbidden, so
     * any move producing it is filtered out of {@link #legalMoves()} alongside the
     * self-check and threefold-repetition filters. Because the AI explores only via
     * {@code legalMoves()}/{@code apply()}, it can never generate or search a Bikjang.
     */
    private static boolean isBikjang(Board b) {
        Position cho = b.findGeneral(Side.CHO);
        Position han = b.findGeneral(Side.HAN);
        if (cho == null || han == null) {
            return false; // a captured General cannot face anything
        }
        if (cho.col() != han.col()) {
            return false; // different files -> no vertical alignment
        }
        int col = cho.col();
        int lo = Math.min(cho.row(), han.row());
        int hi = Math.max(cho.row(), han.row());
        for (int r = lo + 1; r < hi; r++) {
            if (b.pieceAt(r, col) != null) {
                return false; // an intervening piece blocks the line of sight
            }
        }
        return true; // same file, clear line -> Bikjang
    }

    // ------------------------------------------------------------------
    // Apply
    // ------------------------------------------------------------------

    /** Returns a NEW GameState with the move applied, side flipped, position recorded. */
    public GameState apply(Move move) {
        Board nb = applyToBoard(move);
        Side ns = sideToMove.opponent();
        Map<String, Integer> newHist = new HashMap<String, Integer>(history);
        String key = keyOf(nb, ns);
        Integer prev = newHist.get(key);
        newHist.put(key, (prev == null ? 0 : prev) + 1);
        return new GameState(nb, ns, newHist);
    }

    private Board applyToBoard(Move move) {
        Board nb = board.copy();
        if (!move.isPass()) {
            Piece p = nb.pieceAt(move.from());
            nb.set(move.to(), p);
            nb.set(move.from(), null);
        }
        return nb;
    }

    // ------------------------------------------------------------------
    // Game termination
    // ------------------------------------------------------------------

    public boolean isGameOver() {
        if (board.findGeneral(Side.CHO) == null || board.findGeneral(Side.HAN) == null) {
            return true;
        }
        return legalMoves().isEmpty();
    }

    public Side winner() {
        Position cho = board.findGeneral(Side.CHO);
        Position han = board.findGeneral(Side.HAN);
        if (cho == null) {
            return Side.HAN;
        }
        if (han == null) {
            return Side.CHO;
        }
        if (legalMoves().isEmpty()) {
            // Side to move is trapped (checkmated / stalemated -> loss, no draws).
            return sideToMove.opponent();
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Check detection
    // ------------------------------------------------------------------

    public boolean isInCheck(Side side) {
        return checkOnBoard(board, side);
    }

    /** True if {@code side}'s General is missing or attacked by any enemy piece on {@code b}. */
    private static boolean checkOnBoard(Board b, Side side) {
        Position gen = b.findGeneral(side);
        if (gen == null) {
            return true;
        }
        for (Move m : pseudoLegalMoves(b, side.opponent())) {
            if (m.to().equals(gen)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Pseudo-legal move generation (no self-check / repetition filtering)
    // ------------------------------------------------------------------

    private static List<Move> pseudoLegalMoves(Board b, Side side) {
        List<Move> moves = new ArrayList<Move>();
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Piece p = b.pieceAt(r, c);
                if (p == null || p.side() != side) {
                    continue;
                }
                Position from = Position.of(r, c);
                switch (p.type()) {
                    case GENERAL:
                    case GUARD:
                        generalGuardMoves(b, from, side, moves);
                        break;
                    case SOLDIER:
                        soldierMoves(b, from, side, moves);
                        break;
                    case HORSE:
                        horseMoves(b, from, side, moves);
                        break;
                    case ELEPHANT:
                        elephantMoves(b, from, side, moves);
                        break;
                    case CHARIOT:
                        chariotMoves(b, from, side, moves);
                        break;
                    case CANNON:
                        cannonMoves(b, from, side, moves);
                        break;
                    default:
                        break;
                }
            }
        }
        return moves;
    }

    private static boolean isOwn(Board b, Position p, Side side) {
        Piece q = b.pieceAt(p);
        return q != null && q.side() == side;
    }

    // ---- General & Guard ----

    private static void generalGuardMoves(Board b, Position from, Side side, List<Move> moves) {
        for (int[] d : ORTHO) {
            Position to = Position.of(from.row() + d[0], from.col() + d[1]);
            if (to.isInPalace(side) && !isOwn(b, to, side)) {
                moves.add(new Move(from, to));
            }
        }
        for (Position to : palaceDiagonalNeighbors(from, side)) {
            if (!isOwn(b, to, side)) {
                moves.add(new Move(from, to));
            }
        }
    }

    // ---- Soldier ----

    private static void soldierMoves(Board b, Position from, Side side, List<Move> moves) {
        int dir = (side == Side.CHO) ? -1 : 1; // CHO advances up (toward row 0), HAN down.
        addStep(b, from, Position.of(from.row() + dir, from.col()), side, moves); // forward
        addStep(b, from, Position.of(from.row(), from.col() - 1), side, moves);    // sideways
        addStep(b, from, Position.of(from.row(), from.col() + 1), side, moves);    // sideways

        // Forward diagonal steps along palace lines (in whichever palace the soldier stands).
        Side palace = palaceSideOf(from);
        if (palace != null) {
            for (Position to : palaceDiagonalNeighbors(from, palace)) {
                if (to.row() - from.row() == dir && !isOwn(b, to, side)) {
                    moves.add(new Move(from, to));
                }
            }
        }
    }

    private static void addStep(Board b, Position from, Position to, Side side, List<Move> moves) {
        if (to.isOnBoard() && !isOwn(b, to, side)) {
            moves.add(new Move(from, to));
        }
    }

    // ---- Horse (leg-blockable knight) ----

    private static void horseMoves(Board b, Position from, Side side, List<Move> moves) {
        int r = from.row();
        int c = from.col();
        for (int[] d : ORTHO) {
            Position leg = Position.of(r + d[0], c + d[1]);
            if (!leg.isOnBoard() || b.pieceAt(leg) != null) {
                continue; // leg blocked
            }
            if (d[0] != 0) { // vertical leg -> two diagonal destinations
                addDest(b, from, Position.of(r + 2 * d[0], c - 1), side, moves);
                addDest(b, from, Position.of(r + 2 * d[0], c + 1), side, moves);
            } else {         // horizontal leg
                addDest(b, from, Position.of(r - 1, c + 2 * d[1]), side, moves);
                addDest(b, from, Position.of(r + 1, c + 2 * d[1]), side, moves);
            }
        }
    }

    // ---- Elephant (double leg-block) ----

    private static void elephantMoves(Board b, Position from, Side side, List<Move> moves) {
        int r = from.row();
        int c = from.col();
        for (int[] d : ORTHO) {
            Position leg = Position.of(r + d[0], c + d[1]);
            if (!leg.isOnBoard() || b.pieceAt(leg) != null) {
                continue; // first leg blocked
            }
            if (d[0] != 0) { // vertical first step
                for (int s : new int[]{-1, 1}) {
                    Position mid = Position.of(r + 2 * d[0], c + s);
                    Position dest = Position.of(r + 3 * d[0], c + 2 * s);
                    if (mid.isOnBoard() && b.pieceAt(mid) == null) {
                        addDest(b, from, dest, side, moves);
                    }
                }
            } else {         // horizontal first step
                for (int s : new int[]{-1, 1}) {
                    Position mid = Position.of(r + s, c + 2 * d[1]);
                    Position dest = Position.of(r + 2 * s, c + 3 * d[1]);
                    if (mid.isOnBoard() && b.pieceAt(mid) == null) {
                        addDest(b, from, dest, side, moves);
                    }
                }
            }
        }
    }

    private static void addDest(Board b, Position from, Position dest, Side side, List<Move> moves) {
        if (dest.isOnBoard() && !isOwn(b, dest, side)) {
            moves.add(new Move(from, dest));
        }
    }

    // ---- Chariot ----

    private static void chariotMoves(Board b, Position from, Side side, List<Move> moves) {
        for (int[] d : ORTHO) {
            slideRay(b, from, orthoRay(from, d[0], d[1]), side, moves);
        }
        for (List<Position> ray : diagonalRays(from)) {
            slideRay(b, from, ray, side, moves);
        }
    }

    private static void slideRay(Board b, Position from, List<Position> ray, Side side, List<Move> moves) {
        for (Position to : ray) {
            Piece p = b.pieceAt(to);
            if (p == null) {
                moves.add(new Move(from, to));
            } else {
                if (p.side() != side) {
                    moves.add(new Move(from, to)); // capture
                }
                break;
            }
        }
    }

    // ---- Cannon ----

    private static void cannonMoves(Board b, Position from, Side side, List<Move> moves) {
        for (int[] d : ORTHO) {
            cannonRay(b, from, orthoRay(from, d[0], d[1]), side, moves);
        }
        for (List<Position> ray : diagonalRays(from)) {
            cannonRay(b, from, ray, side, moves);
        }
    }

    private static void cannonRay(Board b, Position from, List<Position> ray, Side side, List<Move> moves) {
        boolean screenFound = false;
        for (Position to : ray) {
            Piece p = b.pieceAt(to);
            if (!screenFound) {
                if (p == null) {
                    continue;
                }
                if (p.type() == PieceType.CANNON) {
                    return; // a Cannon may not be used as a screen
                }
                screenFound = true;
            } else {
                if (p == null) {
                    moves.add(new Move(from, to)); // jump to empty
                } else {
                    if (p.type() != PieceType.CANNON && p.side() != side) {
                        moves.add(new Move(from, to)); // capture (never a Cannon)
                    }
                    break; // first piece beyond the screen stops the ray
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Geometry helpers
    // ------------------------------------------------------------------

    private static List<Position> orthoRay(Position from, int dr, int dc) {
        List<Position> ray = new ArrayList<Position>();
        int r = from.row() + dr;
        int c = from.col() + dc;
        while (r >= 0 && r < Board.ROWS && c >= 0 && c < Board.COLS) {
            ray.add(Position.of(r, c));
            r += dr;
            c += dc;
        }
        return ray;
    }

    /** Which side's palace contains this position (null if none). */
    private static Side palaceSideOf(Position p) {
        if (p.isInPalace(Side.CHO)) {
            return Side.CHO;
        }
        if (p.isInPalace(Side.HAN)) {
            return Side.HAN;
        }
        return null;
    }

    /** A palace's diagonal points are its center and its four corners. */
    private static boolean isPalaceDiagonalPoint(Position p, Side palace) {
        if (!p.isInPalace(palace)) {
            return false;
        }
        int centerRow = (palace == Side.CHO) ? 8 : 1;
        if (p.col() == 4 && p.row() == centerRow) {
            return true; // center
        }
        return (p.col() == 3 || p.col() == 5)
                && (p.row() == centerRow - 1 || p.row() == centerRow + 1); // corner
    }

    /** Single-step palace-diagonal neighbors of {@code from} within {@code palace}. */
    private static List<Position> palaceDiagonalNeighbors(Position from, Side palace) {
        List<Position> out = new ArrayList<Position>();
        if (!isPalaceDiagonalPoint(from, palace)) {
            return out;
        }
        for (int[] d : DIAG) {
            Position to = Position.of(from.row() + d[0], from.col() + d[1]);
            if (isPalaceDiagonalPoint(to, palace)) {
                out.add(to);
            }
        }
        return out;
    }

    /** Diagonal sliding rays (for Chariot/Cannon) along palace lines from {@code from}. */
    private static List<List<Position>> diagonalRays(Position from) {
        List<List<Position>> rays = new ArrayList<List<Position>>();
        Side palace = palaceSideOf(from);
        if (palace == null || !isPalaceDiagonalPoint(from, palace)) {
            return rays;
        }
        for (int[] d : DIAG) {
            List<Position> ray = new ArrayList<Position>();
            int r = from.row() + d[0];
            int c = from.col() + d[1];
            while (true) {
                Position pos = Position.of(r, c);
                if (!isPalaceDiagonalPoint(pos, palace)) {
                    break;
                }
                ray.add(pos);
                r += d[0];
                c += d[1];
            }
            if (!ray.isEmpty()) {
                rays.add(ray);
            }
        }
        return rays;
    }

    // ------------------------------------------------------------------
    // Position key (repetition tracking)
    // ------------------------------------------------------------------

    private static String keyOf(Board b, Side toMove) {
        StringBuilder sb = new StringBuilder(Board.ROWS * Board.COLS + 1);
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Piece p = b.pieceAt(r, c);
                sb.append(p == null ? '.' : pieceChar(p));
            }
        }
        sb.append(toMove == Side.CHO ? 'c' : 'h');
        return sb.toString();
    }

    private static char pieceChar(Piece p) {
        char base;
        switch (p.type()) {
            case GENERAL:  base = 'G'; break;
            case GUARD:    base = 'U'; break;
            case HORSE:    base = 'H'; break;
            case ELEPHANT: base = 'E'; break;
            case CHARIOT:  base = 'R'; break;
            case CANNON:   base = 'C'; break;
            default:       base = 'S'; break; // SOLDIER
        }
        // CHO uppercase, HAN lowercase.
        return p.side() == Side.CHO ? base : Character.toLowerCase(base);
    }

    /** Read-only view of the repetition history (position-key -> occurrence count). */
    public Map<String, Integer> repetitionHistory() {
        return Collections.unmodifiableMap(history);
    }
}
