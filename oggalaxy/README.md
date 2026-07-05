# OG Galaxy — offline single-player Java 8 / Swing recreation

A standalone desktop recreation of the browser strategy game **OG Galaxy** (an OGame-family
space-empire builder). Fully offline and single-player: the MMO / alliance / tournament layer
is replaced by computer-controlled AI empires that play by the same rules on the same map.

Resources → buildings → research → shipyard → fleet → deterministic combat / expeditions →
colonization, versus 1–7 AI opponents at Easy / Medium / Hard / Random difficulty.

Zero external dependencies. Every visual is drawn procedurally with Java2D — no bundled or
scraped art.

## Requirements

- A **JDK 8+** (the code targets Java 8; it compiles and runs on 8, 11, 17, 21). A *full*
  (non-headless) JDK is required to actually launch the Swing UI.

## Build & run (plain JDK — no Maven needed)

```bash
cd oggalaxy
mkdir -p out
javac --release 8 -d out $(find src -name '*.java')     # on JDK 8 use: -source 8 -target 8
java -cp out com.whim.oggalaxy.app.Main                  # launch the game (Start screen)
```

Other entry points:

```bash
java -cp out com.whim.oggalaxy.ui.UiPreview              # UI only, on a canned demo world
java -cp out com.whim.oggalaxy.ui.UiPreview start        # UI, starting from the Start screen
java -cp out com.whim.oggalaxy.engine.EngineSelfCheck    # headless engine test (no UI)
```

## Build & run (Maven)

```bash
cd oggalaxy
mvn -q package
java -jar target/oggalaxy-1.0.0.jar
```

## How to play

1. **Start screen** — enter your commander name, pick a class (Explorer / Miner / General),
   choose how many AI opponents (1–7) and each one's difficulty (Random locks to a concrete
   level at game start, logged in the event feed).
2. **Overview / Buildings / Research / Shipyard** — grow your economy and tech. The clock
   controls (top bar) let you Pause/Resume the background simulation, change speed, or press
   **Advance Turn** to step one in-game hour at a time.
3. **Galaxy** — navigate systems, inspect other empires, debris fields and moons, and open the
   **Fleet Dispatch** dialog to attack, transport, deploy, colonize, expedition, recycle or spy.
4. **Reports** — read deterministic per-round combat reports and expedition outcomes.

## Architecture

| Package | Role |
|---|---|
| `api` | Frozen contract: enums, `Cost`, `GameConfig`, `Formulas`, the full `Catalog` (all building/tech/ship/defense/class data), read-only `Views` snapshots, the `GameController` seam, and a `DemoController`. |
| `model` | Serializable runtime state implementing the `Views` interfaces. |
| `engine` | `GameEngine implements GameController`; background tick loop; command validation. |
| `combat` | Deterministic (RNG-free) battle resolution. |
| `ai` | Per-difficulty AI empire controllers. |
| `expedition` | Weighted expedition outcome resolution. |
| `persistence` | Save/load via Java serialization. |
| `ui` | All Swing screens + procedural Java2D art. |
| `app` | `Main` — wires the engine to the UI. |

The UI never touches simulation classes: it reads `GameController.state()` (a `Views`
snapshot) on a Swing timer and repaints. The engine mutates state on its own thread and
publishes snapshots atomically, so the EDT is never blocked.

See `../OGGALAXY_CONTRACT.md` for the full build contract and combat/expedition/AI specs, and
`src/com/whim/oggalaxy/expedition/RESEARCH.md` for the expedition table.

## Known limitations / approximations

- **OG Galaxy's exact balance numbers are not public.** Costs, production, storage, combat
  stats and rapid-fire follow the well-documented OGame formula family (OG Galaxy is an OGame
  clone). Items specific to OG Galaxy — the **Leviathan** flagship stats and the Explorer /
  Miner / General **class bonuses** — are clearly-labelled reasonable approximations in
  `Catalog.java` / `ClassDef.java`.
- **Combat is deterministic** (the request required no-RNG outcomes). Real OGame combat is
  random per-shot; here rapid fire is modelled as an expected-value multiplier and damage is
  distributed proportionally to hull. Moon creation and expedition outcomes use a *seeded*
  (reproducible) RNG, which does not affect any battle result.
- Interplanetary/anti-ballistic **missiles**, moon **jump-gate** logistics and the **Sensor
  Phalanx** scan UI are modelled at the data/building level but not fully surfaced as
  standalone gameplay screens.
- Time is turn/tick based (1 tick = 1 in-game hour) rather than wall-clock real time, since
  there is no server; the speed control and Advance Turn button stand in for real-time growth.
