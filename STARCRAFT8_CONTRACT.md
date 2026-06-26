# 8-Bit StarCraft — Shared Interface Contract (Java 8)

A standalone Swing real-time-strategy **demake**: a fast, retro, 8-bit-arcade-styled
RTS inspired by the *8-Bit StarCraft* concept (Owen Dennis, 2010). Three fully
playable races (Terran, Zerg, Protoss), worker economy (Minerals / Vespene Gas /
Supply), base building, a tech tree, a state-machine AI opponent, and real-time
grid combat. **All stats are invented/emulated for a simplified arcade feel — this
is NOT a reproduction of Blizzard's balance numbers.**

- App dir: `starcraft8/`
- Source root: `starcraft8/src/` (plain layout, package dirs under it — e.g. `starcraft8/src/com/whim/starcraft8/domain/UnitType.java`)
- Base package: `com.whim.starcraft8`
- **Java 8 ONLY.** No `var`, no text blocks, no switch expressions, no records, no `Stream.toList`. Plain Java 8.
- **No external libraries.** Only `javax.swing`, `java.awt`, `java.util`, `java.io`, `java.nio`. No Maven/Gradle, no downloaded assets.
- **No copyrighted assets.** Every sprite is drawn algorithmically with `Graphics2D` or from hardcoded `int[]`/`byte[]` pixel arrays. No external images.
- Compile: `javac -d out $(find starcraft8/src -name '*.java')`. Run: `java -cp out com.whim.starcraft8.app.Main`.

## File ownership (NO overlap between tasks)

- **Task 1 (domain + data)** owns `starcraft8/src/com/whim/starcraft8/domain/**` and `starcraft8/src/com/whim/starcraft8/data/**`.
- **Task 2 (engine + AI)** owns `starcraft8/src/com/whim/starcraft8/engine/**`.
- **Task 3 (Swing UI)** owns `starcraft8/src/com/whim/starcraft8/ui/**`.
- **Main class** (`starcraft8/src/com/whim/starcraft8/app/Main.java`) is written by the **orchestrator** during consolidation. Do NOT create it.

Tasks 2 and 3 MUST `import` the `domain` and `data` types **verbatim** and MUST NOT
create their own copies. Task 3 codes against the `engine` entry-point interfaces
**verbatim**. The contract fixes **public signatures and the threading model**, not
internal implementation. Each task may add small helper types *inside its own package
only*, without changing any signature below. The orchestrator compiles the whole tree.

---

## Coordinate & threading model (read first — applies to all tasks)

- The map is a tile grid `W×H` (default **48×48**). Unit positions are **continuous** `double x,y` in tile units (a unit at `(3.5, 4.0)` sits mid-tile). Tile of a unit = `(int)Math.floor(x), (int)Math.floor(y)`.
- The simulation runs at **60 ticks/sec** on a **background thread** owned by the engine. All cooldowns, build times, and speeds below are expressed in **ticks** and **tiles/tick** (or per-second where noted).
- **The UI never mutates domain state.** UI input is turned into `Command` objects (Task 2 type) and pushed via `Simulation.enqueue(Command)`. The engine drains the command queue at the start of each tick.
- **The UI reads state under the engine's lock.** Task 2 exposes `Simulation.render(WorldReader)` style access via a single method `Simulation.readState(java.util.function.Consumer<WorldReader> reader)` that runs the consumer while holding the engine lock. Task 3 does ALL of its reading of unit/building/resource data inside that callback (copying primitives/coords it needs into its own lightweight render lists), then paints on the EDT. This is the only cross-thread contract that matters; honor it exactly.

---

## `com.whim.starcraft8.domain` — authored by Task 1

> Task 1 fixes the **public API (getters)** below verbatim. The **numeric stat values**
> are Task 1's to finalize/tune, but the baseline table at the bottom is the agreed
> starting point so Task 2/3 can reason about ranges. Enums must NOT cross-reference
> each other's constants in their constructors (avoids static-init ordering bugs);
> production/tech relationships live in `data.TechTree`.

### Enums

```java
public enum Race { TERRAN, ZERG, PROTOSS }

public enum ResourceType { MINERALS, GAS }

public enum ArmorClass { SMALL, MEDIUM, LARGE }

// 8-bit damage model: NORMAL = full vs all; EXPLOSIVE = full vs LARGE, 1/2 vs SMALL;
// CONCUSSIVE = full vs SMALL, 1/2 vs LARGE. (MEDIUM = full for both special types.)
public enum DamageType { NORMAL, EXPLOSIVE, CONCUSSIVE }

public enum AttackKind { NONE, MELEE, RANGED }

public enum UnitState { IDLE, MOVING, ATTACKING, GATHERING, RETURNING, BUILDING, MORPHING, DEAD }

public enum BuildState { UNDER_CONSTRUCTION, COMPLETE, PRODUCING }
```

### `UnitType` (the embedded unit data dictionary)

Enum constants carry all combat/economy stats. Required public accessors (verbatim names):

```java
public enum UnitType {
    // constants listed in baseline table below
    ;
    public Race race();
    public String displayName();
    public int mineralCost();
    public int gasCost();
    public int supplyCost();      // supply consumed
    public int supplyProvided();  // supply granted (Overlord); else 0
    public int maxHp();
    public int maxShield();       // Protoss; else 0
    public int armor();
    public ArmorClass armorClass();
    public int damage();
    public DamageType damageType();
    public AttackKind attackKind();
    public double range();        // tiles (MELEE ~ 1.0)
    public int cooldown();        // ticks between attacks
    public double speed();        // tiles per tick
    public double sight();        // tiles
    public int buildTicks();      // production/morph time
    public boolean isWorker();
    public boolean isFlyer();
    public int splashRadius();    // tiles, 0 = single target
    public java.awt.Color baseColor();  // race/role tint for the UI sprite
}
```

### `BuildingType` (the embedded building data dictionary)

```java
public enum BuildingType {
    ;
    public Race race();
    public String displayName();
    public int mineralCost();
    public int gasCost();
    public int maxHp();
    public int supplyProvided();  // CommandCenter/Hatchery/Nexus, Depot/Pylon
    public int widthTiles();      // footprint
    public int heightTiles();
    public int buildTicks();
    public boolean isTownHall();  // produces workers, accepts resource return
    public boolean isSupply();    // Supply Depot / Pylon
    public boolean isGas();       // Refinery / Extractor / Assimilator
    public java.awt.Color baseColor();
}
```

### Mutable runtime entities (concrete classes authored by Task 1)

These are the live objects the engine mutates and the UI reads. Fields are private;
the listed getters/setters are the contract. Plain mutable POJOs — no logic beyond
trivial helpers (clamping, `distanceTo`).

```java
public final class Unit {
    public Unit(UnitType type, int ownerId, double x, double y);
    public UnitType type();
    public int ownerId();
    public double x(); public double y();
    public void setPos(double x, double y);
    public int hp(); public void setHp(int hp); public int shield(); public void setShield(int s);
    public UnitState state(); public void setState(UnitState s);
    public int cooldownLeft(); public void setCooldownLeft(int t);
    public long id();                 // unique, assigned in ctor via an AtomicLong
    public boolean alive();           // hp > 0 && state != DEAD
    public double distanceTo(double tx, double ty);
    // order target (engine-owned semantics): a destination and/or target entity id
    public double targetX(); public double targetY(); public void setTarget(double x, double y);
    public long targetEntityId(); public void setTargetEntityId(long id);  // -1 = none
    public int carriedResource();     // amount a worker is carrying
    public ResourceType carriedType(); public void setCarried(ResourceType t, int amt);
    public int progressTicks(); public void setProgressTicks(int t); // build/morph timer
}

public final class Building {
    public Building(BuildingType type, int ownerId, int tileX, int tileY);
    public BuildingType type(); public int ownerId();
    public int tileX(); public int tileY();
    public int hp(); public void setHp(int hp);
    public BuildState state(); public void setState(BuildState s);
    public long id();
    public boolean alive();
    public int buildProgress(); public void setBuildProgress(int t);  // ticks toward COMPLETE
    public java.util.Deque<UnitType> productionQueue();              // FIFO of training orders
    public int productionTicksLeft(); public void setProductionTicksLeft(int t);
    public double rallyX(); public double rallyY(); public void setRally(double x, double y);
}

public final class Projectile {
    public Projectile(double x, double y, long targetId, int damage, DamageType dmgType, int splashRadius, double speed, java.awt.Color color);
    public double x(); public double y(); public void setPos(double x, double y);
    public long targetId(); public int damage(); public DamageType damageType();
    public int splashRadius(); public double speed(); public java.awt.Color color();
    public boolean done(); public void setDone(boolean d);
}
```

### Map

```java
public enum Terrain { GROUND, MINERAL_FIELD, GEYSER, UNBUILDABLE }

public final class GameMap {
    public GameMap(int width, int height);
    public int width(); public int height();
    public Terrain terrainAt(int tx, int ty);
    public void setTerrain(int tx, int ty, Terrain t);
    public boolean inBounds(int tx, int ty);
    public boolean buildable(int tx, int ty, int w, int h);   // all tiles GROUND & unoccupied-by-terrain
    // mineral/geyser remaining amounts keyed by packed tile (ty*width+tx)
    public int resourceAt(int tx, int ty); public void setResourceAt(int tx, int ty, int amt);
}
```

### Player / GameState

```java
public final class Player {
    public Player(int id, Race race, boolean ai);
    public int id(); public Race race(); public boolean isAi();
    public int minerals(); public void addMinerals(int d);
    public int gas(); public void addGas(int d);
    public int supplyUsed(); public int supplyCap();          // cap clamped to 200
    public void setSupplyUsed(int u); public void setSupplyCap(int c);
    public boolean defeated(); public void setDefeated(boolean d);
}

public final class GameState {
    public GameState(GameMap map, java.util.List<Player> players);
    public GameMap map();
    public java.util.List<Player> players();
    public Player player(int id);
    public java.util.List<Unit> units();          // mutable live list
    public java.util.List<Building> buildings();   // mutable live list
    public java.util.List<Projectile> projectiles();
    public long tick(); public void setTick(long t);
    public int winnerId();  public void setWinnerId(int id);  // -1 until decided
}
```

---

## `com.whim.starcraft8.data` — authored by Task 1

- `TechTree` — the production/prerequisite graph, with **static** lookups (no cross-enum init hazards):
  ```java
  public final class TechTree {
      public static java.util.List<UnitType> producedBy(BuildingType b);   // train menu
      public static java.util.List<BuildingType> buildableBy(Race r);      // worker build menu
      public static java.util.List<BuildingType> prerequisites(BuildingType b); // required completed buildings
      public static BuildingType townHall(Race r);
      public static UnitType worker(Race r);
      public static UnitType supplyUnit(Race r);   // Overlord for Zerg, else null
      public static BuildingType supplyBuilding(Race r); // Depot/Pylon, null for Zerg
  }
  ```
- `MapFactory` — `public static GameState newSkirmish(Race human, Race ai)` builds the 48×48 map with two mineral lines + geysers near each start location, places each player's town hall + 4 workers, sets starting resources (**50 minerals, 0 gas, supplyCap 10**), and returns a ready `GameState`. This is the single entry point the engine/UI use to start a match.
- `Balance` — named constants: `TICKS_PER_SECOND=60`, `WORKER_CARRY=8`, `GATHER_TICKS=40`, `START_MINERALS=50`, `SUPPLY_MAX=200`, mineral-field amount, geyser amount, etc.

### Baseline stat table (Task 1 finalizes; keep these as the agreed starting values)

Workers — SCV/Drone/Probe: 50 min, supply 1, hp 40 (Probe 20 hp + 20 shield), melee dmg 5, speed 0.055, sight 7, build 300.

Supply: Supply Depot/Pylon — 100 min, hp 100/80(+shield), supplyProvided 8. Overlord — 100 min, hp 120, flyer, AttackKind.NONE, supplyProvided 8, build 600.

Town halls — Command Center/Hatchery/Nexus: 400 min, hp 400, supplyProvided 10, townHall.

Gas: Refinery/Extractor/Assimilator — 75 min, hp 120, isGas.

Production buildings: Barracks 150 / Factory 200 (prereq Barracks) / Spawning Pool 150 / Hydralisk Den 100 (prereq Pool) / Gateway 150. hp ~200, build 360–600.

Combat units (min/gas/supply | hp(+shield) | dmg type kind | range | cd | speed):
- Terran Marine: 50/0/1 | 40 | 6 NORMAL RANGED | 4 | 15 | 0.045
- Terran Firebat: 50/25/1 | 50 | 8 CONCUSSIVE MELEE splash1 | 1 | 22 | 0.045
- Terran Siege Tank: 150/100/2 | 150 | 30 EXPLOSIVE RANGED splash1 | 7 | 40 | 0.030
- Zerg Zergling: 25/0/1 | 35 | 5 NORMAL MELEE | 1 | 8 | 0.075 (built two per order — engine spawns 2)
- Zerg Hydralisk: 75/25/1 | 80 | 10 EXPLOSIVE RANGED | 4 | 16 | 0.050
- Protoss Zealot: 100/0/2 | 60+40 | 8 NORMAL MELEE | 1 | 22 | 0.045
- Protoss Dragoon: 125/50/2 | 80+80 | 12 EXPLOSIVE RANGED | 5 | 22 | 0.050

ArmorClass: workers/Marine/Zergling/Firebat = SMALL; Hydralisk/Zealot = MEDIUM; Siege Tank/Dragoon = LARGE. Protoss shields regenerate slowly (engine: +1 shield / 32 ticks when out of combat).

---

## `com.whim.starcraft8.engine` — authored by Task 2

The engine owns the simulation thread, the command queue, pathfinding, combat math,
resource economy, production, supply accounting, win/lose detection, and the AI.

### Read interface for the UI (verbatim — Task 3 codes against this)

```java
public interface WorldReader {
    GameState state();   // live GameState; only valid INSIDE Simulation.readState callback
}

public interface Simulation {
    void start();        // spins up the 60-tps background thread
    void stop();
    void enqueue(Command c);
    void readState(java.util.function.Consumer<WorldReader> reader); // runs under engine lock
    boolean isRunning();
    int humanPlayerId();
}
```

### Commands (verbatim shape; Task 2 owns concrete subclasses/fields)

`Command` is an interface in `engine`. The orchestrator and Task 3 construct commands
only via the static factory `Commands` so the UI never depends on concrete classes:

```java
public final class Commands {
    public static Command move(java.util.List<Long> unitIds, double tx, double ty);
    public static Command attackMove(java.util.List<Long> unitIds, double tx, double ty);
    public static Command attackTarget(java.util.List<Long> unitIds, long targetId);
    public static Command gather(java.util.List<Long> workerIds, long resourceBuildingOrFieldId); // -1 picks nearest
    public static Command build(long workerId, BuildingType type, int tileX, int tileY);
    public static Command train(long buildingId, UnitType type);
    public static Command setRally(long buildingId, double tx, double ty);
    public static Command stop(java.util.List<Long> unitIds);
}
```
> Resource fields are addressed by a **packed tile id** = `-(ty*width+tx)-2` (negative,
> to never collide with entity ids ≥ 0). `gather` with `-1` auto-targets the nearest field.

### Factory

```java
public final class Engine {
    public static Simulation create(GameState state, int humanPlayerId);  // AI drives all other players
}
```

### Required engine behavior

- **Economy:** workers walk to a mineral field/geyser, spend `GATHER_TICKS`, carry `WORKER_CARRY`, return to nearest owned town hall (gas requires a gas building on the geyser), deposit. Building a gas structure on a geyser enables gas gathering there.
- **Build/train:** validate cost + supply + prerequisites; deduct on order; refund on cancel/failed placement. Supply cap = sum of town-hall(10) + depot/pylon(8) + overlord(8), clamped to 200; a build is rejected if it would exceed cap. Zergling trains **two** units per order for one supply each (keep simple: 1 supply each, two spawned).
- **Combat:** target acquisition within sight, move into range, fire on cooldown. RANGED spawns a `Projectile` that homes the target id and applies damage on arrival; MELEE applies instantly at range. Apply the `DamageType` vs `ArmorClass` multiplier, subtract armor (min 1 dmg). Shields absorb first. Splash hits enemies within `splashRadius`.
- **Pathfinding:** simple grid steering is acceptable (greedy step toward target with obstacle nudge / light BFS). It does not need to be optimal; it must not deadlock units permanently.
- **AI opponent (state machine):** per AI player, loop: keep ~1.2 workers per mineral patch, build supply before capped, expand tech (town hall → supply → first production building → gas → second production), continuously train a mixed army, and once it has N army-supply, issue an `attack-move` wave toward the human's town hall. Re-arm and repeat. Must work for all three races off `TechTree`.
- **Win/lose:** a player with no buildings AND no units is defeated; last undefeated player → `GameState.setWinnerId`.
- **Decoupling:** zero `javax.swing`/`java.awt` imports except `java.awt.Color`/`Point` for data. No rendering.
- Provide `engine/EngineSmokeTest.java` with a `public static void main` that runs a few thousand headless ticks of an AI-vs-AI match and prints unit counts / winner — the orchestrator runs this to verify.

---

## `com.whim.starcraft8.ui` — authored by Task 3

- `GameFrame extends JFrame` — the app window; constructed with a `Simulation`. Owns a Swing `Timer` (~30–60 fps) that calls `simulation.readState(...)`, snapshots render data, and repaints.
- `GamePanel extends JPanel` — the world viewport. Renders terrain, mineral/geyser tiles, buildings, units, projectiles, selection boxes, health/shield bars, and rally points using `Graphics2D` only. **8-bit aesthetic:** chunky pixel sprites drawn from small hardcoded `int[][]` palettes/bitmaps per `UnitType`/`BuildingType` (use `type.baseColor()` as the tint), nearest-neighbour scaling, a low-color palette, hard edges (no anti-aliasing).
- `Hud` (bottom command console) — minerals / gas / supply (e.g. `12/24`), a minimap, the current selection portrait, and contextual action buttons (Build menu when a worker is selected; Train menu when a production building is selected; Move/Attack/Stop). Buttons translate to `Commands.*` + `simulation.enqueue`.
- **Input:** left-drag = box-select own units; left-click = select; right-click = contextual order (move, attack enemy under cursor, or gather on a resource field); keyboard hotkeys (`B` build menu, `A` attack-move, `S` stop, `Esc` cancel). Click-to-place footprint preview for building.
- Pure presentation: UI never writes domain fields; all mutation goes through `Commands` + `simulation.enqueue`. All reads happen inside `simulation.readState`.
- A pixel-font or default `Font` is fine; keep colors few and bold for the retro look.

---

## Consolidation (orchestrator)

1. Merge all three task branches into the orchestrator branch.
2. Write `app/Main.java`: pick races (default human TERRAN vs ai ZERG, or a tiny race-select dialog), `GameState gs = MapFactory.newSkirmish(...)`, `Simulation sim = Engine.create(gs, humanId)`, `new GameFrame(sim).setVisible(true)`, `sim.start()`.
3. `javac -d out $(find starcraft8/src -name '*.java')` — fix any interface friction.
4. Run `EngineSmokeTest` headless to verify the simulation reaches a winner without deadlock.
5. Update root `README.md` with a build/run line for `starcraft8`.
