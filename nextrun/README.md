# Next Run

A turn-based fantasy roguelike RPG in the spirit of Clarus Victoria's *Next Run*,
written in **Java 8** with **Swing/AWT only** — no external libraries, no build
system required. Time is the real enemy: every action burns turns, and waves of
hellish enemies grow exponentially stronger the longer a run lasts.

## Build & run

```bash
cd nextrun
javac -d out $(find src -name "*.java")
java -cp out com.whim.nextrun.app.Main
```

Java 8 (or newer) JDK required. The sources compile cleanly under
`javac -source 8 -target 8`.

## How it plays

You pick one of **8 hero classes**, each with distinct stats and a passive that
bends the rules:

| Class | Passive |
|-------|---------|
| Warrior | +2 damage in combat |
| Mage | sneaking past enemies is far easier |
| Lord | starts wealthy; bribes cost less |
| Artisan | crafting & building cost fewer turns |
| Leader | structures finish faster and grant more |
| Necromancer | enemies weakened; combat hits harder |
| Peasant | gathers extra materials |
| Druid | moving & gathering cost fewer turns |

### The doom loop

A counter ("Turns until next wave") ticks down with **every action**. When it
hits zero, a **wave** of stronger fiends erupts across the map (Imps → Ashen
Knights → Archdemons), the tier climbs, and the interval shortens. Survive — or
win first.

### Action economy

Every action costs **turns**, and the cost scales with your stats: higher
dexterity speeds gathering/crafting/building, higher magic speeds sneaking, and
class passives shave further. Costs never drop below 1 — time is always spent.

### Paths of resolution

Every enemy can be dealt with three ways:

- **Fight** — `attack` vs `defense` damage math; spoils on a kill.
- **Bribe** — pay gold for safe passage (Lords pay less).
- **Sneak** — `dexterity + magic` vs the enemy's sneak DC (Mages excel).

### Four ways to win

- **Economy** — amass 160 gold.
- **Crafting** — forge gear to +6 weapon **and** +6 armor.
- **Settlement** — build 4 structures.
- **Escape** — reach the portal (`◉`) alive.

Lose if your HP hits zero.

## Controls

| Key | Action |
|-----|--------|
| Arrow keys / WASD | Move (into an enemy = blocked; resolve it instead) |
| G / L / E | Gather / Loot / Explore the current tile |
| F / B / Shift+S | Fight / Bribe / Sneak an adjacent enemy |
| 1 / 2 | Forge Weapon / Forge Armor |
| C | Build a structure on open ground |
| R | Rest (heal) |

All actions are also available as context-sensitive buttons on the bottom bar.

## Architecture

Clean separation of concerns — the UI never does game math:

```
com.whim.nextrun
├── domain/   HeroClass, Player, GridMap, Tile, Enemy, Position, EntityType
├── engine/   GameState (authority), ActionCosts, Resolution, MapGenerator, WaveSpawner
├── ui/       MapPanel (Graphics2D grid), GameFrame (dashboard + actions + log)
└── app/      Main (class picker + launch)
```

The world is rendered with pure `Graphics2D` primitives and Unicode glyphs —
no image assets are loaded or downloaded.
