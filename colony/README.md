# Colony Sim

A standalone Java 8 Swing colony-management simulation (RimWorld-like). Plain
`javac` build, zero external dependencies (only `java.util`, `java.awt`,
`javax.swing`).

Built in three tasks: **(1) domain & API** (this PR), **(2) engine**,
**(3) UI/app**. Tasks 2 and 3 import everything defined here; the `api`
interfaces are a hard contract.

## Build

```sh
find colony/src -name '*.java' > /tmp/srcs.txt
javac -source 1.8 -target 1.8 -d /tmp/colony-out @/tmp/srcs.txt
```

## Package layout

```
com.whim.colony                 top-level shared state
  ColonyState                   single shared state aggregate (read by UI, mutated by engine)

com.whim.colony.domain          pure data classes (no logic)
  TerrainType (enum)            GRASS/DIRT/ROCK/WATER/WALL + walkable flag
  BuildingType (enum)           STOCKPILE/BED/FARM/WALL + blocksMovement flag
  Building                      a structure placed on a tile
  MapTile                       one grid cell: x,y + terrain + nullable building; isWalkable()
  ColonyMap                     the MapTile[][] grid: getWidth/getHeight, getTile, inBounds
  Needs                         Hunger/Rest/Mood doubles in [0..100], clamped on set
  SkillType (enum)              FARMING/CONSTRUCTION/MINING/COOKING/MEDICINE/COMBAT
  SkillSet                      SkillType -> level (int), get/set
  Colonist                      pawn: id, name, x, y, Needs, SkillSet, currentJob, path (data only)
  Resources                     Food/Steel/Wood ints with add/consume guards
  IncidentType (enum)           COLD_SNAP/HEAT_WAVE/RAID/RESOURCE_DROP/DISEASE/WANDERER_JOINS
  Incident                      storyteller event data: type, tick, description, severity

com.whim.colony.api             the CONTRACT — Tasks 2 & 3 depend on these
  Job                           assignable work
  Action                        a unit engine step
  Event                         a storyteller incident that mutates state
```

## API contract (`com.whim.colony.api`)

### `Job` — a unit of assignable work (engine-agnostic)
- `String getName()` — short human-readable label.
- `boolean isComplete(ColonyState state, Colonist c)` — true when no work remains.
- `int getTargetX()` — X of the tile the job takes place at.
- `int getTargetY()` — Y of the tile the job takes place at.

### `Action` — a single executable step towards a Job
- `void perform(ColonyState state, Colonist c)` — execute one step, mutating state/colonist.
- `boolean isFinished()` — true once the action is done and should not repeat.

### `Event` — a storyteller incident that mutates state
- `void apply(ColonyState state)` — apply the event's effects to the colony.
- `String describe()` — one-line description for the message log / UI.

## Notes for Tasks 2 & 3
- `ColonyState` is the single shared object: the engine mutates it, the UI reads it.
  It holds `ColonyMap map`, `List<Colonist> colonists`, `Resources resources`, a
  bounded `List<String>` message log (`addMessage`, cap `MAX_MESSAGES = 200`), a
  `long tick`, and a `boolean paused` flag. No simulation logic lives here.
- `Colonist` is data-only. Its `List<int[]> path` is populated by the engine's
  pathfinder; `currentJob` is a nullable `Job`.
- `ColonyMap.getTile(x, y)` returns `null` when out of bounds — guard with
  `inBounds(x, y)`.
