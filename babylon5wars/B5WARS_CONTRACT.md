# Babylon 5 Wars — Shared Interface Contract (Java 8)

Standalone desktop adaptation of the AOG miniatures wargame **Babylon 5 Wars**.
Pure **Java 8 + Swing**, only external dep allowed: **Gson** (data) and **JUnit 4** (tests).

- App dir: `babylon5wars/`
- Base package: `com.whim.b5wars`
- **Java 8 ONLY.** No `var`, no switch expressions, no text blocks, no records, no
  post-Java-8 APIs. Lambdas + standard functional interfaces are fine.
- Rule authority: `babylon5wars/RULES_REFERENCE.md` + `ARCHITECTURE.md`. Every
  `[APPROXIMATED]` value is unverified vs. the real rulebook and lives in data/JSON, never
  hardcoded in engine logic. Mark such constants with an `APPROXIMATED` comment.
- **No model leaks:** the engine/data tasks **import and operate on** the `model` types
  defined below. They MUST NOT redeclare them. Match package, name, and signatures
  **verbatim**; you may add fields/helpers but never change/remove what is specified.

## File ownership (NO overlap)

- **Task D (model + data + resources)** owns:
  - `babylon5wars/src/main/java/com/whim/b5wars/model/**`
  - `babylon5wars/src/main/java/com/whim/b5wars/data/**`
  - `babylon5wars/src/main/resources/**` (JSON)
  - `babylon5wars/pom.xml`
- **Task E (engine + tests)** owns:
  - `babylon5wars/src/main/java/com/whim/b5wars/engine/**`
  - `babylon5wars/src/test/java/com/whim/b5wars/engine/**`
- `com/whim/b5wars/Main.java` and `ui/**` are authored later by the orchestrator/UI task.
  Do NOT create them now.

---

## MODEL (Task D — package `com.whim.b5wars.model`)

### Enums
```java
public enum Side { A, B }

public enum Race { EARTH_ALLIANCE, NARN_REGIME }   // extend later

public enum DefenseType { ARMOR, SHIELD }          // ARMOR = non-regenerating (default)

public enum Section { FORE, AFT, PORT, STARBOARD, PRIMARY }

public enum WeaponTrait { ARMOR_PIERCING, RAKING, INTERCEPTOR, GUIDED, BALLISTIC }

// 6 hexsides clockwise from front. index() 0..5.
public enum Facing {
    F, FR, BR, B, BL, FL;
    public int index();                 // ordinal 0..5
    public Facing rotate(int steps);    // +clockwise / -ccw, wraps mod 6
    public Facing opposite();           // rotate(3)
}
```

### Hex — axial coordinates (immutable)
```java
public final class Hex {
    public Hex(int q, int r);
    public int getQ();
    public int getR();
    public int distance(Hex other);         // axial hex distance
    public Hex neighbor(Facing dir);        // adjacent hex in a facing direction
    public boolean equals(Object o);        // value equality on (q,r)
    public int hashCode();
    public String toString();               // "(q,r)"
}
```

### Dice — seedable RNG (single source of randomness)
```java
public final class Dice {
    public Dice(long seed);
    public int d(int sides);                // 1..sides
    public int d20();                       // 1..20
    public int roll(int count, int sides, int plus);   // sum of count dN + plus
}
```

### WeaponArc — the set of Facings a mount can fire into
```java
public final class WeaponArc {
    public WeaponArc(java.util.Set<Facing> facings);
    public boolean contains(Facing f);
    public java.util.Set<Facing> facings();
    public static WeaponArc of(Facing... f);
    public static WeaponArc all();          // all 6
    public static WeaponArc forward();      // {FL,F,FR}
}
```

### DamageProfile — dice spec for a weapon's damage
```java
public final class DamageProfile {
    public DamageProfile(int count, int sides, int plus);
    public int getCount();
    public int getSides();
    public int getPlus();
    public int roll(Dice dice);             // count dSides + plus
}
```

### Weapon — printed weapon definition (immutable)
```java
public final class Weapon {
    public Weapon(String name, String type, WeaponArc arc, int[] rangeBrackets,
                  int baseToHit, DamageProfile damage, int reloadTurns,
                  java.util.Set<WeaponTrait> traits);
    public String getName();
    public String getType();
    public WeaponArc getArc();
    public int[] getRangeBrackets();        // ascending max-range per accuracy bracket
    public int getBaseToHit();              // d20 target before modifiers
    public DamageProfile getDamage();
    public int getReloadTurns();            // 0 = every turn
    public java.util.Set<WeaponTrait> getTraits();
    public boolean has(WeaponTrait t);
}
```

### Special — a named special system (interceptors, jump engine, hangar, ...)
```java
public final class Special {
    public Special(String id, String name, int value);   // value = rating if applicable
    public String getId();
    public String getName();
    public int getValue();
}
```

### ShipClass — immutable printed template
```java
public final class ShipClass {
    public ShipClass(String id, String name, Race race, int points,
                     int maxSpeed, int turnMode, int thrust, int power,
                     int initiativeBonus, int crewQuality,
                     int sensorRating, int ewRating,
                     java.util.Map<Facing,Integer> armor,
                     java.util.Map<Section,Integer> structure,
                     DefenseType defenseType,
                     java.util.List<Weapon> weapons,
                     java.util.List<Special> specials);
    public String getId();
    public String getName();
    public Race getRace();
    public int getPoints();
    public int getMaxSpeed();
    public int getTurnMode();
    public int getThrust();
    public int getPower();
    public int getInitiativeBonus();
    public int getCrewQuality();
    public int getSensorRating();
    public int getEwRating();
    public java.util.Map<Facing,Integer> getArmor();       // per facing
    public java.util.Map<Section,Integer> getStructure();  // per section (max boxes)
    public DefenseType getDefenseType();
    public java.util.List<Weapon> getWeapons();
    public java.util.List<Special> getSpecials();
}
```

### Ship — mutable in-play instance built from a ShipClass
```java
public final class Ship {
    public Ship(ShipClass type, Side side, Hex pos, Facing facing, int speed);
    public ShipClass getType();
    public Side getSide();

    public Hex getPos();            public void setPos(Hex h);
    public Facing getFacing();      public void setFacing(Facing f);
    public int getSpeed();          public void setSpeed(int s);

    // hexes travelled straight since the last facing change (for turn-mode checks)
    public int getStraightHexes();  public void setStraightHexes(int n);

    // current defense layer per facing (starts == class armor; SHIELD regenerates each turn)
    public java.util.Map<Facing,Integer> getArmor();
    // current structure remaining per section (starts == class structure)
    public java.util.Map<Section,Integer> getStructure();

    // per-turn allocations (written by engine/UI during POWER/EW phases)
    public int getEwOffensive();    public void setEwOffensive(int n);
    public int getEwDefensive();    public void setEwDefensive(int n);
    public int getThrustAvailable();public void setThrustAvailable(int n);

    // weapon i cannot fire until this turn index; engine manages
    public int getReloadReadyTurn(int weaponIndex);
    public void setReloadReadyTurn(int weaponIndex, int turn);

    public boolean isDestroyed();   public void setDestroyed(boolean b);
    public boolean isCrippled();    public void setCrippled(boolean b);

    public int totalStructureRemaining();   // sum of current structure across sections
}
```

### Faction, Scenario, VictoryCondition, Placement
```java
public final class Faction {
    public Faction(Race race, java.util.List<ShipClass> shipClasses);
    public Race getRace();
    public java.util.List<ShipClass> getShipClasses();
    public ShipClass byId(String id);     // null if absent
}

public enum VictoryCondition { DESTROY_OR_CRIPPLE_ENEMY, MOST_STRUCTURE_AFTER_TURNS }

public final class Placement {
    public Placement(String shipClassId, Side side, Hex pos, Facing facing, int speed);
    public String getShipClassId();
    public Side getSide();
    public Hex getPos();
    public Facing getFacing();
    public int getSpeed();
}

public final class Scenario {
    public Scenario(String name, int mapWidth, int mapHeight,
                    java.util.List<Placement> placements,
                    VictoryCondition victory, int turnLimit);
    public String getName();
    public int getMapWidth();
    public int getMapHeight();
    public java.util.List<Placement> getPlacements();
    public VictoryCondition getVictory();
    public int getTurnLimit();
}
```

---

## DATA (Task D — package `com.whim.b5wars.data`)
```java
public final class DataLoader {
    // Loads all faction JSON on the classpath under /factions/*.json
    public static java.util.List<Faction> loadFactions();
    public static Faction loadFaction(String resourcePath);      // e.g. "/factions/earth-alliance.json"
    public static Scenario loadScenario(String resourcePath);    // e.g. "/scenarios/border-skirmish.json"
    // impulse cadence: map speed -> boolean[impulseCount] (true = enters a hex on that impulse)
    public static java.util.Map<Integer,boolean[]> loadImpulseCadence();  // "/tables/impulse-cadence.json"
    public static java.util.List<CriticalEntry> loadCriticalTable();      // "/tables/critical-hits.json"
}

// Critical-hit table row (data-driven). result strings are engine-interpreted.
public final class CriticalEntry {
    public CriticalEntry(int rollMin, int rollMax, String effect);
    public int getRollMin();
    public int getRollMax();
    public String getEffect();   // e.g. "REACTOR", "ENGINE", "WEAPON", "SENSOR", "CREW", "NONE"
}
```
Resources Task D must provide (all numeric values `APPROXIMATED, unverified`):
- `/factions/earth-alliance.json` — ≥2 ship classes incl. **Hyperion** heavy cruiser.
- `/factions/narn-regime.json` — ≥2 ship classes incl. **G'Quan** heavy cruiser.
- `/scenarios/border-skirmish.json` — the 1-on-1 duel (Hyperion vs G'Quan), opposite edges,
  facing inward, speed ~6, `DESTROY_OR_CRIPPLE_ENEMY`, turnLimit ~12.
- `/tables/impulse-cadence.json` — impulseCount = 8; per-speed boolean cadence.
- `/tables/critical-hits.json` — d20 ranges → effect strings.
JSON must be plain (Gson reflection over these field names); include a `"_note"` field marking
approximated blocks.

---

## ENGINE (Task E — package `com.whim.b5wars.engine`)
Task E imports the `model` types above and implements deterministic rules. Suggested API
(you may refine internals, keep these entry points):
```java
public enum TurnPhase { INITIATIVE, POWER, EW, IMPULSE, END_OF_TURN }

public final class GameState {
    public GameState(Scenario scenario, java.util.List<Faction> factions, long seed);
    public java.util.List<Ship> getShips();
    public int getTurn();
    public int getImpulse();
    public TurnPhase getPhase();
    public Dice getDice();
    public Side getInitiativeWinner();
    public boolean isOver();
    public Side getWinner();               // null until over
}

public final class GameEvent {
    public GameEvent(String type, String message);
    public String getType();               // "MOVE","FIRE","HIT","MISS","CRIT","PHASE","VICTORY"
    public String getMessage();
}

public final class TurnManager {
    public TurnManager(GameState state);
    public java.util.List<GameEvent> advancePhase();   // drive the FSM one phase
    public void rollInitiative();
    public java.util.List<GameEvent> runImpulseLoop();  // execute all impulses this turn
    public void endTurn();
}

public final class MovementEngine {
    // returns true if legal; enforces turn-mode via Ship.getStraightHexes()
    public boolean moveForward(Ship s);                 // enter neighbor(facing), speed permitting
    public boolean turn(Ship s, int steps);             // ±1 hexside if turn-mode satisfied & thrust
    public boolean sideslip(Ship s, int steps);         // lateral, thrust cost
    public void accelerate(Ship s, int delta);          // change speed within maxSpeed & thrust
    public void drift(Ship s);                          // powerless: advance along vector, no turn
}

public final class CombatEngine {
    public CombatEngine(Dice dice, java.util.List<CriticalEntry> critTable);
    public boolean inArcAndRange(Ship attacker, int weaponIndex, Ship target);
    public int toHitTarget(Ship attacker, int weaponIndex, Ship target);   // modified d20 target
    public java.util.List<GameEvent> fire(Ship attacker, int weaponIndex, Ship target, int currentTurn);
    // applies damage: defense layer (armor/shield) of hit facing -> structure -> systems/crits
}
```
Tests (Task E owns `src/test/java/com/whim/b5wars/engine/**`): cover turn-mode enforcement,
thrust/accel limits, drift, arc/range gating, the to-hit modifier stack, armor→structure flow,
ARMOR_PIERCING and INTERCEPTOR traits, and crit application — all with a seeded `Dice`.

---

## Integration
Orchestrator merges Task D then Task E, compiles together (`mvn -q compile test`), and reviews
the combined diff before the UI phase. Report results/blockers to the orchestrator task via
`send_prompt`, not task comments.
