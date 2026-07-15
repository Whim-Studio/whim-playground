# The Settlers — clean-room Java 8 / Swing recreation

A from-scratch, single-player-first desktop recreation of **The Settlers** (Blue Byte,
1993; released in North America as *Serf City: Life is Feudal*) — its economy,
road/flag/transport relay, and military/territory systems.

> **Clean-room / no original assets.** All code is original. All graphics are
> placeholder art drawn procedurally with `Graphics2D`; no Blue Byte sprites,
> audio, or text are used or redistributed. See `CREDITS.md` and the art policy
> in `docs/GDD.md`. This project never requires the original game's data files.

## Status

Phases 0–5 complete: fixed-timestep game loop with active `BufferStrategy`
rendering, a pan/zoom camera, terrain + seeded map generator + minimap, the full
building roster with placement/validity and construction, the **economy
simulation** (settler roles, production chains, warehouse/inventory, renewable
wood, tool-gated staffing, distribution/tool-priority UI — press **E**), the
**flag-relay transport** (place flags **F**, lay roads **R**; goods move only
along roads via per-segment carriers with real congestion), and **military &
territory** — knights with five ranks and morale, Guard Hut→Tower→Garrison,
territory borders, and the click-an-enemy-fort attack/defend flow, plus an **AI
opponent** that builds its own economy over roads, expands its territory, and
attacks/defends under the same rules as the human. See `docs/PROGRESS.md` for the
phase log and `docs/GDD.md` for the design spec.

## Requirements

- **JDK 8**, language level 8 only — no `var`, records, switch expressions, lambdas-
  in-interfaces beyond Java 8, or modules. (Developed compiling with `--release 8`.)
- Swing + Java2D only. No third-party runtime dependencies.

## Build & run

Plain JDK (no Maven needed):

```bash
cd settlers
mkdir -p out
find src -name '*.java' > sources.txt
javac --release 8 -d out @sources.txt

# Desktop UI (needs a display):
java -cp out com.whim.settlers.app.Main                     # generated map, default seed
java -cp out com.whim.settlers.app.Main --seed 2026         # generated map, chosen seed
java -cp out com.whim.settlers.app.Main --map maps/tutorial-valley.map  # hand-built map

# On a headless machine, Main runs an engine self-test instead of opening a window:
java -Djava.awt.headless=true -cp out com.whim.settlers.app.Main
```

## Controls (Phase 0)

| Input | Action |
|-------|--------|
| `W` `A` `S` `D` / arrow keys | Pan the camera |
| Mouse wheel | Zoom to cursor |
| Right-drag (or middle-drag) | Pan the camera |
| Left-click minimap | Recentre the camera there |
| Left-click build menu (left edge) | Arm a building for placement |
| Left-click map | Place the armed building (green ghost = valid) |
| Right-click | Cancel placement mode |
| `E` | Toggle the economy panel (stockpile, tool & supply priority) |
| `F` | Flag tool — click land to place a flag |
| `R` | Road tool — click a first flag, then a second, to lay a road |
| `Esc` | Cancel the current tool / placement |
| Left-click enemy fort | Open the attack panel (choose knights, Attack) |

More controls arrive with each phase; this table is kept current.
