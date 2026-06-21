# Janggi (Korean Chess) — Shared Interface Contract (Java 8)

This is the binding API contract for the Janggi project. All three child tasks
MUST adhere to these package names, type names, and method signatures exactly so
the modules compile and link together without friction.

Build: Maven, Java 8 (`maven.compiler.source/target = 1.8`). **No `var`, no text
blocks, no records, no switch expressions, no post-8 Stream collectors.** Java 8
lambdas/streams are fine.

Sources live under `src/main/java/`, tests under `src/test/java/`.
Existing projects (`com.finesse.*`, `com.tiwa.*`) are OFF LIMITS — do not touch them.

---

## Package: `com.janggi.core` — owned by Task 1

### `enum Side`
```
public enum Side {
    CHO,   // 초 (green) — moves first
    HAN;   // 한 (red)
    public Side opponent();   // CHO<->HAN
}
```

### `enum PieceType`
```
public enum PieceType {
    GENERAL, GUARD, HORSE, ELEPHANT, CHARIOT, CANNON, SOLDIER;
}
```

### `final class Position` (immutable value type)
Board is 10 rows (0..9) x 9 columns (0..8). Pieces sit on intersections; row/col
ARE the intersection indices. Row 0 is the HAN home rank, row 9 is the CHO home rank.
```
public static Position of(int row, int col);
public int row();
public int col();
public boolean isOnBoard();                 // 0<=row<=9 && 0<=col<=8
public boolean isInPalace(Side side);       // 3x3 palace for the given side
// value equality + hashCode + toString ("r,c")
```
Palaces: columns 3..5. CHO palace rows 7..9. HAN palace rows 0..2.

### `final class Piece` (immutable)
```
public Piece(Side side, PieceType type);
public Side side();
public PieceType type();
```

### `final class Move` (immutable value type)
```
public Move(Position from, Position to);
public Position from();
public Position to();
// value equality + hashCode + toString
```
A "pass" move (legal in Janggi for any side) is represented as
`new Move(p, p)` where from == to. Helper: `public boolean isPass();`

### `class Board`
Mutable grid the engine manipulates; copyable for search.
```
public Piece pieceAt(Position p);           // null if empty
public Piece pieceAt(int row, int col);     // null if empty
public void set(Position p, Piece piece);   // piece may be null to clear
public Board copy();                         // deep copy
public Position findGeneral(Side side);      // null if captured
```

### `final class SetupChoice`
The pre-game Elephant/Horse transposition. Standard Janggi has 4 arrangements of
the two left-of-general and two right-of-general minor pieces (Horse=M, Elephant=S),
read across the back rank columns 1,2 (left wing) and 6,7 (right wing).
```
public enum Arrangement { MSSM, SMMS, MSMS, SMSM; } // inside-out per convention below
public SetupChoice(Arrangement arrangement);
public Arrangement arrangement();
```
Convention for `Arrangement` letters = pieces on columns [1,2,6,7] for that side's
back rank, where M=HORSE, S=ELEPHANT. MSSM is the orthodox "both elephants inside".
Task 1 owns the exact column mapping; UI only passes an `Arrangement` per side.

### `class GameState` — the central engine object (functional apply)
```
// Build the opening position from each side's chosen transposition.
public static GameState initial(SetupChoice choSetup, SetupChoice hanSetup);

public Board board();                 // live board (do not mutate externally)
public Side sideToMove();
public List<Move> legalMoves();       // all legal moves for sideToMove, incl. pass
public boolean isLegal(Move move);

// Returns a NEW GameState with the move applied and side flipped.
// Records the resulting position into repetition history.
public GameState apply(Move move);

public boolean isGameOver();
public Side winner();                 // null if not over

// Check / checkmate (Janggi has checkmate; the General is captured-by-threat).
public boolean isInCheck(Side side);

// Repetition rule: a move that would create a 3rd occurrence of a position
// (same board + same side to move) is ILLEGAL and excluded from legalMoves().
public boolean wouldRepeat(Move move);
}
```
Notes for Task 1:
- Movement rules: General/Guard confined to palace incl. palace diagonals.
  Horse & Elephant are blockable (leg-block). Chariot slides orthogonally and along
  palace diagonals. Cannon needs exactly one screen to move/capture, CANNOT use a
  Cannon as the screen, and CANNOT capture a Cannon. Soldier moves forward/sideways
  and gains palace diagonals. No draws: repetition is forbidden (see above).
- `apply` must keep enough history (map of position-key -> count) so `wouldRepeat`
  and `legalMoves` enforce threefold-repetition avoidance.

---

## Package: `com.janggi.ai` — owned by Task 2 (depends only on `com.janggi.core`)

### `interface JanggiAI`
```
public interface JanggiAI {
    // Choose a move for state.sideToMove(). Must return a legal move
    // (may be a pass if that is the only/best legal option).
    Move chooseMove(GameState state);
}
```

### `interface Evaluator`
```
public interface Evaluator {
    // Score the state from `perspective`'s point of view (higher = better).
    int evaluate(GameState state, Side perspective);
}
```

### `class MinimaxAI implements JanggiAI`
```
public MinimaxAI(int depth);                 // alpha-beta minimax, default Evaluator
public MinimaxAI(int depth, Evaluator eval);
```
Suggested piece values (centi-points): CHARIOT 1300, CANNON 700, HORSE 500,
ELEPHANT 300, GUARD 300, SOLDIER 200, GENERAL very large (e.g. 1_000_000).
Evaluation should also factor mobility and palace safety. Task 2 owns tuning.

---

## Package: `com.janggi.ui` — owned by Task 3 (depends only on `com.janggi.core` + `com.janggi.ai`)

Swing UI. Render the 9x10 grid with pieces drawn ON intersections, click-to-move,
and a pre-game setup screen letting each human pick a `SetupChoice.Arrangement`.
In "vs Computer" mode, obtain HAN's (or chosen side's) moves via `JanggiAI`.

### `class Main`
```
public static void main(String[] args);      // launches the Swing app
```
Use `SwingUtilities.invokeLater`. Construct AI as `new com.janggi.ai.MinimaxAI(depth)`.
Drive the game purely through `GameState` (`legalMoves`, `apply`, `isGameOver`, `winner`).

---

## Integration contract summary
- Task 1 publishes `com.janggi.core` exactly as above.
- Task 2 imports `com.janggi.core` only; exposes `JanggiAI` / `MinimaxAI`.
- Task 3 imports `com.janggi.core` + `com.janggi.ai`; provides `Main`.
- Nobody edits another package's files. Report back to the orchestrator via send_prompt.
