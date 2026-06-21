# Mad Games Tycoon (Turn-Based) — Shared Interface Contract (Java 8)

Binding API contract for a standalone, **strictly turn-based** adaptation of *Mad Games
Tycoon*. **1 Turn = 1 in-game hour.** All three child tasks MUST adhere to these package
names, type names, and method signatures EXACTLY so the modules compile and link without
friction.

Build: Maven, Java 8 (`maven.compiler.source/target = 1.8`, already set in `pom.xml`).
**No `var`, no text blocks, no records, no switch expressions, no post-8 Stream
collectors (`Collectors.toUnmodifiableList`, `Stream.toList()`).** Java 8 lambdas and the
Java 8 Stream API are fine.

Sources live under `src/main/java/`, tests under `src/test/java/`.
Existing projects (`com.finesse.*`, `com.tiwa.*`, `com.janggi.*`, `com.xiangqi.*`,
`com.midnight.*`) are OFF LIMITS — do not touch, move, or rename them. Add ONLY
`com.tycoon.*`.

## Module ownership
- `com.tycoon.core` — **Task 1**: domain models, `GameState`, project pipeline, AI
  competitor models, the Ascension-style interrupt-driven auto-turn loop.
- `com.tycoon.sim` — **Task 2**: point generation, stress/recovery, bug & polish,
  review scoring with Reviewer-RNG variance, and the AI-competitor tick.
- `com.tycoon.ui` — **Task 3**: Swing 2D grid for the freeform building phase and the
  turn/interrupt control panel.

Task 2 and Task 3 IMPORT `com.tycoon.core` types; they never redefine them. Task 1
must NOT depend on `sim` or `ui`. Task 2 must NOT depend on `ui`.

---

## Core architectural rules (binding)
1. **Time scale.** Each call to advance one turn = exactly one in-game hour. `GameState`
   tracks an absolute `long hour` counter (0-based) plus derived day/week.
2. **Interrupt-driven auto-turn.** The engine repeatedly auto-advances hourly turns and
   HALTS, returning control, only when an `Interrupt` is produced (development milestone,
   employee crisis, market shift, game release, or manual pause/limit).
3. **Simultaneous resolution.** A single turn advances the player studio AND all ~100 AI
   competitors together.
4. **Freeform layout is paused.** Room/object edits happen while paused; costs are
   deducted immediately by the UI calling `core` mutators. The grid/pathfinding state is
   only *finalized* (locked) when the turn advances.

---

## Package: `com.tycoon.core` — owned by Task 1

### `final class GridPos`
Immutable value type. `public static GridPos of(int x, int y);` plus `x()`, `y()`,
`equals`/`hashCode`/`toString`.

### `enum FacilityType`
```
public enum FacilityType {
    DESK,            // workstation; required for an employee to produce points
    COFFEE_MACHINE,  // stress recovery
    HEATER,          // stress recovery
    PLANT,           // stress recovery
    ARCADE_CABINET;  // stress recovery
    public int cost();              // money to place
    public double stressRelief();   // hourly stress reduction radius-effect magnitude (0 for DESK)
    public boolean isWorkstation(); // true only for DESK
}
```

### `enum RoomType`
```
public enum RoomType {
    DEVELOPMENT, RESEARCH, QA, LOUNGE, SERVER, MARKETING;
    public int floorCostPerTile();
}
```

### `final class Facility`
A placed object. `FacilityType type()`, `GridPos pos()`.
`public static Facility at(FacilityType type, GridPos pos);`

### `class Room`
A rectangular region of the floor grid.
```
public Room(RoomType type, int x, int y, int width, int height);
public RoomType type();
public int x(); public int y(); public int width(); public int height();
public boolean contains(GridPos p);
public List<Facility> facilities();   // live, ordered
public void addFacility(Facility f);  // caller ensures inside room + not overlapping
```

### `class FloorPlan`
The freeform building grid. Mutated only while paused; `lock()`/`isLocked()` enforce that
the layout is finalized on turn advance.
```
public FloorPlan(int width, int height);
public int width(); public int height();
public List<Room> rooms();
public void addRoom(Room r);                 // throws IllegalStateException if locked
public boolean placeFacility(Facility f);    // false if out of bounds/occupied/locked
public Facility facilityAt(GridPos p);       // null if empty
public Room roomAt(GridPos p);               // null if no room
public void lock();                          // finalize on turn execution
public void unlock();                        // re-open for the paused building phase
public boolean isLocked();
```

### `class Employee`
```
public Employee(String id, String name, int baseSkill /*S_base, 1..100*/);
public String id(); public String name();
public int baseSkill();
public double stress();              // 0..100, the σ_stress meter
public void setStress(double s);     // clamped 0..100 by impl
public GridPos workstation();        // assigned DESK pos, null if unassigned
public void assignWorkstation(GridPos deskPos);
public String assignedProjectId();   // null if idle
public void assignProject(String projectId);
```

### `enum ProjectPhase { DESIGN, PRODUCTION, POLISH, RELEASED; }`

### `class GameProject`
The development pipeline for one game in production.
```
public GameProject(String id, String title);
public String id(); public String title();
public ProjectPhase phase(); public void setPhase(ProjectPhase p);
public double developmentPoints();   // accumulated P, total
public void addDevelopmentPoints(double p);
public double bugs();                // accumulated bug count
public void addBugs(double b);
public double polish();              // 0..100 polish progress
public void addPolish(double amount);
public Integer reviewScore();        // null until scored; 0..100 once RELEASED+reviewed
public void setReviewScore(int score);
public List<String> assignedEmployeeIds();
```

### `class AiStudio`
A simulated competitor (≈100 of them).
```
public AiStudio(String id, String name, double strength /*0..1 quality bias*/);
public String id(); public String name();
public double strength();
public long cash();          public void addCash(long delta);
public int releasedGames();  public void incrementReleasedGames();
public Integer lastReviewScore(); public void setLastReviewScore(int score);
```

### `enum InterruptType`
```
public enum InterruptType {
    DEVELOPMENT_MILESTONE,  // a phase boundary reached
    EMPLOYEE_CRISIS,        // an employee stress >= crisis threshold
    MARKET_SHIFT,           // competitor chart upheaval
    GAME_RELEASED,          // player project reviewed & shipped
    MANUAL_PAUSE;           // player asked to stop / turn budget exhausted
}
```

### `final class Interrupt`
```
public Interrupt(InterruptType type, long hour, String message);
public InterruptType type(); public long hour(); public String message();
```

### `class GameStudio` (the player)
```
public GameStudio(String name);
public String name();
public long cash(); public void addCash(long delta);
public FloorPlan floorPlan();
public List<Employee> employees();
public List<GameProject> projects();
public Employee employee(String id);    // null if none
public GameProject project(String id);  // null if none
```

### `class GameState`
The root aggregate.
```
public GameState(GameStudio player, List<AiStudio> competitors, long seed);
public long hour();                  // absolute hour counter, starts at 0
public int day();                    // hour / 24
public int week();                   // day / 7
public GameStudio player();
public List<AiStudio> competitors(); // ~100
public java.util.Random rng();       // single seeded source — sim MUST use this, not new Random()
public boolean isGameOver();
public void advanceHourCounter();    // ++hour; called by the loop, not by sim
```
`GameState.newGame(long seed)` static factory builds a player studio with a default
`FloorPlan` (at least **40x30**), a handful of starting employees, and **100**
`AiStudio` competitors with varied strengths.

### `interface TurnProcessor` — implemented by Task 2, called by Task 1's loop
This is the seam between core and sim. Task 1 declares it; Task 2 provides the impl.
```
public interface TurnProcessor {
    // Resolve exactly ONE in-game hour for player + all competitors, mutating state.
    // Return every Interrupt produced this hour (empty list = none).
    List<Interrupt> processHour(GameState state);
}
```

### `class AutoTurnEngine` — owned by Task 1
The Ascension-style interrupt-driven loop.
```
public AutoTurnEngine(GameState state, TurnProcessor processor);

// Auto-advance hour-by-hour until an Interrupt is produced OR maxHours elapse.
// On each hour: lock the FloorPlan, call processor.processHour, advance the counter.
// Stops and returns as soon as any interrupt is produced (control returns to player).
public List<Interrupt> run(int maxHours);

// Advance exactly one hour regardless of interrupts (used by UI single-step).
public List<Interrupt> step();

public GameState state();
```
On returning control, the engine should `unlock()` the FloorPlan so the paused building
phase can resume. (UI re-locks implicitly by calling `run`/`step`.)

---

## Package: `com.tycoon.sim` — owned by Task 2 (high-reasoning)

Provides `class SimTurnProcessor implements com.tycoon.core.TurnProcessor`. All randomness
MUST come from `state.rng()`. Pure functions where practical; expose them for unit tests:

### Point generation (binding formula)
```
// P_hourly = (S_base * mu_ws) * max(0.1, 1 - sigma_stress/100)
public static double hourlyPoints(int baseSkill, double workstationMultiplier, double stress);
```
`mu_ws` (`workstationMultiplier`) is the desk/room quality multiplier (≈1.0 baseline,
higher for better-equipped development rooms). An employee with no DESK produces 0.

### Stress & recovery (binding)
```
// Net hourly stress delta BEFORE clamping: base accrual minus facility relief in the room.
public static double hourlyStressDelta(double baseAccrual, double facilityRelief);
public static final double STRESS_CRISIS_THRESHOLD; // e.g. 90.0 -> EMPLOYEE_CRISIS
```
Facility relief = sum of `FacilityType.stressRelief()` for stress facilities in the
employee's room (Coffee/Heater/Plant/Arcade). Stress clamps to [0,100].

### Bugs & polish (binding)
During PRODUCTION, bugs accumulate alongside points; during POLISH, bugs are burned down
and `polish()` rises. Expose the per-hour math as static helpers for tests.

### Review scoring with Reviewer RNG (binding)
```
// Base from development points & polish, then apply bounded RNG variance. 0..100.
public static int reviewScore(double developmentPoints, double bugs, double polish,
                              java.util.Random rng);
```
The Reviewer-RNG variance must be bounded (e.g. ±10) so good games usually score well but
the charts stay unpredictable. AI competitors get scored with the same idea via
`state.rng()`.

### `processHour` responsibilities (binding)
For the one hour: accrue points/bugs/polish per assigned employee, update stress with
facility relief, advance `ProjectPhase` at thresholds (emit `DEVELOPMENT_MILESTONE`),
emit `EMPLOYEE_CRISIS` when stress ≥ threshold, tick the 100 competitors (occasionally
emit `MARKET_SHIFT`), and on player release emit `GAME_RELEASED`. Return all interrupts.

---

## Package: `com.tycoon.ui` — owned by Task 3 (Swing)

Imports `com.tycoon.core` only. A `JFrame`-based app:
- A `JPanel` rendering the `FloorPlan` 2D grid (rooms tinted by `RoomType`, facilities as
  icons/glyphs), grid cell size constant.
- **Freeform building phase** (paused): mouse-drag to draw a `Room`, click to place a
  `Facility` from a palette; each placement calls `core` mutators and deducts cash. Edits
  are rejected when `floorPlan.isLocked()`.
- A control bar: **Advance** (calls `AutoTurnEngine.run(maxHours)`), **Step** (`step()`),
  and a log/console showing returned `Interrupt`s (type + hour + message). When control
  returns, re-enter the paused building phase.
- `class TycoonApp { public static void main(String[] args) } ` boots a `GameState.newGame`
  and shows the frame. (Final consolidated main class; Task 3 owns it.)

`TycoonApp` must run headless-safe enough to instantiate models without throwing if no
display — guard `main` so the engine/state can be smoke-tested. (A simple
`GraphicsEnvironment.isHeadless()` check is fine.)

---

## Definition of done (per child)
- Compiles under Java 8 with `mvn -q compile`.
- Owns ONLY its package; imports the contract types above by their exact signatures.
- Task 1 & Task 2 ship JUnit tests under `src/test/java/com/tycoon/...`.
- Pushes its branch and opens a PR back to `whim-wd-179`, then reports via `send_prompt`.
