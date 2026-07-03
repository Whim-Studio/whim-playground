# Powermonger — Build Contract

A standalone, **Java 8**, zero-dependency, procedural-graphics Swing adaptation of
the 1990 Bullfrog real-time strategy classic **Powermonger**. Three parallel tasks
build against the shared `api` package. This file is the single source of truth for
the seams between them.

## Hard constraints (ALL tasks)

- **Java 8 ONLY.** No `var`, no switch expressions, no text blocks, no records, no
  `List.of`/`Map.of`, no `Stream.toList()`, no local type inference. Standard
  `switch` statements and Java 8 Streams/lambdas are fine.
  `maven.compiler.source/target = 1.8`.
- **Zero external libraries.** Only `javax.swing`, `java.awt`, `java.util`,
  `java.io`, `java.lang`. JUnit allowed for tests only.
- **No downloaded assets.** Every visual is drawn with `java.awt.Graphics2D`
  (shapes, gradients, strokes, polygons). No image files, no icons on disk.
- **Package root:** `com.whim.powermonger`
- **Source root:** `powermonger/src/`
- Every task's code **must compile** and import ONLY from `api` (and its own
  sub-package). No task edits another task's sub-package.
- When done: **push your branch and send_prompt a short report back to the
  orchestrator task** (this task). Do not open a PR into main yourself.

## Package ownership

| Package | Owner | Contents |
|---|---|---|
| `com.whim.powermonger.api` | **Orchestrator (DONE — do not edit)** | `Enums`, `Views`, `GameController`, `ActionResult` |
| `com.whim.powermonger.domain` | **Task 1** | `MapGrid`, `Tile`, `Town`, `Townsperson`, `Captain`, `ArmyBloc`, `Pigeon`, `Order`, `CommandQueue`, `WorldState`, `WorldGenerator`, `BalanceOfPower` |
| `com.whim.powermonger.engine` | **Task 2** | `GameEngine` (implements `api.GameController`), `SimulationLoop`, `LifeAI` (townsperson autonomy), `CommandLag`, `PostureMath`, `CombatResolver`, `WeatherSystem`, `VictoryMonitor` |
| `com.whim.powermonger.ui` | **Task 3** | `GameFrame`, `MapPanel`, `MiniMapPanel`, `ConsolePanel`, `BalancePanel`, `IsoRenderer`, `SpriteFactory`, `UiPalette`, `StubController` (dev only) |
| `com.whim.powermonger.app` | **Orchestrator (final)** | `Main` — wires engine + ui |

## The `api` package (already committed — read it, DO NOT modify)

- **`Enums`**
  - `Posture` — `PASSIVE(1,0.25)`, `NEUTRAL(2,0.50)`, `AGGRESSIVE(3,1.00)`; has
    `swords()`, `scale()`, `cycleUp()`, `cycleDown()`.
  - `CommandType` — `SCOUT, FIGHT, GATHER_FOOD, SUPPLY_FOOD, RECRUIT, DISBAND,
    INVENT, TRADE, MOVE`; each has `label()`.
  - `TerrainType` — `DEEP_WATER, SHALLOW_WATER, BEACH, GRASS, FOREST, HILL,
    MOUNTAIN, TOWN`.
  - `Season` — `SPRING, SUMMER, AUTUMN, WINTER`.
  - `Weather` — `CLEAR, RAIN, SNOW`.
  - `Allegiance` — `PLAYER, ENEMY, NEUTRAL`.
  - `Job` — `FARMING, FISHING, HERDING, CRAFTING, IDLE`.
- **`Views`** — read-only interfaces: `TileView`, `TownView`, `TownspersonView`,
  `CaptainView`, `PigeonView`, `GameStateView`. Coordinates are in TILE units;
  blocs/people use fractional tile coordinates. **UI reads ONLY these.**
- **`GameController`** — the ONE seam between UI and engine:
  `state()`, `issueOrder(captainId,type,tx,ty)`, `setDestination(captainId,tx,ty)`,
  `setPosture(captainId,posture)`, `selectCaptain(id)`, `selectedCaptainId()`,
  `newGame(seed)`, `start()`, `stop()`.
- **`ActionResult`** — `ok(msg)` / `fail(msg)`, `isSuccess()`, `message()`.

**Data flow:** UI renders `controller.state()` (a `GameStateView`) on a Swing
timer. User input calls a `GameController` action. The engine mutates domain state
on its background thread; the UI re-reads `state()` and repaints. UI never casts a
`*View` to a concrete domain class.

## Ruleset the domain + engine must encode

- **World map:** a `MapGrid` (target **48×48** tiles) of `Tile`s, each with a
  `TerrainType`, integer `elevation` (0..maxElevation, target 6), tree flag, food
  potential, and snow flag. Towns sit on `TOWN` tiles. Deforesting a `FOREST` tile
  (via combat/gathering) turns it to `GRASS`, removes trees, and locally nudges
  weather toward drier/harsher (documented, simple effect) and can raise movement
  cost of surrounding tiles slightly.
- **Seasons & weather:** cycle `SPRING→SUMMER→AUTUMN→WINTER`. Winter/late-autumn
  raise snow probability; snow and rain reduce the global movement factor
  (`GameStateView.movementFactor()`), snow paints tiles white.
- **Artificial life:** neutral `Townsperson` agents autonomously path to nearby
  resource nodes and perform `Job`s (farming on GRASS, fishing at water edges,
  herding, crafting in town) with **no player input**. They wander/return on a loop.
- **Player & captains:** the player is a warlord commanding up to **4 Captains**,
  each leading an `ArmyBloc`. Captain 0 is the **supreme commander**.
- **Bloc control & postures:** the player issues indirect `CommandType` orders to a
  bloc; `Posture` scales execution — e.g. `RECRUIT` at `AGGRESSIVE` recruits ~100%
  of a town's eligible population, at `PASSIVE` ~25%; `GATHER_FOOD` loots
  proportionally. Postures scale combat aggression and gather/recruit magnitude.
- **Command lag (carrier pigeons):** orders to a **subordinate** captain are not
  instantaneous. A `Pigeon` flies from the supreme commander to the target; flight
  time is **proportional to 2D Euclidean distance** between them. The order applies
  only on arrival. Orders to the supreme commander (or the selected captain if it is
  the commander) apply immediately.
- **Combat:** when opposing blocs collide, resolve real-time skirmish math using
  strength, posture, terrain elevation advantage, and food/morale. Losers shed
  strength; a captain at 0 strength is eliminated.
- **Victory:** the **Balance of Power** in [-1,+1] shifts toward the player by
  capturing towns and eliminating enemy captains, and toward the enemy for the
  reverse. `+1` (or all enemy captains dead) = player victory; `-1` = defeat.

## Seam details each task MUST honour

- **Task 1 (domain)** owns the concrete state. Domain classes **implement the
  `Views` interfaces** (e.g. `Tile implements Views.TileView`, `Captain implements
  Views.CaptainView`). Provide a `WorldState` aggregate that can produce a
  `GameStateView` snapshot (either by implementing it or via a small adapter).
  Provide `WorldGenerator.generate(long seed)` returning a populated `WorldState`
  with terrain, elevation, forests, towns, townspeople, 4 player captains and 2–3
  enemy captains. Include a `DomainSelfCheck` `main` that builds a world and prints
  a summary so the package is runnable standalone.
- **Task 2 (engine)** implements `api.GameController` in `GameEngine`. It drives a
  fixed-timestep `SimulationLoop` on a **background daemon thread** (~20 ticks/s),
  advancing life AI, movement (scaled by `movementFactor`), pigeons, combat,
  weather/seasons and the balance of power. `state()` must return a **snapshot**
  that the EDT can read without tearing (copy or synchronize). It depends ONLY on
  `api` + `domain`. Include an `EngineSelfCheck` `main` that runs a headless world
  for N ticks and prints balance-of-power progression.
- **Task 3 (ui)** builds Swing on top of `api.GameController` ONLY. Layout: a
  clickable **minimap (left)**, a large **2.5D pseudo-isometric MapPanel (center)**
  rendered with `Graphics2D` (elevation lift + drop-shadows, procedural sprites for
  units/trees/flocks of birds), a **Balance-of-Power scale (bottom)**, and a
  **command console** with the 8 `CommandType` icon buttons + a 3-sword posture
  toggle. `MouseListener` on the map sets a destination line for the selected
  captain (calls `controller.setDestination`). To develop independently, ship a tiny
  `StubController implements GameController` returning a static hand-made snapshot;
  the orchestrator swaps in the real engine via `Main`. Include a `main`
  (`UiPreview`) that launches the frame against the stub.

## Build / run

Standalone Maven module (no root pom involvement):

```
mvn -f powermonger/pom.xml -q compile      # compiles all packages
```

Final entry point (orchestrator writes it): `com.whim.powermonger.app.Main`.
