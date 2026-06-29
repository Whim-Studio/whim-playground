# Ruinlander — Interface Contract

A turn-based post-apocalyptic survival RPG (overworld exploration + tactical
combat + crafting). Java 8, Swing/AWT/`java.util` only, no external libraries,
no Maven/Gradle for the core. This file is the **binding contract** between the
three parallel child tasks. Do not change a public signature defined here
without reporting the change back to the orchestrator first.

## Hard constraints (ALL tasks)

- **Java 8 only.** No `var`, no switch expressions, no text blocks, no
  records, no `List.of`/`Map.of`, no `java.time` requirement. Streams are
  allowed but not required.
- **Only** `javax.swing`, `java.awt`, `java.util`, `java.lang`, `java.io`
  (for nothing networked). **No external assets / no downloads.** All visuals
  are drawn with `Graphics2D` primitives, colors, or Unicode glyphs.
- Source root: `ruinlander/src`. Package root: `com.whim.ruinlander`.
- Compile target 1.8. Run with plain `javac`/`java` (see Build section).
- **RNG ownership:** ONLY `engine` may use randomness, via a single
  `java.util.Random` instance it owns (constructed with a seed). `domain` and
  `ui` contain **zero** `Random`/`Math.random()`. UI reads state and calls the
  engine; it never rolls dice itself.
- **Dependency direction:** `ui` → `engine` → `domain`. `domain` imports
  nothing from `engine`/`ui`. `engine` imports only `domain`. No `engine` class
  imports `javax.swing`/`java.awt`.

## Package / file ownership (NO two tasks edit the same file)

```
com.whim.ruinlander
├── domain/      [TASK 1]  mutable game model + state machine
│   ├── Position.java          (int x,y value object; equals/hashCode; manhattan/adjacent helpers)
│   ├── StatType.java          enum HEALTH, HUNGER, THIRST, FATIGUE, RADIATION, TEMPERATURE
│   ├── TerrainType.java       enum WASTELAND, CITY, TOXIC, SETTLEMENT, FOREST, WATER, ROAD
│   ├── GameMode.java          enum EXPLORATION, COMBAT, INVENTORY, CRAFTING, GAME_OVER
│   ├── Faction.java           enum SCAVENGERS, ENCLAVE, RAIDERS, NEUTRAL
│   ├── EntityType.java        enum PLAYER, RAIDER, MUTANT, ANIMAL, CONTAINER, SETTLEMENT
│   ├── ItemCategory.java      enum FOOD, WATER, MATERIAL, MEDICAL, WEAPON, ARMOR, MISC
│   ├── WeaponClass.java       enum MELEE, FIREARM
│   ├── Item.java              item definition (id,name,category,weight,stackable + effect fields)
│   ├── Weapon.java            extends Item: weaponClass, damage, accuracy, apCost, range, ammoItemId
│   ├── Armor.java             extends Item: damageReduction (0..1), coverage
│   ├── Inventory.java         List<ItemStack>, add/remove/count/find, capacity by weight
│   ├── ItemStack.java         Item + quantity
│   ├── Entity.java            interface (data only — see signatures below)
│   ├── Enemy.java             implements Entity (hp, maxHp, ap, attack, accuracy, armorReduction, faction)
│   ├── Container.java         implements Entity (Inventory loot, looted flag)
│   ├── Settlement.java        implements Entity (Faction, name; reputation lives on Player)
│   ├── Tile.java              TerrainType + optional Entity + discovered flag
│   ├── GridMap.java           Tile[][]; width/height; getTile/setEntity/inBounds; playerStart
│   ├── Player.java            stats map, Inventory, equipped Weapon/Armor, Position, AP, rep map
│   └── GameStateManager.java  holds Player + GridMap + GameMode + active CombatState; mode transitions
├── engine/      [TASK 2]  pure logic, NO Swing/AWT imports
│   ├── SurvivalEngine.java    stat decay per step + environmental damage (see signatures)
│   ├── CombatState.java       per-encounter tactical state (player AP, enemy list, turn owner, log)
│   ├── CombatEngine.java      hit chance, damage, AP spend, enemy AI turn, win/lose detection
│   ├── AttackOutcome.java     hit/miss, damage dealt, target killed, message
│   ├── EncounterGenerator.java decides if/what encounter triggers for a terrain (uses engine RNG)
│   ├── LootGenerator.java     fills Container loot for a terrain (uses engine RNG)
│   ├── CraftingSystem.java    recipe list; canCraft(inv,recipe); craft(inv,recipe) -> ItemStack
│   ├── Recipe.java            id,name, Map<itemId,qty> inputs, output ItemStack
│   └── ItemDb.java            static factory of all Item/Weapon/Armor definitions + starter map
├── ui/          [TASK 3]  Swing presentation; reads state, calls engine
│   ├── GameController.java    owns GameStateManager + engine objects; wires input→engine→repaint
│   ├── GameFrame.java         JFrame, BorderLayout, assembles panels
│   ├── MapPanel.java          CENTER: draws exploration grid OR combat grid via Graphics2D
│   ├── StatusPanel.java       side dashboard: JProgressBar per StatType + equipped gear
│   ├── LogPanel.java          scrolling action log (JTextArea)
│   ├── InventoryPanel.java    inventory grid + crafting menu (CARD-swapped or toggled)
│   └── input handling          KeyListener (movement/menus) + MouseListener (combat targets)
└── app/         [TASK 3]
    └── Main.java               public static void main → builds ItemDb starter state, shows GameFrame
```

`data`/initial-world building: TASK 1 owns a `domain/WorldFactory.java` that
builds the initial `GridMap` + `Player` (placing terrain, settlements, a few
containers/enemies). It may call `engine.ItemDb` for starter items — so
`WorldFactory` is the ONE allowed `domain → engine` reference; keep it confined
to that file. (If you prefer zero domain→engine coupling, TASK 1 may instead
expose `WorldFactory.build(Map<String,Item> starterItems)` and let the
controller pass `ItemDb` items in. Either is acceptable; pick one and note it.)

## Key public signatures (binding)

### domain
```java
public final class Position {
    public final int x, y;
    public Position(int x, int y);
    public int manhattan(Position o);
    public boolean isAdjacent(Position o);     // 4-neighbour
    // equals/hashCode on (x,y)
}

public interface Entity {
    EntityType getType();
    Position getPosition();
    void setPosition(Position p);
    String glyph();        // single Unicode/ASCII char for rendering, e.g. "R","M","☣"
    java.awt.Color color(); // suggested render color (AWT is fine in domain for this hint)
}

public class Player {
    public Player(Position start);
    public int getStat(StatType t);                 // 0..max
    public int getMaxStat(StatType t);
    public void setStat(StatType t, int v);          // engine clamps via this
    public void addStat(StatType t, int delta);
    public boolean isDead();                          // HEALTH<=0
    public Position getPosition();  public void setPosition(Position p);
    public Inventory getInventory();
    public Weapon getEquippedWeapon();  public void equipWeapon(Weapon w);
    public Armor getEquippedArmor();    public void equipArmor(Armor a);
    public int getActionPoints();  public void setActionPoints(int ap);  // combat
    public int getReputation(Faction f);  public void addReputation(Faction f, int d);
}
// NOTE: TEMPERATURE is a "comfort" stat: 100 = ideal, low = hypothermia risk.
// RADIATION accumulates upward (0 good, high bad). HUNGER/THIRST/FATIGUE
// accumulate upward (0 = sated, 100 = starving). HEALTH downward = death.

public class GridMap {
    public GridMap(int width, int height);
    public int getWidth();  public int getHeight();
    public boolean inBounds(int x, int y);
    public Tile getTile(int x, int y);
    public void setTile(int x, int y, Tile t);
    public Position getPlayerStart();  public void setPlayerStart(Position p);
}

public class GameStateManager {
    public GameStateManager(GridMap map, Player player);
    public GridMap getMap();  public Player getPlayer();
    public GameMode getMode();  public void setMode(GameMode m);
    public Object getCombatState();   // holds engine.CombatState as Object to avoid domain→engine import;
    public void setCombatState(Object cs);  // controller casts. (Or use generics-free Object handle.)
    public int getTurnCount();  public void incrementTurn();
}
```

### engine
```java
public class SurvivalEngine {
    public SurvivalEngine();
    // Apply one overworld step on the given terrain: increments hunger/thirst/
    // fatigue, adjusts temperature toward terrain ambient, adds radiation in
    // TOXIC tiles, then applies health damage from any critical stat.
    // Returns human-readable notes for the log (may be empty list).
    public java.util.List<String> applyStep(Player p, TerrainType terrain);
    // Consume an item's effects (food reduces hunger, water reduces thirst, etc.)
    public java.util.List<String> consume(Player p, Item item);
}

public class CombatEngine {
    public CombatEngine(java.util.Random rng);
    public CombatState start(Player p, java.util.List<Enemy> enemies);
    public AttackOutcome playerAttack(CombatState s, Player p, Enemy target);
    public java.util.List<AttackOutcome> enemyTurn(CombatState s, Player p);
    public boolean playerWon(CombatState s);
    public boolean playerLost(Player p);
    // hit chance = clamp(baseAcc - distancePenalty - targetEvade, 5%, 95%)
}

public class EncounterGenerator {
    public EncounterGenerator(java.util.Random rng);
    // Returns enemies for an encounter, or empty list if none triggered.
    public java.util.List<Enemy> maybeEncounter(TerrainType terrain, int turnCount);
}

public class CraftingSystem {
    public CraftingSystem();
    public java.util.List<Recipe> getRecipes();
    public boolean canCraft(Inventory inv, Recipe r);
    public ItemStack craft(Inventory inv, Recipe r); // consumes inputs; returns output or null
}

public final class ItemDb {
    public static Item get(String id);
    public static Weapon weapon(String id);
    public static Armor armor(String id);
    public static java.util.Map<String,Item> all();
}
```

### ui
- `GameController` constructs `GameStateManager`, `SurvivalEngine`,
  `CombatEngine`, `EncounterGenerator`, `CraftingSystem` (one shared
  `new java.util.Random(SEED)` passed to engine ctors).
- Movement (WASD/arrows): controller validates bounds via `GridMap`, moves
  player, calls `SurvivalEngine.applyStep`, then `EncounterGenerator
  .maybeEncounter`; if enemies returned, `CombatEngine.start` and
  `setMode(COMBAT)`.
- Combat input: select target (mouse or number key) → `playerAttack`; when
  player AP exhausted or player ends turn → `enemyTurn`; check win/lose.
- UI only reads `Player`/`GridMap`/`CombatState`; never mutates stats directly.

## Build & run (binding)

```bash
# from repo root
find ruinlander/src -name '*.java' > /tmp/ruinlander.sources
javac -d ruinlander/out @/tmp/ruinlander.sources
java -cp ruinlander/out com.whim.ruinlander.app.Main
```
Java 8 must compile clean with `-source 8 -target 8` (do not rely on newer
APIs). Add `ruinlander/out/` to `ruinlander/.gitignore`.

## Reporting

Each task pushes its branch, opens a PR into `whim-wd-338`, and sends a
send_prompt report to the orchestrator with: files added, any signature
deviation from this contract (with reason), and how it verified compilation of
its own package against stub/shared domain.
