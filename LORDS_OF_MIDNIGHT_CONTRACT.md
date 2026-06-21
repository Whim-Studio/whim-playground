# The Lords of Midnight — Shared Interface Contract (Java 8)

Binding API contract for the Lords of Midnight project (a standalone adaptation of
the 1984 classic). All three child tasks MUST adhere to these package names, type
names, and method signatures EXACTLY so the modules compile and link without friction.

Build: Maven, Java 8 (`maven.compiler.source/target = 1.8`, already set in `pom.xml`).
**No `var`, no text blocks, no records, no switch expressions, no post-8 Stream
collectors (e.g. `Collectors.toUnmodifiableList`, `Stream.toList()`).** Java 8 lambdas
and the Java 8 Stream API are fine.

Sources live under `src/main/java/`, tests under `src/test/java/`.
Existing projects (`com.finesse.*`, `com.tiwa.*`, `com.janggi.*`, `com.xiangqi.*`) are
OFF LIMITS — do not touch, move, or rename their files. Add ONLY `com.midnight.*`.

## World model (binding)
- Map is a rectangular grid, `width` columns (x: 0..width-1) by `height` rows
  (y: 0..height-1). **y=0 is the NORTH edge; y increases SOUTH.** Default map is
  `Map.standard()` sized **at least 60x40**.
- Two sides: **FREE** (the player — the Lords of Light) and **DOOMDARK** (the Witchking
  and his foul armies).
- The player wins by EITHER the Adventure path (Morkin destroys the Ice Crown) OR the
  Wargame path (a Free lord captures Doomdark's citadel Ushgarak). Doomdark wins if
  Luxor the Moonprince is slain, or Doomdark captures the Citadel of the Moon (Xajorkith).
- Strict Day/Night cycle. **Characters may ONLY move/fight during DAY.** At NIGHT,
  Doomdark's armies move and resolve attacks; Free lords are frozen.
- **Only Morkin may carry and destroy the Ice Crown.** No other lord may bear it.
- The northern **Mountains of Ithorn** and any `MOUNTAINS`/`LAKE` tile are impassable.

---

## Package: `com.midnight.core` — owned by Task 1

### `enum Side`
```
public enum Side {
    FREE,       // the player's Lords of Light
    DOOMDARK;   // the Witchking's armies
    public Side opponent();
}
```

### `enum Direction` (8-way compass, clockwise from north)
```
public enum Direction {
    NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST;
    public int dx();              // -1,0,+1   (EAST = +1)
    public int dy();              // -1,0,+1   (NORTH = -1, SOUTH = +1)
    public Direction opposite();
    public Direction clockwise();        // turn 45 deg right
    public Direction anticlockwise();    // turn 45 deg left
}
```

### `enum Terrain`
```
public enum Terrain {
    PLAINS, FOREST, MOUNTAINS, SNOW, DOWNS, WASTELAND,
    CITADEL, KEEP, TOWER, VILLAGE, HENGE, RUINS, LAKE;
    public boolean isPassable();   // false for MOUNTAINS and LAKE
    public int moveCost();         // hours to ENTER a tile of this terrain (see below)
}
```
Suggested `moveCost` (hours): PLAINS 4, DOWNS 5, VILLAGE 4, CITADEL/KEEP/TOWER 4,
HENGE/RUINS 6, FOREST 8, SNOW 9, WASTELAND 10, FOREST near mountains heavier is fine.
MOUNTAINS/LAKE return a large/irrelevant cost and are impassable. Task 1 owns tuning.

### `final class Location` (immutable value type)
```
public static Location of(int x, int y);
public int x();
public int y();
public Location neighbor(Direction d);          // x+dx, y+dy
public int chebyshevDistanceTo(Location other);
// value equality + hashCode + toString ("x,y")
```

### `class Map`
Holds terrain + named strongholds.
```
public Map(int width, int height);
public static Map standard();                   // the canonical Midnight map (>=60x40)
public int width();
public int height();
public boolean inBounds(Location loc);
public Terrain terrainAt(Location loc);         // PLAINS for out-of-data tiles
public void setTerrain(Location loc, Terrain t);
public boolean isPassable(Location loc);        // inBounds && terrain.isPassable()
public Stronghold strongholdAt(Location loc);   // null if none
public java.util.List<Stronghold> strongholds();
```

### `class Stronghold` (citadel / keep / tower)
```
public Stronghold(String name, Location location, Terrain type, Side owner, int garrison);
public String name();
public Location location();
public Terrain type();                 // CITADEL, KEEP, or TOWER
public Side owner();
public void setOwner(Side s);
public int garrison();                 // defending warriors
public void setGarrison(int n);
public boolean isUshgarak();           // Doomdark's home citadel (wargame target)
public boolean isXajorkith();          // Citadel of the Moon (Luxor's home)
```

### `class Character` (a Lord, mutable — engine and AI mutate via setters)
```
public Character(String name, Side side, Location location, Direction facing);
public String name();
public Side side();                    // recruited independents become FREE
public Location location();   public void setLocation(Location loc);
public Direction facing();    public void setFacing(Direction d);
public int energy();          public void setEnergy(int e);      // stamina 0..127
public int courage();         public void setCourage(int c);     // 0..127
public int hoursRemaining();  public void setHoursRemaining(int h); // action pts left today
public int warriors();        public void setWarriors(int n);    // foot soldiers
public int riders();          public void setRiders(int n);      // mounted soldiers
public boolean isAlive();     public void kill();
public boolean isMounted();                                       // riders>0 -> faster
public boolean isRecruited(); public void setRecruited(boolean b);// under player control
public boolean isMorkin();                                        // name.equals("Morkin")
public boolean isLuxor();                                         // name.equals("Luxor")
public boolean carriesIceCrown();   public void setCarriesIceCrown(boolean b); // Morkin only
```

### `enum Phase { DAY, NIGHT }`

### `enum Outcome { ONGOING, FREE_ADVENTURE_WIN, FREE_WARGAME_WIN, DOOMDARK_WIN }`

### `class GameState` — the central engine object
```
public static GameState newGame();        // standard map, dawn of Day 1, FREE to act

public Map map();
public int day();                         // starts at 1
public Phase phase();                     // DAY or NIGHT
public java.util.List<Character> characters();
public java.util.List<Character> charactersOf(Side s);
public java.util.List<Character> playerLords();          // FREE && recruited && alive
public java.util.List<Character> charactersAt(Location loc);
public Character selected();              // the player's currently-controlled lord
public void select(Character c);
public Location iceCrownLocation();       // where the Ice Crown currently rests

// ----- DAY actions (only valid while phase()==DAY and c is FREE/recruited & alive) -----
public boolean canMove(Character c, Direction d);   // passable dest, enough hours, alive, day
public int moveCost(Character c, Direction d);      // hours; mounted lords pay less
public boolean move(Character c, Direction d);      // applies move, deducts hours; false if illegal
public void look(Character c, Direction d);         // set facing only (free action)
public boolean tryRecruit(Character lord);          // recruit an independent lord co-located w/ a recruiter
public boolean tryDestroyIceCrown();                // true if Morkin (bearer) is at iceCrownLocation -> Adventure win

// ----- turn cycle -----
public boolean isDay();
public void endDay(NightResolver resolver);   // DAY->NIGHT: invoke Doomdark AI/combat, then dawn of next day
                                              // (resets hoursRemaining for living lords, recovers energy)

// ----- victory -----
public Outcome outcome();
public boolean isOver();
```

Binding rules Task 1 MUST enforce:
- `move` fails (returns false) if: not DAY, character not alive/recruited, destination not
  passable/in-bounds, or `hoursRemaining() < moveCost`. On success: deduct hours, update
  location, set facing toward `d`, drain a little energy. Mounted lords (riders>0) pay a
  reduced cost (e.g. 75%).
- Characters **cannot move at night** — `move`/`canMove` must return false when `phase()==NIGHT`.
- `endDay` MUST call `resolver.resolveNight(this)` (see Task 2 interface) exactly once,
  then advance to the next day's dawn: increment `day()`, set `phase()=DAY`, reset each
  living lord's `hoursRemaining` and recover some energy.
- `outcome()`: `FREE_ADVENTURE_WIN` once Morkin destroys the Ice Crown; `FREE_WARGAME_WIN`
  once a FREE lord owns Ushgarak; `DOOMDARK_WIN` if Luxor is dead or Doomdark owns Xajorkith.

Canonical starting characters (Task 1 seeds these in `newGame()`, FREE & recruited):
**Luxor the Moonprince**, **Morkin**, **Corleth the Fey**, **Rorthron the Wise**.
Plus several independent FREE lords (`isRecruited()==false`) scattered on the map, e.g.
**Lord of the Dawn**, **Lord Blood**, **Lord Brith**, **Lord Gard**, **Lord Shimeril**.
Doomdark commands several DOOMDARK armies in the north near **Ushgarak**.
The Ice Crown starts at the **Tower of Doom** in the far north.

JUnit suite under `src/test/java/com/midnight/core/` covering movement cost/validation,
the no-night-movement rule, the day/night transition, and each victory condition.

---

## Package: `com.midnight.ai` — owned by Task 2 (depends ONLY on `com.midnight.core`)

### `interface NightResolver` — the hook `GameState.endDay` calls
```
public interface NightResolver {
    // Run Doomdark's entire NIGHT phase against the live state: move his armies,
    // resolve all combats, capture/lose strongholds. Mutate `state` via core setters.
    // Returns a human-readable report of what happened. Must NOT advance the day
    // (GameState.endDay does that afterward). Never returns null.
    NightReport resolveNight(GameState state);
}
```

### `final class NightReport`
```
public NightReport(java.util.List<BattleResult> battles, java.util.List<String> movements);
public java.util.List<BattleResult> battles();
public java.util.List<String> movements();
public String narrative();   // joined plain-English summary for the UI message area
```

### `final class BattleResult`
```
public BattleResult(Location where, Side victor, int freeLosses, int doomdarkLosses, String text);
public Location where();
public Side victor();          // null = indecisive
public int freeLosses();
public int doomdarkLosses();
public String text();
```

### `interface CombatResolver`
```
public interface CombatResolver {
    // Resolve a battle between all forces present at `where`. Apply casualties to the
    // characters/strongholds via core setters and return the outcome. Combat math MUST
    // factor terrain (defensive bonus), each lord's courage, and fatigue/energy.
    BattleResult resolveBattle(GameState state, Location where);
}
```

### `class StandardCombatResolver implements CombatResolver`
Deterministic-ish resolution (seedable). Attacker strength scales with warriors+riders,
courage, and energy; defender gets a terrain bonus (CITADEL/KEEP/TOWER/FOREST/MOUNTAINS
strong, PLAINS weak). Task 2 owns the formula and tuning.
```
public StandardCombatResolver();
public StandardCombatResolver(long seed);
```

### `class DoomdarkAI implements NightResolver`
Doomdark's night brain. Each night: advance armies toward their objectives — spread out,
hunt the player's recruited Lords, and press toward Free-held citadels (especially
Xajorkith). When a Doomdark army shares a tile with Free forces or a Free stronghold,
invoke the `CombatResolver`. Pure logic — NO Swing, NO System.out spam (return text in the
report).
```
public DoomdarkAI();
public DoomdarkAI(CombatResolver resolver);
public DoomdarkAI(CombatResolver resolver, long seed);
```

JUnit suite under `src/test/java/com/midnight/ai/` for combat math (terrain/courage/fatigue
effects) and that the AI mutates state legally (no FREE army movement, no day advance).

---

## Package: `com.midnight.ui` — owned by Task 3 (depends on `com.midnight.core` + `com.midnight.ai`)

Java Swing app. Drive everything through `GameState`; never re-implement rules.

Required panels/components:
1. **Directional compass control** — an 8-way compass (N, NE, E, SE, S, SW, W, NW).
   "Look" buttons rotate the selected lord's `facing` (call `look`); "Move" (e.g. a
   forward/move button) calls `move(selected, facing)`. Disable when it is NIGHT or the
   lord is out of hours.
2. **2.5D "Landscaping" viewpoint** — first-person panel rendered with Java 2D
   (`Graphics2D`). Look outward from `selected().location()` along `selected().facing()`;
   layer distant terrain (mountains/snow on the horizon), mid-ground (forests, henges,
   citadels), and foreground (armies/lords on adjacent tiles). Re-render on every
   look/move. Pure painting from the core map + characters.
3. **Strategic top-down map overlay** — toggleable panel painting the whole `Map` grid
   (terrain colors, strongholds, lord markers for both sides), highlighting `selected()`.
4. **Character selection menu** — list/cycle the player's lords (`playerLords()`), select
   one to control; show stamina (energy), courage, hours remaining, army size.
5. **Turn control** — an "End Day" button calling
   `state.endDay(new com.midnight.ai.DoomdarkAI())`, then showing the returned night
   `NightReport.narrative()` (the resolver report) in a message area. Show `outcome()` /
   win-loss banners.

### `class Main`
```
public static void main(String[] args);   // launches via SwingUtilities.invokeLater
```

---

## Integration contract summary
- Task 1 publishes `com.midnight.core` EXACTLY as above + JUnit tests.
- Task 2 imports `com.midnight.core` ONLY; exposes `NightResolver`, `NightReport`,
  `BattleResult`, `CombatResolver`, `StandardCombatResolver`, `DoomdarkAI`.
- Task 3 imports `com.midnight.core` + `com.midnight.ai`; provides `com.midnight.ui.Main`.
- Nobody edits another package's files. Each task pushes its branch and opens a PR into
  `whim-wd-171`, then reports back to the orchestrator via `send_prompt`.
