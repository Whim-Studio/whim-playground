# The Settlers — clean-room Java 8 / Swing recreation

A from-scratch, single-player-first desktop recreation of **The Settlers** (Blue Byte,
1993; released in North America as *Serf City: Life is Feudal*) — its economy,
road/flag/transport relay, and military/territory systems.

> **Clean-room / no original assets.** All code is original. All graphics are
> placeholder art drawn procedurally with `Graphics2D`; no Blue Byte sprites,
> audio, or text are used or redistributed. See `CREDITS.md` and the art policy
> in `docs/GDD.md`. This project never requires the original game's data files.

## Status

Phases 0–2 complete: fixed-timestep game loop with active `BufferStrategy`
rendering, a pan/zoom camera, eight terrain types, a seeded procedural map
generator, hand-built text-map loading, a click-to-recentre minimap, and the
full building roster with a left-edge build menu, placement/validity rules, a
green/red placement ghost, and the construction lifecycle. See `docs/PROGRESS.md`
for the phase-by-phase log and `docs/GDD.md` for the design spec.

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

More controls arrive with each phase; this table is kept current.
