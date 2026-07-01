# War Room: Tactical Sandbox — Interface Contract

A standalone **Java 8 + Swing** real-time tactical sandbox. A top-down 2D
battlefield where the user (the commander) designs a scenario in **Simulation
Mode** — generate terrain, place cross-era units, drop markers, and draw
synchronized movement routes with arrival times — then presses **Play** to hand
control to a real-time **Battle Mode** engine that moves units along their
routes, fires synchronized detonations, resolves combat, and drives morale,
panic and rout via tactical stances. No external libraries; every visual is
drawn procedurally with `Graphics2D` (no image files are ever loaded).

This file is the **binding contract** between the three parallel child tasks.
**Do not change a public signature defined here without reporting back to the
orchestrator first.** Cross-package code is mergeable ONLY because every
signature below is fixed. Write compile-ready code against these signatures; the
orchestrator compiles the full tree at integration.

## Hard constraints (ALL tasks)

- **Java 8 only.** No `var`, no switch *expressions* (classic `switch` statements
  are fine), no text blocks, no records, no `List.of`/`Map.of`, no
  `Stream.toList`, no Java 9+ APIs. Compile target 1.8.
- **Only** `javax.swing`, `java.awt`, `java.util` (incl. `java.util.Random`,
  `concurrent`, `atomic`), `java.lang`. No I/O of assets, no Maven/Gradle.
- Source root: `warroom/src`. Package root: `com.whim.warroom`.
- Plain `javac`/`java` (see Build). 
- **Determinism:** all randomness comes from `SandboxState.rng` (one shared
  `Random`, seeded). No `Math.random()`, no `new Random()` elsewhere. Given the
  same scenario + seed, a run is byte-for-byte reproducible — this is what makes
  rewind/seek correct.
- **Threading:** the engine simulation loop runs on a **background thread** owned
  by Task 2. It never touches Swing. It publishes immutable `SimSnapshot`
  frames. Task 3's UI reads snapshots and marshals all repaint onto the EDT via
  `SwingUtilities.invokeLater`. The UI never computes a game rule; it reads
  domain/snapshot state and calls `SimEngine`.
- Engine/domain never import `javax.swing` or `java.awt.*` **except**
  `java.awt.Color` (colors live on domain enums and are fine to read).

## Coordinate & time model (FIXED — all tasks rely on this)

- **World space** is `double` units. The map is `cols × rows` tiles, each
  `MapState.TILE_SIZE = 32.0` world units square. World size =
  `cols*32 × rows*32`. Task 3 applies pan/zoom to map world→screen.
- **Time** is integer **ticks**. `SimEngine.TICKS_PER_SECOND = 60`. A `Waypoint`
  arrival time is an absolute tick from scenario start (tick 0). Speed is a
  playback multiplier over wall-clock only; it never changes tick math.

## Package / file ownership (NO two tasks edit the same file)

```
com.whim.warroom
├── domain/   [TASK 1] core model + interfaces (no engine/ui imports)
│   ├── Era.java          enum ANTIQUITY, MEDIEVAL, MODERN
│   ├── Faction.java      enum BLUE, RED (+ NEUTRAL) with Color
│   ├── Stance.java       enum OFFENSIVE, DEFENSIVE, RETREAT
│   ├── Biome.java        enum GRASSLAND, FOREST, HILLS, DESERT, SNOW, URBAN, WATER
│   ├── Vec2.java         immutable 2D double vector
│   ├── TerrainTile.java  one grid cell: biome + elevation
│   ├── MapState.java     terrain grid + procedural generator (RESEARCH-free)
│   ├── UnitType.java     immutable archetype/template (era + stats)
│   ├── UnitCatalog.java  static catalog of archetypes across eras [RESEARCH]
│   ├── Unit.java         live mutable unit instance
│   ├── Waypoint.java     one route point: pos + arrivalTick + optional detonation
│   ├── Route.java        ordered waypoints for a unit
│   ├── MapMarker.java    a dropped commander marker
│   ├── SandboxState.java whole scenario: map, units, markers, shared rng, tick
│   ├── SimSnapshot.java  immutable render frame (UnitView/BlastView) for a tick
│   ├── SimEngine.java    INTERFACE implemented by Task 2
│   └── SimListener.java  INTERFACE the UI implements for frame callbacks
├── engine/   [TASK 2] pure logic, implements SimEngine. No Swing/AWT (Color ok)
│   ├── SimEngineImpl.java  implements SimEngine; owns the background tick thread
│   ├── MovementSystem.java route-following 2D vector movement + terrain cost
│   ├── CombatResolver.java deterministic damage/engagement math
│   ├── MoraleSystem.java   morale decay, panic triggers, rout
│   ├── AiController.java    per-stance behavior (Offensive/Defensive/Retreat)
│   └── EngineSmokeTest.java dependency-free main() asserting the math
├── ui/       [TASK 3] Swing only; reads state/snapshots, calls engine
│   ├── WarRoomFrame.java    JFrame + BorderLayout + mode switching
│   ├── BattlefieldPanel.java CENTER Graphics2D render + mouse input
│   ├── EditorPanel.java     WEST editor tools (era/unit/stance palette, terrain)
│   ├── PlaybackBar.java     SOUTH Play/Pause/FF/Rewind timeline + Cinema toggle
│   ├── SandboxController.java wires SandboxState + SimEngine + panels
│   └── ThemeUI.java         shared colors/fonts helpers
└── app/      [TASK 3]
    └── Main.java            EDT bootstrap: build SandboxState → SimEngineImpl → WarRoomFrame
```

## Build & run (FIXED)

```bash
cd warroom
javac -d out $(find src -name "*.java")
java -cp out com.whim.warroom.app.Main            # launch the sandbox
java -cp out com.whim.warroom.engine.EngineSmokeTest   # headless engine self-check
```

---

## domain/ signatures  [TASK 1]

### `enum Era { ANTIQUITY, MEDIEVAL, MODERN }`
- `String label()` — e.g. "Antiquity".

### `enum Faction { BLUE, RED, NEUTRAL }`
- `java.awt.Color color()` — BLUE `new Color(70,130,220)`, RED
  `new Color(210,70,60)`, NEUTRAL `new Color(150,150,150)`.
- `Faction enemyOf()` — BLUE↔RED, NEUTRAL→NEUTRAL.

### `enum Stance { OFFENSIVE, DEFENSIVE, RETREAT }`
- `String label()`.

### `enum Biome { GRASSLAND, FOREST, HILLS, DESERT, SNOW, URBAN, WATER }`
- `Color color()` — base fill for the tile.
- `double moveCostMul()` — movement speed multiplier applied to units on the
  tile (GRASSLAND 1.0, FOREST 0.65, HILLS 0.7, DESERT 0.85, SNOW 0.6,
  URBAN 0.8, WATER 0.25).
- `double coverBonus()` — additive defense fraction when defending on it
  (FOREST/URBAN 0.25, HILLS 0.15, else 0.0).
- `boolean passable()` — WATER `false`, else `true`.

### `class Vec2`  (immutable)
- Constructor `Vec2(double x, double y)`; public `final double x, y;`
- `Vec2 add(Vec2 o)`, `Vec2 sub(Vec2 o)`, `Vec2 scale(double s)`
- `double len()`, `double dist(Vec2 o)`, `Vec2 normalized()` (zero-safe → (0,0))
- `static Vec2 lerp(Vec2 a, Vec2 b, double t)`

### `class TerrainTile`
Constructor `TerrainTile(int col, int row, Biome biome, double elevation)`
- `int getCol()`, `int getRow()`, `Biome getBiome()`, `void setBiome(Biome)`
- `double getElevation()` (0..1), `void setElevation(double)`

### `class MapState`
- `public static final double TILE_SIZE = 32.0;`
- Constructor `MapState(int cols, int rows)` — fills GRASSLAND, elevation 0.5.
- `int getCols()`, `int getRows()`
- `TerrainTile tile(int col, int row)` (clamped), `boolean inBounds(int c,int r)`
- `double worldWidth()` = cols*TILE_SIZE, `double worldHeight()` = rows*TILE_SIZE
- `TerrainTile tileAtWorld(double wx, double wy)`
- `double moveCostMulAtWorld(double wx, double wy)` — the biome mul at that point
- `double coverBonusAtWorld(double wx, double wy)`
- **Generator:** `static MapState generate(int cols, int rows, long seed, Biome dominant)`
  — value/diamond-square-style elevation noise seeded by `seed`, then biome
  assignment influenced by `dominant` (dominant biome is the majority, with
  water in low elevation and hills/snow in high). Deterministic per seed.

### `class UnitType`  (immutable archetype)
Constructor:
`UnitType(String id, String name, Era era, double maxHealth, double attack, double defense, double speed, double range, double maxMorale)`
- getters for all (`getId, getName, getEra, getMaxHealth, getAttack, getDefense,
  getSpeed, getRange, getMaxMorale`).
- `speed` is world-units per **second** at 1.0 terrain mul. `range` is world
  units. `attack`/`defense` are abstract points (see combat math). `maxMorale`
  0..100.

### `class UnitCatalog`  [RESEARCH deliverable]
- `static java.util.List<UnitType> all()` — every archetype (immutable order).
- `static java.util.List<UnitType> byEra(Era era)`.
- `static UnitType byId(String id)` — null if absent.
- Must contain **at least 3 archetypes per era** with distinct, balanced stats,
  e.g. Antiquity: Legionary / Hoplite / Archer / War Elephant; Medieval: Knight /
  Man-at-Arms / Longbowman / Trebuchet; Modern: Rifle Infantry / Machine-gun
  Team / Main Battle Tank / Artillery. Ids are stable lowercase-hyphen strings
  (e.g. `"roman-legionary"`). Document the stat table in a comment.

### `class Unit`  (live mutable instance)
Constructor `Unit(int id, UnitType type, Faction faction, Vec2 pos)`
- `int getId()` (final), `UnitType getType()`, `Faction getFaction()`
- `Vec2 getPos()` / `void setPos(Vec2)`; `double getHeading()` /
  `void setHeading(double)` (radians)
- `double getHealth()` / `void setHealth(double)`; `double getMorale()` /
  `void setMorale(double)` (start = type.maxMorale)
- `Stance getStance()` / `void setStance(Stance)` (default DEFENSIVE)
- `Route getRoute()` / `void setRoute(Route)` (nullable)
- `boolean isRouted()` / `void setRouted(boolean)` (panicked/fleeing)
- `boolean isAlive()` → `health > 0`

### `class Waypoint`
Constructor `Waypoint(Vec2 pos, int arrivalTick)`
- `Vec2 getPos()`, `int getArrivalTick()`
- detonation (optional synchronized blast that fires when the tick is reached):
  `boolean isDetonation()`, `void setDetonation(boolean)`,
  `double getBlastRadius()` / `void setBlastRadius(double)`,
  `double getBlastDamage()` / `void setBlastDamage(double)`

### `class Route`
Constructor `Route()`
- `java.util.List<Waypoint> getWaypoints()` (mutable, ordered)
- `void add(Waypoint w)`; `boolean isEmpty()`
- `int finalArrivalTick()` — arrivalTick of last waypoint, or 0 if empty

### `class MapMarker`
Constructor `MapMarker(Vec2 pos, String label, Color color)`
- `Vec2 getPos()`, `String getLabel()`, `Color getColor()`

### `class SandboxState`
- Constructor `SandboxState(MapState map, long seed)`
- `MapState getMap()`
- `java.util.List<Unit> getUnits()` (mutable)
- `java.util.List<MapMarker> getMarkers()` (mutable)
- `final Random rng` (public) — the one shared RNG, seeded by `seed`
- `long getSeed()`
- `int nextUnitId()` — monotonically increasing id source for new units
- `Unit unit(int id)` — or null
- Helpers: `void addUnit(Unit u)`, `void removeUnit(int id)`

### `class SimSnapshot`  (immutable render frame — engine → UI)
Nested public static value types (plain fields, set once in constructor):
```java
class UnitView {
    public final int id; public final String typeId; public final Faction faction;
    public final double x, y, heading, health, maxHealth, morale, maxMorale;
    public final Stance stance; public final boolean routed, alive;
    // all-args constructor
}
class BlastView {
    public final double x, y, radius, age; // age 0..1 for fade animation
    // all-args constructor
}
```
`SimSnapshot`:
- `int getTick()`
- `java.util.List<UnitView> getUnits()` (unmodifiable)
- `java.util.List<BlastView> getBlasts()` (unmodifiable, active blasts this frame)
- `boolean isFinished()` — true when one side is eliminated / all routed
- Constructor `SimSnapshot(int tick, List<UnitView> units, List<BlastView> blasts, boolean finished)`

### `interface SimListener`
- `void onFrame(SimSnapshot snap)` — invoked by the engine thread; the UI
  implementation MUST marshal any Swing work with `SwingUtilities.invokeLater`.

### `interface SimEngine`  (implemented by Task 2's `SimEngineImpl`)
```java
int TICKS_PER_SECOND = 60;

void loadScenario(SandboxState state); // deep-usable snapshot of the scenario at tick 0
void addListener(SimListener l);

void play();                 // start/resume advancing wall-clock → ticks
void pause();                // freeze at current tick (editor still viewable)
boolean isPlaying();

void setSpeed(double multiplier); // 0.25/0.5/1/2/4 — playback speed only
double getSpeed();

void seek(int tick);         // rewind or fast-forward to an absolute tick
int getCurrentTick();        // tick currently presented
int getMaxSimTick();         // furthest tick simulated so far (timeline length)

SimSnapshot snapshotAt(int tick); // deterministic frame for scrubbing (may simulate up to tick)
void reset();                // back to tick 0 with the loaded scenario
void shutdown();             // stop thread; called on window close
```
**Semantics for Task 2:** simulation is deterministic from the loaded
`SandboxState` + seed. Forward simulation advances integer ticks at
`TICKS_PER_SECOND * speed`. The engine keeps enough recorded frames (or replays
from tick 0 deterministically) so that `seek(t)` / `snapshotAt(t)` for any
`t <= getMaxSimTick()` returns the correct frame → this is Rewind. `onFrame` is
fired once per presented tick while playing.

---

## engine/ combat & morale math  [TASK 2] (guidance, not signatures)

- **Movement:** each tick, a unit with a `Route` moves toward the active
  waypoint. Effective speed = `type.speed / TICKS_PER_SECOND *
  map.moveCostMulAtWorld(pos)`. To honor a waypoint's `arrivalTick`, pace so the
  unit covers the remaining distance over the remaining ticks (clamp to max
  speed; if it cannot arrive in time it moves at max speed). Advance to the next
  waypoint when reached or its arrivalTick passes.
- **Detonation:** when the current tick ≥ a detonation waypoint's arrivalTick,
  emit a blast: every unit within `blastRadius` takes `blastDamage` scaled by
  `1 - dist/blastRadius`. Emit a `BlastView` for that frame.
- **Engagement:** units of opposing factions within `attacker.range` deal damage
  per tick. Suggested: `dmg = attack * stanceAtkMul * (1 - defenderCoverDef) /
  TICKS_PER_SECOND` where `defenderCoverDef` folds `defense` +
  `map.coverBonusAtWorld` + stance. Deterministic; RNG only for small variance
  via `state.rng` if desired.
- **Stances:** OFFENSIVE = higher attack mul, closes distance, ignores route once
  enemy in sight; DEFENSIVE = holds/route, higher effective defense; RETREAT =
  moves away from nearest enemy, minimal attack.
- **Morale:** decays on taking damage, nearby friendly deaths, being
  outnumbered; recovers slowly when safe. Below a panic threshold (e.g. 25) the
  unit becomes `routed=true` (flees toward friendly edge, cannot attack). A side
  is defeated when all its units are dead or routed → `SimSnapshot.finished`.
- `EngineSmokeTest.main` must assert: a unit reaches a waypoint by its
  arrivalTick (±1 tick tolerance), a detonation damages an in-radius unit, an
  isolated unit under fire eventually routs, and `seek`/`snapshotAt` are
  deterministic (same tick → identical unit positions).

---

## ui/ behavior  [TASK 3] (guidance, not signatures)

- **Two modes** toggled in `WarRoomFrame`: **Simulation (editor)** and **Battle
  (playback)**. `PlaybackBar` Play switches to Battle Mode and calls
  `SimEngine.play()`; Pause/rewind stay in Battle Mode; a "Back to Editor" /
  `reset()` returns to Simulation Mode.
- **Editor (`EditorPanel` + `BattlefieldPanel`):**
  - Terrain: pick dominant biome + regenerate (`MapState.generate`), or paint
    biomes; elevation shown as shading.
  - Unit palette grouped by `Era` from `UnitCatalog`; pick faction + stance;
    **click** on the field to place a `Unit`.
  - **Drag** from a selected unit to draw a `Route`: each click adds a
    `Waypoint`; assign arrival times (auto from a default speed, editable).
    Optionally mark a waypoint as a detonation with radius/damage.
  - **Drag-box** selection of units; drop `MapMarker`s.
- **Battle Mode (`BattlefieldPanel` renders `SimSnapshot`):** animated units
  (health ring, morale tint, routed flash), blasts, route ghosts.
- **`PlaybackBar`:** Play, Pause, Fast-Forward (cycles speed via
  `setSpeed`), Rewind/scrub timeline (`seek` across `0..getMaxSimTick()`), and a
  **Cinema Mode** toggle that hides `EditorPanel` + `PlaybackBar` chrome for a
  clean full-screen view (a hotkey/edge-hover brings the bar back).
- All mouse handling via `MouseListener`/`MouseMotionListener`; never block the
  EDT — engine work is on Task 2's thread, UI only reads snapshots.
- `Main.main` builds a small demo scenario (a generated map + a few BLUE vs RED
  units with routes) so the app is interesting the moment it launches, then
  shows `WarRoomFrame` on the EDT.
```
