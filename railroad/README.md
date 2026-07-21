# Railroad Tycoon — Phase 1 (Java 8 / Swing)

A standalone clone of the 1990 DOS rail-management strategy game, built in pure
**Java 8 + Swing + `Graphics2D`** — **no external libraries, no Maven/Gradle**.
All graphics are drawn procedurally; no original artwork or manual text is used.

This is **Phase 1**: a generated terrain map, terrain-priced track laying between
towns, and a single train that shuttles a connected route to earn distance-based
revenue. Stations, cargo, finance, rivals and other systems are intentionally
out of scope for this phase.

## Requirements
- A **Java 8+** JDK (`javac` / `java`). Source is written to the Java 8 language level.
- A graphical display (it opens a Swing window).

## Build & Run
From this `railroad/` directory:

```bash
# compile
mkdir -p out && find src -name '*.java' > sources.txt && javac -d out @sources.txt

# run
java -cp out com.railroad.Main
```

To enforce the Java 8 language level explicitly (recommended when building with a
newer JDK):

```bash
javac --release 8 -d out @sources.txt
# (or, on JDK 8 itself:  javac -source 8 -target 8 -d out @sources.txt)
```

### Reproducible maps
The world is generated from a fixed seed (default `42`) so a layout can be
reproduced exactly. Pass a different seed on the command line:

```bash
java -Drailroad.seed=1234 -cp out com.railroad.Main
```

## How to play
1. **Build track.** Click the **Build Track** tool, then press-and-drag across
   adjacent tiles from one town to another. Each segment is charged to the
   treasury by terrain: clear ground is cheap, hills more, mountains expensive,
   and water is a costly bridge (see the on-map legend for exact prices). Track
   is blocked if you cannot afford the next segment.
2. **Buy a train.** Once two towns are joined by track, click **Buy Train**. One
   train is placed on the first connected town pair and starts shuttling back
   and forth. (Phase 1 supports a single train on a single route.)
3. **Run the clock.** Use **Start / Pause** to run or halt the game clock and
   **Speed** to cycle 1x → 2x → 4x. Each completed one-way trip pays
   distance-based revenue into the treasury.
4. **Watch the HUD.** The status bar shows the treasury, in-game date, selected
   tool and completed-trip count (with the last trip's revenue).

The map scrolls: use the scrollbars or drag inside its scroll pane to pan.

## Architecture
Packages map to the three design responsibilities, mirroring the repo's other
Java apps:

| Package | Responsibility |
|---|---|
| `com.railroad.model` | Domain state (single source of truth): `World`/`TileGrid`/`Tile`/`TerrainType`, `Town`, `TrackNetwork`/`TrackSegment`, `Route`, `Train`, `Company`, `GameDate`, and the central `GameState`. |
| `com.railroad.logic` | `MapGenerator` (deterministic, seeded procedural terrain + town placement) and `GameClock` (Swing-timer game loop that calls `GameState.tick`). |
| `com.railroad.ui` | Swing presentation: `MapPanel` (procedural `Graphics2D` map, legend, track-laying input), `HudPanel` (toolbar + status bar), `GameController` (view↔model mediator), `GameFrame`. |

`com.railroad.Main` launches the window on the EDT.

### Key interfaces (designed to be extended by later phases)
- `model.World` / `model.TileGrid` — tile access by `(x, y)`, `TerrainType` enum, town list.
- `model.Town` — id, name, tile position (room reserved for cargo supply/demand).
- `model.TrackNetwork` — undirected tile graph of `TrackSegment`s; `isConnected`, `areTownsConnected`, and BFS `findPath`.
- `model.Train` — position along a `Route`, speed, capacity (present but unused in Phase 1).
- `model.Company` — cash treasury with `canAfford` / `spend` / `earn` hooks (loans/bonds/stock deliberately not implemented).
- `model.GameState` / `logic.GameClock` — central state and `tick(dt)` advancing trains and the date.

## Verified
- Compiles cleanly at the Java 8 language level (`javac --release 8`) — only the
  standard Swing `serialVersionUID` lint warnings.
- A headless smoke test exercised: deterministic terrain from a seed, town
  placement/spacing, terrain-priced track laying and insufficient-funds refusal,
  network connectivity + BFS routing, buying the train, and multi-trip revenue
  accrual over many clock ticks — no runtime errors.
- The Swing window itself was not launched here (headless container); run the
  command above on a machine with a display.
