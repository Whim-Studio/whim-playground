# Ruinlander — Wasteland Survival RPG

A turn-based post-apocalyptic survival RPG. Java 8 + Swing/AWT only — **no
external libraries, no Maven/Gradle, no downloaded assets** (all visuals are
drawn with `Graphics2D` primitives and Unicode glyphs).

Built as a three-layer parallel project against `RUINLANDER_CONTRACT.md`:

- `domain/` — game model + state machine (Player, GridMap, entities,
  `GameStateManager`, `WorldFactory`).
- `engine/` — survival math, turn-based combat, encounters, loot, crafting.
  The only source of RNG (a single seeded `java.util.Random`).
- `ui/` + `app/` — Swing dashboard: map canvas, survival meters, action log,
  inventory + crafting; `KeyListener`/`MouseListener` input.

## Build & run

```bash
# from repo root
find ruinlander/src -name '*.java' > /tmp/ruinlander.sources
javac --release 8 -d ruinlander/out @/tmp/ruinlander.sources
java -cp ruinlander/out com.whim.ruinlander.app.Main
```

## Controls

| Mode | Keys |
|------|------|
| Explore | `WASD` / arrows = move · `I` = inventory · `C` = crafting |
| Combat | `1-9` or click = target · `SPACE`/`F` = attack · move keys = reposition · `E` = end turn |
| Inventory | `1-9` = use/equip · `I`/`Esc` = back |
| Crafting | `1-9` = craft · `C`/`Esc` = back |

## Survival meters

Six meters drive the game: **Health, Hunger, Thirst, Fatigue, Radiation,
Warmth**. Every overworld step advances hunger/thirst/fatigue, drifts warmth
toward the terrain's ambient, and accrues radiation in TOXIC zones. When a meter
hits its critical band it bleeds Health. Eat, drink, rest at settlements, and
use Rad-Away to stay alive.
