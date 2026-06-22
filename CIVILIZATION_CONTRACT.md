# Civilization (1991) — Shared Interface Contract (Java 8)

Standalone Java 8 Swing adaptation of Sid Meier's **Civilization 1** (1991): a turn-based 4X
on a **square-tiled** world map, with city management, unit combat, a cascading tech tree, and
governments. **Square grid only — never hexes.** Settlers are the only unit that can build
roads, irrigation, mines, and railroads.

- App dir: `civ/`
- Base package: `com.whim.civ`
- **Java 8 ONLY.** No `var`, no switch expressions, no text blocks, no records, no post-Java-8
  `Stream` collectors (no `Collectors.toUnmodifiableList`, no `Stream.toList`, no `.mapMulti`).
  Lambdas, method refs, and the standard Java 8 functional interfaces / `Collectors.toList`
  etc. are fine. **No external libraries** except JUnit 4 (test scope only) — use only
  `javax.swing`, `java.awt`, `java.util`, `java.io`, `java.nio`, `java.util.concurrent`.
- **No model leaks / no redeclaration.** Tasks 2 and 3 **import and operate on** the `domain`
  types authored by Task 1. They MUST NOT redeclare or fork them. Communicate only through the
  public types named in this contract.

## File ownership (NO overlap between tasks)

- **Task 1 (domain/state)** owns: `civ/src/main/java/com/whim/civ/domain/**`
- **Task 2 (engine)** owns: `civ/src/main/java/com/whim/civ/engine/**` and
  `civ/src/test/java/com/whim/civ/engine/**`
- **Task 3 (ui)** owns: `civ/src/main/java/com/whim/civ/ui/**`
- **Main class** (`com/whim/civ/Main.java`) is written by the orchestrator during
  consolidation. Do NOT create it.

Every public type below is part of the contract. Match the package, name, and signatures
**verbatim**. You may add extra fields, helpers, overloads, and classes within your own
package, but do not change or remove anything specified here.

---

## DOMAIN (Task 1 — package `com.whim.civ.domain`)

### Enums

```java
// Square-grid terrain. Base yields are the unworked tile yields BEFORE improvements,
// government penalties, or specialist bonuses. defenseBonusPct is added to a defender.
public enum Terrain {
    // food, shields, trade, moveCost, defenseBonusPct, canIrrigate, canMine, canFarmRoad
    OCEAN     (1, 0, 2, 1, 0,   false, false, false),
    GRASSLAND (2, 1, 0, 1, 0,   true,  false, true),
    PLAINS    (1, 1, 1, 1, 0,   true,  false, true),
    FOREST    (1, 2, 0, 2, 25,  false, false, true),
    HILLS     (1, 0, 0, 2, 100, true,  true,  true),
    MOUNTAINS (0, 1, 0, 3, 200, false, true,  true),
    DESERT    (0, 1, 0, 1, 0,   true,  true,  true),
    TUNDRA    (1, 0, 0, 1, 0,   false, false, true),
    ARCTIC    (0, 0, 0, 2, 0,   false, false, false),
    SWAMP     (1, 0, 0, 2, 0,   false, false, true),
    JUNGLE    (1, 0, 0, 2, 0,   false, false, true);
    Terrain(int food, int shields, int trade, int moveCost, int defenseBonusPct,
            boolean canIrrigate, boolean canMine, boolean canFarmRoad) { ... }
    public int getFood();
    public int getShields();
    public int getTrade();
    public int getMoveCost();              // movement points to enter (square grid)
    public int getDefenseBonusPct();       // terrain defense bonus, e.g. 100 == +100%
    public boolean canIrrigate();
    public boolean canMine();
    public boolean canBuildRoad();         // also gates railroad
}

// Tile improvements a Settler can build. ROAD/RAILROAD add trade & cut move cost.
public enum Improvement { NONE, ROAD, IRRIGATION, MINE, RAILROAD }

// Branches of trade output within a city.
public enum TradeChannel { TAX, SCIENCE, LUXURY }

// The unit roster. Required units must exist; Task 1 may add more.
// attack/defense/movement are nominal strength; cost is shield cost to build.
public enum UnitType {
    // attack, defense, movement, cost, isSettler, canFound, prereqTech (null == none)
    SETTLERS (0, 1, 1, 40, true,  true,  null),
    MILITIA  (1, 1, 1, 10, false, false, null),
    PHALANX  (1, 2, 1, 20, false, false, TechType.BRONZE_WORKING),
    LEGION   (4, 2, 1, 20, false, false, TechType.IRON_WORKING),
    CHARIOT  (4, 1, 2, 30, false, false, TechType.THE_WHEEL),
    CATAPULT (6, 1, 1, 40, false, false, TechType.MATHEMATICS),
    DIPLOMAT (0, 0, 2, 30, false, false, TechType.WRITING),
    CAVALRY  (2, 1, 2, 20, false, false, TechType.HORSEBACK_RIDING),
    MUSKETEER(3, 3, 1, 30, false, false, TechType.GUNPOWDER);
    UnitType(int attack, int defense, int movement, int cost,
             boolean isSettler, boolean canFound, TechType prereq) { ... }
    public int getAttack();
    public int getDefense();
    public int getMovement();              // movement points per turn
    public int getCost();                  // shields to produce
    public boolean isSettler();            // can terraform / build roads
    public boolean canFound();             // can found a city
    public TechType getPrereq();           // null == always available
}

// Cascading tech tree. Each tech lists 0..2 prerequisite techs. Reaching SPACE_FLIGHT
// (and beyond) is the long-term goal; this subset must form a valid acyclic graph.
public enum TechType {
    ALPHABET,
    BRONZE_WORKING,
    POTTERY,
    CEREMONIAL_BURIAL,
    HORSEBACK_RIDING,
    MASONRY,
    WRITING(ALPHABET),
    CODE_OF_LAWS(ALPHABET),
    IRON_WORKING(BRONZE_WORKING),
    THE_WHEEL,
    MATHEMATICS(ALPHABET, MASONRY),
    CURRENCY(BRONZE_WORKING),
    MONARCHY(CEREMONIAL_BURIAL, CODE_OF_LAWS),
    THE_REPUBLIC(CODE_OF_LAWS, WRITING),
    LITERACY(CODE_OF_LAWS, WRITING),
    TRADE(CURRENCY, WRITING),
    GUNPOWDER(IRON_WORKING, INVENTION),
    INVENTION(WRITING, ENGINEERING),
    ENGINEERING(THE_WHEEL, CONSTRUCTION),
    CONSTRUCTION(CURRENCY, MASONRY),
    UNIVERSITY(MATHEMATICS, LITERACY),
    DEMOCRACY(LITERACY, INVENTION),
    SPACE_FLIGHT(COMPUTERS, UNIVERSITY),
    COMPUTERS(MATHEMATICS, UNIVERSITY);
    // The constructor takes a varargs of prerequisite TechType.
    TechType(TechType... prereqs) { ... }
    public java.util.List<TechType> getPrereqs();  // unmodifiable, never null
    public int getBaseCost();                       // research beakers; deterministic, >0
}

// Governments affect corruption, trade, and military unrest. Values are data-driven.
public enum Government {
    // maxTradePerTile, corruptionPct, martialLawUnits, tradeBonusEnabled, prereqTech
    ANARCHY  (1, 75, 0, false, null),
    DESPOTISM(2, 50, 3, false, null),   // tiles producing 3+ of a yield lose 1 (despotism penalty)
    MONARCHY (3, 30, 3, false, TechType.MONARCHY),
    COMMUNISM(3, 0,  3, false, TechType.COMMUNISM_TECH_PLACEHOLDER),
    REPUBLIC (5, 20, 0, true,  TechType.THE_REPUBLIC),  // +1 trade on tiles already making trade
    DEMOCRACY(5, 0,  0, true,  TechType.DEMOCRACY);
    Government(int maxTradePerTile, int corruptionPct, int martialLawUnits,
              boolean tradeBonusEnabled, TechType prereq) { ... }
    public int getCorruptionPct();
    public int getMartialLawUnits();       // # of military units that quell unhappiness
    public boolean hasTradeBonus();        // Republic/Democracy +1 trade on trade tiles
    public boolean appliesDespotismPenalty();   // true only for DESPOTISM/ANARCHY
    public TechType getPrereq();
}
// NOTE: COMMUNISM uses TechType.COMMUNISM if you prefer; if you keep COMMUNISM out of the
// TechType subset above, give COMMUNISM government a null prereq instead. Do NOT invent a
// fake tech enum constant — pick one and document it in a class-level comment.

// City improvements and Wonders. isWonder marks a one-per-world Wonder.
public enum Building {
    // cost(shields), upkeep(gold/turn), isWonder, prereqTech
    BARRACKS    (40, 1, false, null),
    GRANARY     (60, 1, false, TechType.POTTERY),
    TEMPLE      (40, 1, false, TechType.CEREMONIAL_BURIAL),
    MARKETPLACE (80, 1, false, TechType.CURRENCY),
    LIBRARY     (80, 1, false, TechType.WRITING),
    CITY_WALLS  (60, 1, false, TechType.MASONRY),
    AQUEDUCT    (80, 2, false, TechType.CONSTRUCTION),
    UNIVERSITY_B(160,3, false, TechType.UNIVERSITY),
    PYRAMIDS    (200,0, true,  TechType.MASONRY),
    GREAT_LIBRARY(300,0,true,  TechType.LITERACY),
    HANGING_GARDENS(200,0,true,TechType.POTTERY);
    Building(int cost, int upkeep, boolean isWonder, TechType prereq) { ... }
    public int getCost();
    public int getUpkeep();
    public boolean isWonder();
    public TechType getPrereq();
}
```

### Core mutable model classes (concrete; full getters/setters)

```java
public final class Tile {
    public Tile(Terrain terrain);
    public Terrain getTerrain();
    public void setTerrain(Terrain t);
    public Improvement getImprovement();   // default NONE
    public void setImprovement(Improvement i);
    public boolean hasRoad();              // ROAD or RAILROAD present
    public boolean hasGoodyHut();
    public void setGoodyHut(boolean b);
    public int getOwnerCivId();            // -1 if unowned
    public void setOwnerCivId(int id);
    // Effective yields AFTER improvements (irrigation +food, mine +shields, road +trade)
    // but BEFORE government effects. The ENGINE applies government/despotism/corruption.
    public int yieldFood();
    public int yieldShields();
    public int yieldTrade();
}

public final class GameMap {
    public GameMap(int width, int height);  // square grid, x in [0,width), y in [0,height)
    public int getWidth();
    public int getHeight();
    public Tile getTile(int x, int y);      // throws if out of bounds
    public boolean inBounds(int x, int y);
    // The 21-tile "city work radius" (the fat cross) around (cx,cy), in-bounds only,
    // INCLUDING the center tile. Order is unspecified but stable.
    public java.util.List<int[]> cityWorkTiles(int cx, int cy);  // each int[]{x,y}
    // 8-neighborhood (square adjacency) used for movement & combat adjacency.
    public java.util.List<int[]> neighbors(int x, int y);
}

public final class Unit {
    public Unit(UnitType type, int ownerCivId, int x, int y);
    public UnitType getType();
    public int getOwnerCivId();
    public int getX();
    public int getY();
    public void setPosition(int x, int y);
    public int getMovesLeft();             // reset each turn to type.getMovement()
    public void setMovesLeft(int m);
    public int getHitPoints();             // see ENGINE combat model; start = maxHitPoints()
    public void setHitPoints(int hp);
    public int maxHitPoints();             // 10 for normal, 15 for veteran
    public boolean isVeteran();
    public void setVeteran(boolean v);
    public boolean isFortified();
    public void setFortified(boolean f);
    public boolean isAlive();              // hitPoints > 0
}

public final class City {
    public City(int ownerCivId, String name, int x, int y);
    public int getOwnerCivId();
    public String getName();
    public int getX();
    public int getY();
    public int getPopulation();            // # of citizens (size)
    public void setPopulation(int p);
    public int getFoodStore();
    public void setFoodStore(int f);
    public int getShieldStore();
    public void setShieldStore(int s);
    public java.util.List<Building> getBuildings();   // completed buildings/wonders
    // Current production order: exactly one of these is non-null at a time.
    public UnitType getProducingUnit();
    public void setProducingUnit(UnitType u);
    public Building getProducingBuilding();
    public void setProducingBuilding(Building b);
    public int getFoodBoxSize();           // food needed to grow: (pop+1)*10
    public boolean isInDisorder();         // set by ENGINE when unhappy >= content
    public void setInDisorder(boolean d);
}

public final class Civilization {
    public Civilization(int id, String name, boolean human);
    public int getId();
    public String getName();
    public boolean isHuman();
    public Government getGovernment();
    public void setGovernment(Government g);
    public java.util.Set<TechType> getKnownTechs();   // mutable set
    public boolean knows(TechType t);
    public TechType getResearching();
    public void setResearching(TechType t);
    public int getResearchBeakers();       // accumulated toward getResearching()
    public void setResearchBeakers(int b);
    public int getTreasury();              // gold
    public void setTreasury(int g);
    // Tax/science/luxury rates as percentages summing to 100 (each a multiple of 10).
    public int getTaxRate();
    public int getScienceRate();
    public int getLuxuryRate();
    public void setRates(int tax, int science, int luxury);   // must sum to 100
    public boolean isAlive();
    public void setAlive(boolean a);
}

public final class GameState {
    public GameState(GameMap map);
    public GameMap getMap();
    public java.util.List<Civilization> getCivilizations();
    public java.util.List<City> getCities();
    public java.util.List<Unit> getUnits();
    public int getActiveCivIndex();        // index into getCivilizations()
    public void setActiveCivIndex(int i);
    public int getYear();                  // negative == B.C.; starts at -4000
    public void setYear(int y);
    public int getTurnNumber();            // starts at 1
    public void setTurnNumber(int n);
    // convenience lookups
    public java.util.List<Unit> unitsAt(int x, int y);
    public City cityAt(int x, int y);      // null if none
    public java.util.List<City> citiesOf(int civId);
    public java.util.List<Unit> unitsOf(int civId);
    public Civilization civById(int id);
}
```

### Turn loop & services

```java
// Phases of one civilization's turn, in order. The TurnManager drives the sequence.
public enum TurnPhase { UPKEEP, MOVEMENT, PRODUCTION, RESEARCH, END }

// Year progression rule (Civ1-style): the TurnManager advances the year each full
// round of all civs. Implement advanceYear() with these breakpoints:
//   year < -1000 : +50 per round; -1000..-1 : +25; 1..1000 : +20; 1000..1500 : +10;
//   1500..1750 : +5; 1750..1850 : +2; year >= 1850 : +1.  Year 0 is skipped (-1 -> +1).
public final class TurnManager {
    public TurnManager(GameState state, EngineServices services);
    public TurnPhase getPhase();
    public void beginTurn();               // UPKEEP for active civ
    public void endTurn();                 // runs remaining phases, advances active civ
    public void advanceYear();             // applies the breakpoints above
    public boolean isHumanTurn();
}

// The bridge interface the domain calls into for engine behavior, so domain never
// depends on the engine package. Task 2 implements this; Task 1 declares it and
// ships a NoOpEngineServices for standalone/unit testing.
public interface EngineServices {
    void runUpkeep(GameState state, Civilization civ);        // food/upkeep/disorder
    void runProduction(GameState state, Civilization civ);    // shields -> units/buildings
    void runResearch(GameState state, Civilization civ);      // beakers -> new tech
    void runAI(GameState state, Civilization civ);            // no-op for human civ
}

// Ships in domain so the game can run without the engine. Every method is a safe no-op.
public final class NoOpEngineServices implements EngineServices { ... }

// Factory producing a ready-to-play GameState (map, civs, starting settlers/militia).
public final class GameFactory {
    public static GameState newStandardGame(int width, int height, int numCivs, long seed);
}
```

---

## ENGINE (Task 2 — package `com.whim.civ.engine`)

Task 2 imports `com.whim.civ.domain.*` and implements the simulation. It MUST provide a
class implementing `EngineServices` and wire the sub-engines below.

```java
public final class GameEngine implements com.whim.civ.domain.EngineServices {
    public GameEngine();
    // EngineServices methods delegate to the sub-engines below.
}

// --- Combat (Civil-1-style HP/firepower attrition) ---
public final class CombatResolver {
    public CombatResolver(java.util.Random rng);
    // Effective attack/defense strength used by the duel:
    //   attacker = attackUnit.getType().getAttack() * (isVeteran ? 1.5 : 1.0)
    //   defender = defendUnit.getType().getDefense() * (isVeteran ? 1.5 : 1.0)
    //              * (1 + terrainDefenseBonusPct/100)
    //              * (isFortified ? 1.5 : 1.0)
    //              * (cityWalls present ? 3.0 : 1.0)
    public double attackStrength(Unit attacker);
    public double defenseStrength(Unit defender, Terrain terrain,
                                  boolean fortified, boolean cityWalls);
    // Round-based duel: each round, attacker wins with probability
    //   attackS / (attackS + defenseS); loser takes 1 HP. Repeat until one reaches 0 HP.
    // Returns true iff the attacker is the survivor. Mutates both units' HP.
    public boolean resolveCombat(Unit attacker, Unit defender, Terrain terrain,
                                 boolean fortified, boolean cityWalls);
}

// --- Economy: city food/shields/trade, growth, corruption, science split ---
public final class EconomyEngine {
    public EconomyEngine();
    // Sum of worked-tile yields over the city radius (uses up to getPopulation() tiles,
    // always counting the center tile), AFTER improvements and government effects
    // (despotism penalty, republic/democracy trade bonus, corruption on trade).
    public int computeFood(GameState s, City c);
    public int computeShields(GameState s, City c);
    public int computeTrade(GameState s, City c);   // post-corruption total trade
    // Split a city's trade into tax/science/luxury using the civ's rates.
    public int[] splitTrade(Civilization civ, int totalTrade);  // {tax, science, luxury}
    // Apply one turn of growth: add (food - 2*pop) to store; grow at foodBox, starve if <0.
    public void grow(GameState s, City c);
    // Add net shields to the store and complete the current production order if affordable.
    public void produce(GameState s, City c);
    // Happiness: returns true if the city should be in civil disorder this turn.
    public boolean computeDisorder(GameState s, City c);
}

// --- Research ---
public final class ResearchEngine {
    public ResearchEngine();
    // Add this turn's science output to the civ; if beakers >= researching.getBaseCost(),
    // learn the tech, clear beakers, and auto-pick the next researchable tech.
    public void advance(GameState s, Civilization civ, int scienceThisTurn);
    // A tech is researchable iff not known and all prereqs are known.
    public java.util.List<TechType> researchable(Civilization civ);
}

// --- Rival AI ---
public final class AIController {
    public AIController(java.util.Random rng);
    // One AI civ's full turn: settle/expand, set production, move & attack with units,
    // manage research, and decide war/peace. Operates only through domain mutators.
    public void takeTurn(GameState s, Civilization civ);
}
```

Task 2 also ships a JUnit (or `public static void main`) self-test at
`civ/src/test/java/com/whim/civ/engine/EngineSelfTest.java` covering: a deterministic combat
duel with a fixed-seed `Random`, a city growth tick, a trade/tax/science split, and a research
unlock. It must compile and pass with `mvn -q test` OR a documented `javac`+`java` invocation.

---

## UI (Task 3 — package `com.whim.civ.ui`)

Task 3 imports `com.whim.civ.domain.*` (and may call `EngineServices` via the GameState's
TurnManager). It MUST NOT import `com.whim.civ.engine.*` directly except optionally in a demo
launcher — talk to the engine only through the `EngineServices` interface handed in.

```java
public final class MainFrame extends javax.swing.JFrame {
    public MainFrame(GameState state, EngineServices engine);
    public void showGame();                // build UI, set visible
}

// Top-down square-tiled viewport with scroll / jump-to-coordinate centering.
public final class MapPanel extends javax.swing.JPanel {
    public MapPanel(GameState state);
    public void centerOn(int tileX, int tileY);   // jump-to-coordinate centering
    public void setTileSize(int px);
    // Repaints terrain, improvements, cities, units for the active civ.
}

// City management screen: shows worked tiles on the fat-cross grid, food/shield/trade,
// the production queue, and completed buildings.
public final class CityScreen extends javax.swing.JDialog {
    public CityScreen(java.awt.Frame owner, GameState state, City city, EconomyView econ);
}

// Read-only view the UI uses to display engine-computed economy numbers without importing
// the engine package. Task 1 declares this interface in domain; Task 2 (or the orchestrator
// Main) provides an adapter backed by EconomyEngine.
//   --> Declared in com.whim.civ.domain. UI depends on the interface, not the engine.
public interface EconomyView {           // (DOMAIN type — listed here for Task 3's use)
    int food(GameState s, City c);
    int shields(GameState s, City c);
    int trade(GameState s, City c);
    int[] tradeSplit(Civilization civ, int totalTrade);
}

// Tech selection tree panel and active-unit command panel.
public final class TechTreePanel extends javax.swing.JPanel {
    public TechTreePanel(GameState state);
    public void setCivilization(Civilization civ);
}
public final class UnitCommandPanel extends javax.swing.JPanel {
    public UnitCommandPanel(GameState state);
    public void setActiveUnit(Unit u);   // null clears
}
```

`EconomyView` is a **DOMAIN** type (package `com.whim.civ.domain`) so both UI and engine can
reference it without a UI→engine dependency. Task 1 authors it; Task 2 provides an
`EconomyEngine`-backed adapter (or the orchestrator wires one in `Main`).

---

## Consolidation (orchestrator)

The orchestrator writes `com/whim/civ/Main.java`: build a `GameState` via
`GameFactory.newStandardGame(...)`, construct the engine `GameEngine`, wrap economy in an
`EconomyView` adapter, build `MainFrame(state, engine)`, and `showGame()`. Then verify the
whole module compiles (`mvn -q -pl civ compile` or `javac` over `civ/src/main/java`) and the
engine self-test passes.

## Civ-1 fidelity checklist (do not violate)

- Square grid with the 21-tile fat-cross city radius. **No hexes.**
- Only Settlers terraform (road/irrigation/mine/railroad) and found cities.
- Trade splits into Tax / Science / Luxury by civ rates summing to 100.
- Despotism penalty: a tile yielding 3+ of food/shields/trade loses 1 of that yield.
- Republic/Democracy add +1 trade to tiles already producing trade.
- Combat uses HP attrition; veteran ×1.5, fortify ×1.5, city walls ×3, terrain % bonus.
- Year starts at 4000 B.C. (`-4000`) and advances by the documented breakpoints, skipping 0.
- Tech tree is an acyclic prerequisite graph terminating toward Space Flight.
