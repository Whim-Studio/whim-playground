# Xiangqi (Chinese Chess) — Shared Interface Contract (Java 8)

This is the binding API contract for the Xiangqi project. All three child tasks
MUST adhere to these package names, type names, and method signatures EXACTLY so
the modules compile and link together without friction.

Build: Maven, Java 8 (`maven.compiler.source/target = 1.8`, already set in `pom.xml`).
**No `var`, no text blocks, no records, no switch expressions, no post-8 Stream
collectors (e.g. `toList()`).** Java 8 lambdas and the Java 8 Stream API are fine.

Sources live under `src/main/java/`, tests under `src/test/java/`.
Existing projects (`com.finesse.*`, `com.tiwa.*`, `com.janggi.*`) are OFF LIMITS —
do not touch, move, or rename their files. Add ONLY `com.xiangqi.*` packages.

Coordinate geometry (binding):
- Board is 10 rows (0..9) x 9 columns (0..8). Pieces sit ON intersections; row/col
  ARE the intersection indices.
- **Row 0 is BLACK's home rank (top), row 9 is RED's home rank (bottom).**
- The **River** runs between row 4 and row 5. RED's half is rows 5..9; BLACK's
  half is rows 0..4.
- **Palaces:** columns 3..5. RED palace rows 7..9. BLACK palace rows 0..2.

---

## Package: `com.xiangqi.core` — owned by Task 1

### `enum Side`
```
public enum Side {
    RED,    // moves first (bottom, rows 5..9)
    BLACK;  // top, rows 0..4
    public Side opponent();   // RED<->BLACK
    public int forward();     // RED = -1 (toward row 0), BLACK = +1 (toward row 9)
}
```

### `enum PieceType`
```
public enum PieceType {
    GENERAL, ADVISOR, ELEPHANT, HORSE, CHARIOT, CANNON, SOLDIER;
}
```

### `final class Position` (immutable value type)
```
public static Position of(int row, int col);
public int row();
public int col();
public boolean isOnBoard();              // 0<=row<=9 && 0<=col<=8
public boolean isInPalace(Side side);    // 3x3 palace for the given side
public boolean isAcrossRiver(Side side); // true once this position is on the enemy half
// value equality + hashCode + toString ("r,c")
```

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

### `class Board`
Mutable grid the engine manipulates; copyable for search.
```
public Piece pieceAt(Position p);           // null if empty
public Piece pieceAt(int row, int col);     // null if empty
public void set(Position p, Piece piece);   // piece may be null to clear
public Board copy();                         // deep copy
public Position findGeneral(Side side);      // null if captured
```

### `class GameState` — the central engine object (functional apply)
```
public static GameState initial();    // standard Xiangqi opening position, RED to move

public Board board();                 // live board (do not mutate externally)
public Side sideToMove();
public List<Move> legalMoves();       // all FULLY-legal moves for sideToMove
public boolean isLegal(Move move);

// Returns a NEW GameState with the move applied and side flipped.
// Records the resulting position into board history.
public GameState apply(Move move);

public boolean isGameOver();
public Side winner();                 // null if not over / draw

public boolean isInCheck(Side side);
public boolean isCheckmate(Side side);   // side to move has no legal move and is in check
public boolean isStalemate(Side side);   // no legal move and NOT in check (loss in Xiangqi)
```

Binding rules Task 1 MUST enforce (no shortcuts):
- **General:** moves 1 step orthogonally, confined to its 3x3 palace.
- **Advisor:** moves 1 step diagonally, confined to its palace.
- **Elephant:** moves exactly 2 points diagonally, **cannot cross the River**, and is
  blocked if the intervening diagonal point ("elephant eye") is occupied.
- **Horse:** moves one orthogonal + one diagonal (L). **Hobbled/leg-blocked**: if the
  orthogonal point it steps through is occupied, the move is illegal.
- **Chariot:** slides any distance orthogonally; blocked by pieces.
- **Cannon:** moves like a chariot when NOT capturing; to capture it must jump over
  **exactly one** intervening piece (the "screen"), of either color.
- **Soldier:** before crossing the River, moves 1 step forward only. After crossing,
  it may also move 1 step sideways. Never moves backward.
- **Flying General:** the two Generals may NEVER face each other on the same file with
  no piece between them. Any move (including one that exposes this) leaving the two
  Generals on a clear shared file is ILLEGAL.
- `legalMoves()` must already exclude moves that leave one's own General in check
  (including via the Flying General rule). `apply` keeps board history.

---

## Package: `com.xiangqi.ai` — owned by Task 2 (depends ONLY on `com.xiangqi.core`)

### `interface XiangqiAI`
```
public interface XiangqiAI {
    // Choose a move for state.sideToMove(). Must return a legal move.
    Move chooseMove(GameState state);
}
```

### `interface Evaluator`
```
public interface Evaluator {
    // Score the state from `perspective`'s point of view (higher = better), centipawns.
    int evaluate(GameState state, Side perspective);
}
```

### `class MinimaxAI implements XiangqiAI`
```
public MinimaxAI(int depth);                 // alpha-beta minimax, default Evaluator
public MinimaxAI(int depth, Evaluator eval);
```
Suggested material values (centipoints): CHARIOT 900, CANNON 450, HORSE 400,
ELEPHANT 200, ADVISOR 200, SOLDIER 100 (200 once across the River),
GENERAL very large (e.g. 1_000_000). Evaluation should also factor mobility,
board control, and palace safety. Task 2 owns tuning.

### Coach types (the educational tutor) — owned by Task 2

```
public final class MoveAdvice {
    public MoveAdvice(Move move, int score, String explanation);
    public Move move();
    public int score();            // evaluation of this move for the advised side (centipoints)
    public String explanation();   // plain-English reason, e.g.
                                   // "Moving your Cannon to (2,7) forks the Chariot and Horse."
}

public interface Coach {
    // Return up to `n` of the strongest moves for state.sideToMove(),
    // best first, each with a plain-English explanation. Used by the UI in
    // Coach mode to highlight squares and show text. Never returns null.
    java.util.List<MoveAdvice> topMoves(GameState state, int n);
}

public class XiangqiCoach implements Coach {
    public XiangqiCoach();          // builds on MinimaxAI + Evaluator
    public XiangqiCoach(int depth);
}
```
The Coach reads the minimax evaluation to rank the side-to-move's candidate moves
and translates the evaluation deltas (material gained, captures threatened/forked,
checks, palace safety changes) into a concise human sentence. Pure logic — NO Swing.

---

## Package: `com.xiangqi.ui` — owned by Task 3 (depends on `com.xiangqi.core` + `com.xiangqi.ai`)

Swing UI. Render the 9x10 grid with the River and both Palaces, draw pieces ON the
intersections, support click-to-move (click own piece -> highlight legal targets ->
click target). Two modes selectable at startup: **Two Player (local)** and
**Player vs Computer**. In vs-Computer mode the engine side's move comes from
`new com.xiangqi.ai.MinimaxAI(depth)`.

**Coach Mode** (toggle, only meaningful in vs-Computer mode): when ON and it is the
human's turn, call `Coach.topMoves(state, k)`, highlight the recommended target
square(s) on the board, and render the top move's `explanation()` text in a side
panel (or tooltip). Render Chinese piece glyphs (or single letters as fallback);
RED vs BLACK clearly distinguished by color.

### `class Main`
```
public static void main(String[] args);      // launches the Swing app via SwingUtilities.invokeLater
```
Drive the game purely through `GameState` (`legalMoves`, `apply`, `isGameOver`,
`winner`, `isInCheck`). Construct the AI/Coach from `com.xiangqi.ai`.

---

## Integration contract summary
- Task 1 publishes `com.xiangqi.core` exactly as above + a JUnit suite under
  `src/test/java/com/xiangqi/core/`.
- Task 2 imports `com.xiangqi.core` only; exposes `XiangqiAI`, `Evaluator`,
  `MinimaxAI`, `Coach`, `XiangqiCoach`, `MoveAdvice`.
- Task 3 imports `com.xiangqi.core` + `com.xiangqi.ai`; provides `com.xiangqi.ui.Main`.
- Nobody edits another package's files. Each task pushes its branch and opens a PR
  into `whim-wd-163`, then reports back to the orchestrator via send_prompt.
