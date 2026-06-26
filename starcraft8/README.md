# 8-Bit StarCraft

A standalone, retro **8-bit RTS demake** inspired by the *8-Bit StarCraft* concept
(Owen Dennis, 2010). Three fully playable races (Terran, Zerg, Protoss), a worker
economy (Minerals / Vespene Gas / Supply), base building, a tech tree, real-time
grid combat, and a state-machine AI opponent — all in pure **Java 8 + Swing** with
**zero external libraries** and **no external art** (every sprite is drawn
algorithmically from hardcoded pixel bitmaps).

> All unit/building stats are **invented and emulated** for a fast arcade feel — this
> is a simplified demake, not a reproduction of Blizzard's balance numbers.

## Build & run

```bash
javac -d out $(find starcraft8/src -name '*.java')
java -cp out com.whim.starcraft8.app.Main
```

A small dialog lets you pick your race and the AI's race, then the match starts.

### Headless engine smoke test (no display required)

```bash
java -Djava.awt.headless=true -cp out com.whim.starcraft8.engine.EngineSmokeTest
```

Runs an AI-vs-AI match to completion and prints economy/army snapshots and the winner.

## Controls

- **Left-drag** — box-select your units. **Left-click** — select a unit/building.
- **Right-click** — contextual order: move, attack the enemy under the cursor, or
  send a worker to gather a resource field. On a selected building, sets its rally point.
- **B** build menu (worker selected) · **A** attack-move · **S** stop · **Esc** cancel.
- Arrow keys pan the camera. The bottom console shows Minerals / Gas / Supply, a
  minimap, the current selection, and contextual Build/Train action buttons.

## Architecture (built by three parallel agents against `STARCRAFT8_CONTRACT.md`)

- `com.whim.starcraft8.domain` + `.data` — entities, the unit/building stat
  dictionary, tech tree, and `MapFactory` skirmish setup.
- `com.whim.starcraft8.engine` — the 60-tps simulation on a background thread,
  command queue, economy, combat, pathfinding, win/lose, and the AI. Fully
  decoupled from the UI.
- `com.whim.starcraft8.ui` — Swing `Graphics2D` rendering (chunky pixel sprites,
  HUD, minimap) and input. Reads engine state only under the engine lock via
  `Simulation.readState`, and mutates nothing directly — all input flows through
  `Commands` + `Simulation.enqueue`.
- `com.whim.starcraft8.app.Main` — entry point wiring the three layers together.
