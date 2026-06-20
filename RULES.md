# Finesse Chess — Rules

> **Status of these rules.** Walter Browne's "Finesse" chess variant has no
> authoritative, fully recoverable ruleset available to this project. The rules
> below are therefore a **coherent, self-consistent ruleset** defined for this
> digital implementation. Every rule and every assumption is documented
> explicitly here. **This document is the contract**: the test suite (Task 3)
> validates the engine against exactly what is written below. Where a choice was
> made in the absence of a canonical source, it is flagged **[ASSUMPTION]**.

---

## 1. Board

- The board is **8×8**, the same dimensions as standard chess.
- Coordinates use `Position(file, rank)` with **0-based** indices:
  - `file` ranges `0..7` (file 0 = the "a-file", file 7 = the "h-file").
  - `rank` ranges `0..7` (rank 0 = White's back rank, rank 7 = Black's back rank).
- A square is **in bounds** iff `0 <= file <= 7 && 0 <= rank <= 7`
  (`Board.isInBounds`).

## 2. Colors and the moving side

- Two colors: `WHITE` and `BLACK`.
- White's pieces start on ranks 0–1; Black's pieces start on ranks 6–7.
- White "forward" is toward increasing rank (+1); Black "forward" is toward
  decreasing rank (−1).
- `GameState.sideToMove` names whose turn it is. The move generator only ever
  generates moves for a piece whose color equals `sideToMove`.

## 3. Starting position

The back ranks replace standard chess's **bishops** with the variant piece, the
**Finesse** (`F`). Every other piece is in its standard chess square.

```
rank 7 (Black):  r  n  F  q  k  F  n  r
rank 6 (Black):  p  p  p  p  p  p  p  p
rank 5        :  .  .  .  .  .  .  .  .
rank 4        :  .  .  .  .  .  .  .  .
rank 3        :  .  .  .  .  .  .  .  .
rank 2        :  .  .  .  .  .  .  .  .
rank 1 (White):  P  P  P  P  P  P  P  P
rank 0 (White):  R  N  F  Q  K  F  N  R
                 0  1  2  3  4  5  6  7   <- file
```

Explicit placement (file, rank):

| Piece              | White (rank 0/1)              | Black (rank 7/6)              |
|--------------------|-------------------------------|-------------------------------|
| Rook   (`R`/`r`)   | (0,0), (7,0)                  | (0,7), (7,7)                  |
| Knight (`N`/`n`)   | (1,0), (6,0)                  | (1,7), (6,7)                  |
| **Finesse** (`F`)  | (2,0), (5,0)                  | (2,7), (5,7)                  |
| Queen  (`Q`/`q`)   | (3,0)                         | (3,7)                         |
| King   (`K`/`k`)   | (4,0)                         | (4,7)                         |
| Pawn   (`P`/`p`)   | (0..7, 1)                     | (0..7, 6)                     |

- **[ASSUMPTION]** The Queen starts on file 3 and the King on file 4 for both
  colors (standard "queen on her color" is *not* mirrored; both colors use the
  same file layout, which keeps the setup symmetric by reflection across the
  center rank).

## 4. Piece movement

A **move** produced by the generator is a destination `Position`. A piece may
never move onto a square occupied by a **friendly** piece. Sliding pieces are
blocked by the first piece they meet along a ray (they may capture it if it is
an enemy, then stop).

### 4.1 Standard pieces (unchanged from chess)

- **Rook** — slides any distance along the 4 orthogonal directions. Captures the
  first enemy piece on a ray.
- **Knight** — leaps to the 8 `(±1,±2)/(±2,±1)` squares. Not blocked by
  intervening pieces. Captures an enemy on the target square.
- **Queen** — slides any distance along the 8 orthogonal + diagonal directions.
- **King** — moves one square in any of the 8 directions. Captures an enemy on
  the target square.
- **Pawn**:
  - Moves **forward one** square to an empty square.
  - From its **starting rank** (White rank 1, Black rank 6) may move **forward
    two** squares, only if both the intermediate and target squares are empty.
  - **Captures** one square **diagonally forward** onto an enemy-occupied square.
  - **Promotion**: a pawn that reaches the far rank (White rank 7, Black rank 0)
    is promoted to a **Queen**. **[ASSUMPTION]** promotion is always to Queen.
  - **[ASSUMPTION]** There is **no en passant** in this variant.

### 4.2 The Finesse (`F`) — the signature variant piece

The Finesse separates *moving* from *capturing* — the namesake "finesse":

- **Quiet move (no capture):** the Finesse slides any distance along the **4
  diagonal directions**, exactly like a bishop. It is blocked by any piece
  (friendly or enemy) on the diagonal and may **never capture along the
  diagonal** — a diagonal ray ends at (and excludes) the first occupied square.
- **Capture:** the Finesse captures **only** by a **knight's leap** — it may
  capture an enemy piece sitting on one of its 8 `(±1,±2)/(±2,±1)` squares. It
  may **not** make a non-capturing move to a knight square, and it cannot use
  the knight leap to land on an empty or friendly square.

In short: **moves like a bishop, captures like a knight.** A Finesse glides
quietly on the diagonals and strikes only with a knight's jump.

## 5. Scope of `legalMoves` (engine contract)

- `MoveGenerator.legalMoves(GameState, Position from)` returns the list of legal
  **destination `Position`s** for the piece on `from`.
- If `from` is empty, out of bounds, or holds a piece **not** of `sideToMove`,
  the result is an **empty list**.
- **[ASSUMPTION] Check / checkmate are out of scope.** `legalMoves` returns
  moves that are legal *by movement, board-bounds, and friendly-occupancy
  rules*. It does **not** filter out moves that would leave one's own king in
  check, and there is no check/checkmate/stalemate detection. Self-check
  legality, castling, and the 50-move/repetition rules are intentionally not
  modeled in this implementation and are not tested.
