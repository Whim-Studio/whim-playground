# Tactical Nexus

A deterministic (zero-RNG) grid Puzzle-RPG in the spirit of *Tower of the
Sorcerer* / *Tactical Nexus*, written in Java 8 with Swing/AWT only — no external
libraries, no build system required.

You climb a small tower of floors. Every wall, door, key, gem, enemy and
staircase is fixed: there is no randomness, so a floor is a pure logic puzzle.
Spend keys to open matching doors, collect stat gems to grow strong enough to
beat the enemies guarding the path, and take the stairs upward. The whole run is
fully undoable.

## Build & run

```bash
cd tactical-nexus
javac -d out $(find src -name "*.java")
java -cp out com.whim.tacticalnexus.app.Main
```

This compiles every source file under `src/` into `out/` and launches the game.
Java 8 (or newer) JDK required.

## Controls

| Key                    | Action                          |
| ---------------------- | ------------------------------- |
| Arrow keys             | Move / interact in that direction |
| Ctrl + Z               | Undo the last state change      |
| Ctrl + Y               | Redo                            |
| Ctrl + Shift + Z       | Redo (alternate)                |

Moving into a tile resolves whatever it holds:

- **Wall** (dark-gray square) — blocked.
- **Door** (colored square with a keyhole) — opens in place if you hold a
  matching key, spending one key; you do not advance onto it that move.
- **Key** (colored diamond) — picked up; you step onto the tile.
- **Gem** (colored circle, `+H`/`+A`/`+D`) — adds HP / ATK / DEF.
- **Enemy** (colored square with a name initial and a tiny `HP/ATK/DEF` tag) —
  fought via the engine's deterministic combat. The move is blocked if your ATK
  can't pierce its DEF, or if the fight would be lethal.
- **Stairs** (`<` up / `>` down chevrons) — travel between floors.

## Layout

`GamePanel` (CENTER) draws the current floor with `Graphics2D` primitives;
`StatusPanel` (EAST) shows the HP / ATK / DEF / keys / gold / EXP / level
dashboard. The UI reads game state and calls the engine — it computes no rules
itself.

## Architecture

| Package   | Responsibility                                            |
| --------- | -------------------------------------------------------- |
| `domain`  | Immutable game model (player, grid, entities).           |
| `state`   | `GameState` snapshots + `StateManager` undo/redo stacks. |
| `data`    | `FloorFactory` — the hand-built demo floors.             |
| `engine`  | Pure, Swing-free movement + combat resolution.           |
| `ui`      | Swing presentation: panels, controller, frame.           |
| `app`     | `Main` — EDT bootstrap.                                  |
