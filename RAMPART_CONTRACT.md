# Rampart (1990, Atari Games) — Build Contract

A standalone, **Java 8**, zero-dependency, procedural-graphics Swing recreation of
the 1990 Atari Games arcade classic **Rampart**. Three tasks build against the
seams defined here. This file is the single source of truth for the boundaries
between the model, engine, and UI layers.

## Hard constraints (ALL tasks)

- **Java 8 ONLY.** No `var`, no switch expressions, no text blocks, no records,
  no `List.of`/`Map.of`, no `Stream.toList()`. Traditional `switch` statements,
  standard for-loops, anonymous classes, and Java 8 lambdas/streams are fine.
  `maven.compiler.source/target = 1.8`.
- **Zero external libraries.** Only `javax.swing`, `java.awt`, `java.awt.event`,
  `java.util`, `java.io`, `java.lang`. No Maven/Gradle plugins beyond the jar
  plugin already in `rampart/pom.xml`. No downloaded assets — every visual is
  drawn with `Graphics2D` primitives (rectangles, polygons, ovals) color-coded by
  tile/unit type. Optional graceful load of local `resources/*.png` with fallback
  to drawn placeholders (never assume network access).
- **Package root:** `com.rampart` — **Source root:** `rampart/src/`
- **Strict dependency direction:** `ui -> engine -> model`. NEVER reversed.
  `model` imports nothing from `engine`/`ui`. `engine` imports nothing from `ui`
  and references ZERO Swing/AWT classes. `ui` never re-implements engine/model
  logic (no territory math, no collision math in the UI).
- Every task's code **must compile** with `javac --release 8`. No task edits
  another task's package.
- When done: **push your branch and `send_prompt` a short report back to the
  orchestrator task** (do NOT open a PR into main yourself).

## Package ownership

| Package | Owner | Contents |
|---|---|---|
| `com.rampart.model`  | **Task 1** | Grid/Tile, Coord, enums, Castle, WallPiece (polyominoes), Cannon, Ship, GameState, LevelData + `LEVELS`, read-only `*View` interfaces, `RESEARCH.md`, `ModelSelfCheck` |
| `com.rampart.engine` | **Task 2** | `GameApi` impl (`GameEngine`), `PhaseController`, `TerritoryCalculator`, `CannonSystem`, `WallBuildSystem`, `ShipAI`, `ScoreSystem`, `EngineSelfCheck` |
| `com.rampart.ui`     | **Task 3** | `GameFrame`, `GamePanel`, `Renderer`, `Hud`, `InputHandler`, `Palette`, `StubGameApi` (dev only), `UiPreview`, CardLayout screens (title/round/gameover) |
| `com.rampart.engine.GameApi` | **Orchestrator (committed before wave 2)** | the single UI↔engine seam interface |
| `com.rampart.app`    | **Orchestrator (final)** | `Main` — wires `GameEngine` + `GameFrame` |

## Researched Rampart mechanics (authoritative — do NOT re-guess these)

Rampart is a single-screen game played over a repeating **three-phase loop**:

1. **BUILD / CANNON-PLACEMENT phase (timed, ~short):** the player places cannons
   inside *enclosed* castle territory. Cannons may only sit on land that is fully
   surrounded by an unbroken wall loop around a castle. A fixed pool of cannons is
   available per round; each placed cannon occupies grid cells.
2. **BATTLE phase (timed):** enemy ships sail in the surrounding water and bombard
   the walls, blasting wall segments into **rubble** and leaving gaps. The player
   fires cannons by clicking a target cell; each shot has a **reload/cooldown** and
   lobs a ball that impacts an area, destroying walls/ships it hits. Ships that are
   hit enough are destroyed. Land units may also come ashore in later rounds.
3. **REPAIR phase (timed, ~short):** the player is dealt a sequence of **Tetris-like
   polyomino wall pieces** (I, L, S, Z, T, square, single, etc.) one at a time,
   which are rotated and dropped onto the grid to re-seal the walls blown open
   during battle and to expand territory. Pieces cannot overlap water, existing
   walls, cannons, or castles.

**Round survival rule (critical):** at the end of the REPAIR phase the player must
have **at least one castle completely enclosed** by an unbroken wall loop. If no
castle is enclosed, the game is over. Enclosed territory (the flood-filled land
area inside a sealed wall loop) determines how many cannons can be placed and drives
scoring; larger enclosed territory = better. Surviving advances to the next round
with tougher/faster ships. (These are the widely-documented arcade rules; Task 1
must capture the exact numeric thresholds it chooses in `RESEARCH.md` and expose
them as constants so the engine and tests use one source of truth.)

**Grid:** a rectangular tile grid. Tile kinds: `WATER`, `LAND`, `WALL`, `RUBBLE`,
`CANNON`, `CASTLE`. Coordinates are `(col,row)`; keep a single `Coord` type.

## The seam: `com.rampart.engine.GameApi`

The **only** contact surface between UI and engine. UI holds a `GameApi`, polls a
read-only snapshot each Swing-timer tick, repaints, and forwards input as method
calls. UI NEVER casts a snapshot to a concrete engine/model class. Committed by the
orchestrator before wave 2 so Task 2 implements it and Task 3 stubs it. Shape:

```java
public interface GameApi {
    void newGame();                       // reset to round 1, title/first level
    void startRound();                    // begin the current round at BUILD phase
    void tick(long dtMillis);             // advance timers/battle sim by dt
    GameStateView state();                // immutable-ish read-only snapshot for the UI

    // BUILD/CANNON-PLACEMENT phase input
    boolean placeCannon(int col, int row);

    // BATTLE phase input
    boolean fireCannonAt(int col, int row);

    // REPAIR phase input (current dealt piece)
    void rotatePiece();                                  // rotate current wall piece CW
    boolean placePieceAt(int col, int row);              // anchor current piece at cell
    WallPieceView currentPiece();                        // piece being placed (or null)

    // phase control / lifecycle
    void endPhaseEarly();                 // player signals ready before timer expires
    boolean isGameOver();
}
```

`GameStateView`, `WallPieceView`, and the other `*View` interfaces live in
`com.rampart.model` (Task 1) and are implemented by the concrete model classes so
the engine can return live model objects directly as read-only views.

## Interface expectations between tasks

- **Task 1 (model)** delivers pure data + enums + validation getters/setters, the
  read-only `*View` interfaces (`GameStateView`, `GridView`, `TileView`,
  `CastleView`, `CannonView`, `ShipView`, `WallPieceView`), a `LevelData` set with
  **at least 3** hardcoded level layouts in `LevelData.LEVELS`, and `RESEARCH.md`
  documenting the rules + chosen constants. **No logic** (no AI, no territory
  flood-fill, no rendering, no input). Include a `main` in `ModelSelfCheck` that
  instantiates the models and prints a sanity summary.
- **Task 2 (engine)** owns ALL mutation, timers, AI, territory flood-fill, cannon
  trajectory/collision, wall-placement validation, scoring, and round progression.
  `GameEngine implements com.rampart.engine.GameApi`. Every public method's Javadoc
  states which Task 1 model types it consumes/returns. Zero Swing/AWT imports.
  Include `EngineSelfCheck` with a `main` that plays a scripted round headless.
- **Task 3 (ui)** builds the Swing front-end against `GameApi` + the model `*View`
  interfaces only. It ships a `StubGameApi implements GameApi` returning a
  hand-built snapshot so the UI runs before Task 2 lands. `Main` is wired by the
  orchestrator, not Task 3; Task 3 may keep a `UiPreview` with its own `main` that
  launches the frame against `StubGameApi`. Title / round-transition / game-over
  screens are separate JPanels swapped via `CardLayout`. The render/update loop is a
  `javax.swing.Timer` that calls `engine.tick(...)` then repaints — no game logic in
  UI classes.

## Build & run (orchestrator, final)

```
cd rampart
javac --release 8 -d out $(find src -name '*.java')
java -cp out com.rampart.app.Main
```
(Maven `mvn -o package` also works via the jar plugin; main class is
`com.rampart.app.Main`.)
