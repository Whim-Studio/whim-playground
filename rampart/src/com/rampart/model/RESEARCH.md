# Rampart (1990, Atari Games) — Model-Layer Research & Constants

This document is the **single source of truth** for the domain rules and the exact
numeric constants the `com.rampart.model` layer chose. Task 2 (engine) and Task 3
(ui) must read these from `Rules` (and the `*View` interfaces) rather than
re-guessing or hardcoding magic numbers. It captures the *rules the data encodes*;
it does **not** implement them (no flood-fill, AI, collision, or rendering lives in
this package).

## The three-phase loop

Rampart is a single-screen game played over a repeating loop of three timed phases,
represented by `Phase`:

1. **BUILD (cannon placement)** — `Phase.BUILD`, `Rules.BUILD_PHASE_MILLIS = 15000`.
   The player places cannons on **enclosed** land (land fully surrounded by an
   unbroken wall loop around a castle). A fixed pool of cannons is available per
   round: `Rules.cannonPoolForRound(round)` = `CANNON_POOL_BASE(3)` +
   `CANNON_POOL_PER_ROUND(1)` per extra round, clamped to `CANNON_MAX(8)`. A cannon
   occupies `CANNON_FOOTPRINT(1)` cell.
2. **BATTLE** — `Phase.BATTLE`, `Rules.BATTLE_PHASE_MILLIS = 30000`. Enemy ships
   sail the surrounding water and bombard walls, turning `WALL` tiles into `RUBBLE`
   and opening gaps. The player fires cannons at target cells; each shot has a
   `CANNON_RELOAD_MILLIS(800)` cooldown and a `CANNON_BLAST_RADIUS(1)` impact area.
   Ships have per-type health (`ShipType.baseHealth`) and sink when depleted.
3. **REPAIR** — `Phase.REPAIR`, `Rules.REPAIR_PHASE_MILLIS = 20000`. The player is
   dealt Tetris-like polyomino wall pieces one at a time (`REPAIR_QUEUE_SIZE = 3`
   previewed) to re-seal broken loops and grow territory. Pieces may not overlap
   water, walls, cannons, or castles (validated by the **engine**, not here).

Between rounds: `Phase.ROUND_TRANSITION` (`ROUND_TRANSITION_MILLIS = 2500`) then the
next `BUILD`. Failure ends at `Phase.GAME_OVER`. `Phase.TITLE` is the attract state.

## Round-survival rule (critical)

At the **end of the REPAIR phase**, the player must have at least
`Rules.MIN_ENCLOSED_CASTLES_TO_SURVIVE = 1` castle completely enclosed by an
unbroken wall loop. If none is enclosed, the game is over. The engine computes
enclosure with a flood-fill and writes the result back onto the model:
`Castle.setEnclosed(...)` / `Castle.setTerritory(...)` and `Tile.setEnclosed(...)`.
`GameState.enclosedCastleCount()` and `Castle.territory()` are the read-only
surfaces the UI/engine consume.

## Grid & coordinates

- Grid: **`GRID_COLS = 30` × `GRID_ROWS = 22`** tiles (`Grid`, `Tile`).
- Coordinates are `(col, row)` via the single immutable `Coord` type. Column is x
  (east positive); row is y (**south positive**, so north = row − 1).
- Tile kinds (`TileType`): `WATER, LAND, WALL, RUBBLE, CANNON, CASTLE`.
- Tiles default to `WATER`; `LevelData` decodes the real map.

## Wall-piece shapes

`WallShape` = `DOT, I, O, T, L, J, S, Z` — the seven standard tetrominoes plus a
one-cell `DOT` for patching. `WallPiece` precomputes **`PIECE_ROTATIONS = 4`**
rotation states from each shape's base layout by rotating 90° clockwise
`(c,r) → (−r,c)` and normalising into the non-negative quadrant. `WallPiece` holds
an `anchor` `Coord`; `absoluteCells()` = anchor + current-rotation offsets.
`rotate()` only advances the rotation index — **no placement/overlap validation
lives in the model** (that is the engine's job).

## Cannon rules (data only)

`Cannon` holds `position`, a `reloadRemainingMillis` counter (+`decReload`/setter),
`ammo` (`CANNON_START_AMMO = -1` ⇒ unlimited), and an `alive` flag. `ready()` is
true when alive, off-cooldown, and has ammo. No firing/trajectory/targeting logic —
the engine drives all of it and writes the counters back.

## Ship rules (data only)

`Ship` holds a **sub-cell** floating-point position (`double x,y` in cell units) so
the UI can render smooth motion, `health`, a `heading` (`Direction`), a waypoint
`path` list with a `pathIndex` cursor, and a `fireCooldownMillis` cadence counter
(+`decFireCooldown`). `ShipType` baselines: `SLOOP`(hp 2, 100 pts, 1.0 cell/s),
`FRIGATE`(hp 4, 250 pts, 0.7), `GALLEON`(hp 6, 500 pts, 0.5). Wave size:
`Rules.shipsForRound(round)` = `SHIPS_BASE(3)` + `SHIPS_PER_ROUND(1)` per extra
round. All movement/AI/firing lives in the engine.

## Scoring / territory (thresholds)

- `SCORE_PER_TERRITORY_CELL = 10` per enclosed land cell counted at round end.
- `SCORE_ROUND_SURVIVAL_BONUS = 1000` for surviving a round.
- `TERRITORY_GOOD_FRACTION = 0.40` — informational threshold (fraction of buildable
  land enclosed) for a "good territory" HUD/bonus cue. `GameState.territoryFraction()`
  exposes the current 0..1 reading (engine-computed).

## Levels

`LevelData.LEVELS` holds **3** hardcoded 30×22 ASCII maps (legend: `~`=water,
`.`=land, `#`=wall, `C`=castle):

| # | Name | Castles | Notes |
|---|------|---------|-------|
| 1 | Lonely Keep  | 1 | central island, castle pre-ringed by a starting wall loop |
| 2 | Twin Shores  | 2 | wider island, two open castles to wall in |
| 3 | Three Crowns | 3 | large landmass, three spread-out castles |

`LevelData.newGameState()` decodes a map into a `Grid`, spawns `Castle`s at the `C`
cells, and seeds `phase=TITLE`, `round=1`, `lives=max(1, castleCount)`, and
`cannonsRemainingToPlace = cannonPoolForRound(1)`. Row width and count are validated
at construction; `ModelSelfCheck` asserts every level is 30×22 with ≥1 castle.

## Read-only view seam

Concrete classes implement the read-only `*View` interfaces so the engine can hand
live model objects to the UI as immutable-ish snapshots (UI never casts back to
concrete types): `GameStateView`, `GridView`, `TileView`, `CastleView`,
`CannonView`, `ShipView`, `WallPieceView`. These match the seam described in
`RAMPART_CONTRACT.md` (`GameApi` returns `GameStateView` / `WallPieceView`).
