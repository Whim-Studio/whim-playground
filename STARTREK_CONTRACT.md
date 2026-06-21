# StarTrek: A New Beginning — Shared Interface Contract (Java 8)

Standalone Swing strategy/combat hybrid: a clone of *Star Trek: Birth of the Federation*.
A turn-based galaxy map (TBS) seamlessly toggling to a real-time 2D top-down space battle (RTS).

- App dir: `startrek/`
- Base package: `com.whim.startrek`
- **Java 8 ONLY.** No `var`, no switch expressions, no text blocks, no records, no post-Java-8 `Stream` collectors. Lambdas and standard functional interfaces are fine. **No external libraries** — only `javax.swing`, `java.awt`, `java.util`, `java.io`, `java.nio`, `java.util.concurrent`.
- No floating singletons of shared models: Tasks 2 and 3 **import and operate on** the `domain` types authored by Task 1. They MUST NOT redeclare them.

## File ownership (NO overlap between tasks)

- **Task 1 (domain/state)** owns: `startrek/src/main/java/com/whim/startrek/domain/**`
- **Task 2 (engine)** owns: `startrek/src/main/java/com/whim/startrek/engine/**` and `startrek/src/test/java/com/whim/startrek/engine/**`
- **Task 3 (ui)** owns: `startrek/src/main/java/com/whim/startrek/ui/**`
- **Main class** (`com/whim/startrek/Main.java`) is written by the orchestrator during consolidation. Do NOT create it.

Every public type below is part of the contract. Match the package, name, and signatures **verbatim**. Add fields/helpers freely, but do not change or remove anything specified here.

---

## DOMAIN (Task 1 — package `com.whim.startrek.domain`)

### Enums

```java
public enum TechType { BIOTECH, PROPULSION, WEAPON, CONSTRUCTION, ENERGY, COMPUTER }

public enum ResourceType {
    DILITHIUM(true), DEUTERIUM(true), CREDITS(true), METALS(true), OFFICERS(false);
    private final boolean tradable;
    ResourceType(boolean tradable) { this.tradable = tradable; }
    public boolean isTradable() { return tradable; } // OFFICERS are NEVER tradable
}

public enum FacilityType {
    SHIPYARD, TRADE_FACILITY, STARBASE, OUTPOST, SENSOR_ARRAY, RESEARCH_FACILITY
}

public enum EmpireStatus { PEACE, WAR, UNREST }

// Civilization level drives the starting tech-point pool (level*10 of a 50 max).
public enum Race {
    // name, civLevel(1..5), capBio, capProp, capWeap, capConst, capEnergy, capComp
    AKAALI    (1, 3,3,2,2,2,3),
    OCAMPA    (1, 4,2,2,2,3,2),
    FEDERATION(5, 7,10,6,7,8,9),
    DOMINION  (5, 10,7,10,10,6,5),
    KLINGON   (5, 6,8,10,7,7,5),
    ROMULAN   (5, 6,9,8,7,7,8);
    // Task 1 may add intermediate (level 2-4) races; these six are required.
    Race(int civLevel, int bio, int prop, int weap, int cons, int energy, int comp) { ... }
    public int getCivLevel();                 // 1..5
    public int getTechPointPool();             // civLevel * 10  (max 50)
    public int getCap(TechType t);             // per-tree maximum achievable level
}

// Map terrain. Behavior is data-driven so engine + ui read it uniformly.
public enum MapObjectType {
    // destroysAssets, hullDamageMinPct, hullDamageMaxPct
    EMPTY            (false, 0,  0),
    SOLAR_SYSTEM     (false, 0,  0),
    NEBULA           (false, 0,  0),   // blocks sensors / cloak detection
    ENERGY_STORM     (false, 5,  15),
    SUPERNOVA        (true,  0,  0),
    STABLE_WORMHOLE  (false, 0,  0),   // safe teleport between linked cells
    UNSTABLE_WORMHOLE(false, 25, 50),  // teleport + 25-50% hull damage
    BLACK_HOLE       (true,  0,  0),   // destroys any fleet entering
    SUPER_BLACK_HOLE (true,  0,  0);   // destroys fleet + may collapse neighbor cells
    public boolean destroysAssets();
    public int getHullDamageMinPct();
    public int getHullDamageMaxPct();
}
```

### Core mutable model classes (concrete, public no-arg or specified constructors, full getters/setters)

```java
public class Ship {
    public Ship(String name, String shipClass, Race owner);
    public String getName();              public String getShipClass();
    public Race getOwner();
    public int getMaxHull();   public int getHull();   public void setHull(int h);
    public int getMaxShields();public int getShields();public void setShields(int s);
    public int getMaxEnergy(); public int getEnergy(); public void setEnergy(int e);
    public boolean isCloakCapable();      public void setCloakCapable(boolean b);
    public boolean isCloaked();           public void setCloaked(boolean b);
    public int getOfficersRequired();     // ship cannot move/fight without crew
    public int getWeaponDamage();         public int getWeaponRange();
    public double getSpeed();             // RTS units/sec
    // RTS live-battle position (ignored on the TBS map)
    public double getX(); public double getY(); public void setPosition(double x, double y);
    public boolean isDestroyed();         // hull <= 0
}

public class Fleet {
    public Fleet(int id, Race owner);
    public int getId();                   public Race getOwner();
    public java.util.List<Ship> getShips();
    public void addShip(Ship s);
    public int getRow(); public int getCol();      public void setCell(int row, int col);
    public int getDestRow(); public int getDestCol(); public void setDestination(int row, int col);
    public boolean isCloaked();           // true if every cloak-capable ship is cloaked
    public int totalOfficersRequired();
    public boolean isEmpty();
}

public class StarSystem {
    public StarSystem(String name, int row, int col);
    public String getName();              public int getRow(); public int getCol();
    public Race getOwner();               public void setOwner(Race r); // null = independent
    public long getPopulation();          public void setPopulation(long p);
    public int getFacility(FacilityType t);            // count, >=0
    public void setFacility(FacilityType t, int count);
    public long getStockpile(ResourceType r);          public void setStockpile(ResourceType r, long amt);
    public long getProduction(ResourceType r);         // per-turn output
    public void setProduction(ResourceType r, long amt);
    public boolean isBorgControlled();    public void setBorgControlled(boolean b);
}

public class GridCell {
    public GridCell(int row, int col, MapObjectType type);
    public int getRow(); public int getCol();
    public MapObjectType getType();       public void setType(MapObjectType t);
    public StarSystem getSystem();        public void setSystem(StarSystem s); // null if none
    public java.util.List<Fleet> getFleets();          // multiple fleets per cell allowed
    public int getWormholeLinkRow();      public int getWormholeLinkCol(); // -1 if none
    public void setWormholeLink(int row, int col);
}

public class GalaxyMap {
    public GalaxyMap(int rows, int cols);
    public int getRows(); public int getCols();
    public GridCell getCell(int row, int col);         // bounds-checked, null if OOB
    public boolean inBounds(int row, int col);
    public java.util.List<Fleet> allFleets();
    public java.util.List<StarSystem> allSystems();
}

public class Empire {
    public Empire(Race race);
    public Race getRace();
    public EmpireStatus getStatus();      public void setStatus(EmpireStatus s);
    public long getTreasury(ResourceType r);           public void setTreasury(ResourceType r, long amt);
    public void addTreasury(ResourceType r, long delta);
    public int getTechLevel(TechType t);  public void setTechLevel(TechType t, int level); // 0..Race cap
    public java.util.List<Fleet> getFleets();
    public java.util.List<StarSystem> getSystems();
    public boolean isPlayer();            public void setPlayer(boolean b);
}
```

### Game state + turn loop (Task 1)

```java
public enum TurnPhase { INCOME, RESEARCH, MOVEMENT, COMBAT, BORG, END }

public class GameState {
    public GameState(GalaxyMap map, java.util.List<Empire> empires);
    public GalaxyMap getMap();
    public java.util.List<Empire> getEmpires();
    public Empire getPlayerEmpire();
    public int getTurnNumber();
    public TurnPhase getPhase();          public void setPhase(TurnPhase p);
    public BorgState getBorgState();      // never null
    public boolean isBattleActive();      public void setBattleActive(boolean b);
}

// Persistent Borg threat. Engine mutates it; it never auto-resolves until eradicated.
public class BorgState {
    public boolean isActive();            public void setActive(boolean b);
    public int getCubeCount();            public void setCubeCount(int n);
    public java.util.List<int[]> getControlledCells(); // {row,col} pairs
    public int getIntensity();            public void setIntensity(int n); // scales every turn while active
}

// TurnManager runs the phase state machine. The engine (Task 2) is injected so domain
// has NO compile dependency on engine — see GameServices below.
public class TurnManager {
    public TurnManager(GameState state, GameServices services);
    public void advanceTurn();            // runs INCOME->RESEARCH->...->END, increments turn
    public TurnPhase getCurrentPhase();
}

// Bridge interface so TurnManager can drive Task 2 logic without importing concrete engine classes.
// Task 2 supplies the implementation; Task 1 only depends on this interface.
public interface GameServices {
    void applyIncome(GameState s);
    void applyResearch(GameState s);
    void resolveMovement(GameState s);    // fleet nav, wormhole/hazard effects, cloak updates
    void resolveCombat(GameState s);      // auto-resolve TBS encounters not opened as live battles
    void stepBorg(GameState s);
}

// Factory that builds a ready-to-play galaxy for a chosen player race + difficulty.
public class GameFactory {
    public static GameState newGame(Race playerRace, int rows, int cols);
}
```

Task 1 also provides a `NoOpGameServices implements GameServices` (empty bodies) so the domain + UI can compile and run standalone before Task 2 lands. The orchestrator swaps in Task 2's real `EngineServices` during consolidation.

---

## ENGINE (Task 2 — package `com.whim.startrek.engine`)

Operates purely on `com.whim.startrek.domain` types. No Swing imports.

```java
// Dynamic supply/demand. Base price per 1000 units =
//   totalGalacticSupply(resource) / (totalGalacticCredits * 1000).
// Interest multiplier scales with empire status (WAR/UNREST raise it) and is REDUCED by
// the empire's TRADE_FACILITY count. OFFICERS are non-tradable: buy/sell must reject them.
public class EconomyEngine {
    public EconomyEngine();
    public double basePricePer1000(ResourceType r, GameState s);
    public double interestMultiplier(Empire e);
    public boolean buy(Empire e, ResourceType r, long units, GameState s);  // false if non-tradable/insufficient
    public boolean sell(Empire e, ResourceType r, long units, GameState s);
}

// Cloak/scan + navigation AI.
public class FleetAI {
    public FleetAI();
    public boolean isDetected(Fleet target, Empire observer, GameState s); // sensor-array vs cloak + nebula
    public void stepCloaking(Empire e, GameState s);                       // Romulan/Klingon cloak logic
    public int[] nextStepToward(Fleet f, int destRow, int destCol, GameState s); // {row,col}
}

// Persistent Borg plague. Expands, conquers independent systems, builds cubes, scales until eradicated.
public class BorgEngine {
    public BorgEngine();
    public void step(GameState s);          // mutates BorgState; intensity grows each active turn
    public boolean isEradicated(GameState s);
}

// Real-time 2D top-down battle. UI drives stepping on a timer; engine owns the math (vectors,
// projectile tracking, hit-box collisions, shield-then-hull damage).
public class BattleSimulator {
    public BattleSimulator(java.util.List<Ship> sideA, java.util.List<Ship> sideB, double arenaWidth, double arenaHeight);
    public void step(double dtSeconds);     // advance positions, weapons, collisions
    public java.util.List<Projectile> getProjectiles();
    public java.util.List<Ship> getShips(); // both sides, for rendering
    public boolean isFinished();
    public Race getWinner();                // null until finished
}

public class Projectile {
    public double getX(); public double getY();
    public double getVx(); public double getVy();
    public boolean isTorpedo();             // false = phaser beam
    public Race getOwner();
}

// Implements the domain bridge; wired into TurnManager during consolidation.
public class EngineServices implements com.whim.startrek.domain.GameServices { public EngineServices(); }
```

Task 2 ships a `main`-runnable `EngineSelfTest` under `engine/**` (test dir) printing PASS/FAIL for the price formula, the officers-non-tradable guard, Borg scaling, and one battle resolution. No external test deps required to build the app.

---

## UI (Task 3 — package `com.whim.startrek.ui`)

Reads `domain` state and calls `engine` services. Pure `javax.swing`/`java.awt`/`Graphics2D` — no chart/game libs.

```java
// Built by orchestrator's Main: new MainFrame(gameState, services...).setVisible(true) on the EDT.
public class MainFrame extends javax.swing.JFrame {
    // Constructor wiring is finalized with the orchestrator; assume it receives a ready GameState
    // plus EconomyEngine, FleetAI, BorgEngine, and a BattleSimulator factory. Provide a public
    // no-arg constructor too that builds a demo game via GameFactory.newGame(...) so the app runs
    // standalone before integration.
    public MainFrame();
    public void showGalaxyView();
    public void showBattleView(BattleSimulator battle);
}
```

- **GalaxyPanel** (`JPanel`): `Graphics2D` grid of the `GalaxyMap` — draws terrain (wormholes, black holes, nebulas, supernovas, energy storms), star systems, and fleet stacks (multiple per cell). A lightweight minimap derived from the map array. Mouse: click a fleet then a destination to set `setDestination`; click a system to open a build menu (FacilityType) and a trade board (EconomyEngine buy/sell, OFFICERS hidden/disabled).
- **BattlePanel** (`JPanel`): top-down arena rendering `BattleSimulator` ships (directional sprites), phaser beams, particle-style torpedo arcs, and per-ship shield/hull bars. A `javax.swing.Timer` calls `battle.step(dt)` then `repaint()`. Click-to-target overlay.
- **MainFrame** toggles between the two views (CardLayout) and exposes an "End Turn" control that calls `TurnManager.advanceTurn()`.

The orchestrator finalizes the exact `MainFrame`/`Main` wiring during consolidation; keep UI construction parameterizable rather than hard-coding a single constructor shape.
