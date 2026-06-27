# Tactical Nexus — Interface Contract

A deterministic (zero-RNG) grid Puzzle-RPG in the spirit of *Tower of the
Sorcerer* / *Tactical Nexus*. Java 8, Swing/AWT only, no external libraries,
no Maven/Gradle for the core. This file is the **binding contract** between the
three parallel child tasks. Do not change a public signature defined here
without reporting the change back to the orchestrator first.

## Hard constraints (all tasks)

- **Java 8 only.** No `var`, no switch expressions, no text blocks, no
  records, no `List.of`, no streams-as-required (allowed but not necessary).
- **Only** `javax.swing`, `java.awt`, `java.util`, `java.lang`.
- **Zero RNG.** No `java.util.Random`, no `Math.random()`. Everything
  deterministic.
- Source root: `tactical-nexus/src`. Package root: `com.whim.tacticalnexus`.
- Compile target 1.8. Run with plain `javac`/`java` (see Build section).

## Package / file ownership (NO two tasks edit the same file)

```
com.whim.tacticalnexus
├── domain/      [TASK 1]  immutable game model
│   ├── Position.java           (row,col value object)
│   ├── KeyColor.java           enum YELLOW, BLUE, RED
│   ├── GemType.java            enum HP, ATK, DEF (+ amount on the gem entity)
│   ├── StairDirection.java     enum UP, DOWN
│   ├── EntityType.java         enum WALL, DOOR, KEY, ENEMY, GEM, STAIR, PLAYER, EMPTY
│   ├── Entity.java             interface (data only — see below)
│   ├── Wall.java, Door.java, KeyItem.java, Enemy.java, StatGem.java, Staircase.java
│   ├── GridMap.java            one floor; immutable, structural-sharing `with…`
│   └── Player.java             immutable stats + inventory + position
├── state/       [TASK 1]
│   ├── GameState.java          immutable snapshot: Player + all floors + floorIndex
│   └── StateManager.java       undo/redo stacks of GameState
├── data/        [TASK 1]
│   └── FloorFactory.java       builds the initial GameState (2–3 demo floors)
├── engine/      [TASK 2]  pure logic, no Swing imports
│   ├── CombatResult.java
│   ├── CombatCalculator.java   deterministic combat math
│   ├── MoveOutcome.java
│   └── MoveResolver.java       resolves a movement vector → new GameState
├── ui/          [TASK 3]  Swing only; reads state, calls engine, never computes rules
│   ├── GamePanel.java          CENTER — draws the grid with Graphics2D primitives
│   ├── StatusPanel.java        EAST — HP/ATK/DEF/keys/gold/exp/level dashboard
│   ├── GameController.java     wires StateManager + MoveResolver; handles a key event
│   └── GameFrame.java          JFrame, BorderLayout, KeyListener (arrows, Ctrl+Z/Y)
└── app/         [TASK 3]
    └── Main.java               EDT bootstrap: FloorFactory → StateManager → GameFrame
```

## Domain model (Task 1)

All domain types are **immutable**. Mutators return new instances.

```java
public final class Position {
    public Position(int row, int col);
    public int row(); public int col();
    public Position translate(int dRow, int dCol);
    // value equality + hashCode required (used as map keys)
}
```

`Entity` is **data only** — interaction rules live in Task 2. Entities never
mutate themselves.

```java
public interface Entity {
    EntityType type();
    boolean blocksMovement();   // true if the player cannot step onto it until resolved
    char glyph();               // debug/text fallback, e.g. '#','D','K','E','+','>','<'
}
```

Concrete entities (constructors are the contract; add getters as needed):

- `Wall()` — type WALL, blocks, glyph '#'.
- `Door(KeyColor color)` — type DOOR, blocks; `color()`.
- `KeyItem(KeyColor color)` — type KEY, does not block; `color()`.
- `StatGem(GemType gem, int amount)` — type GEM, does not block; `gem()`, `amount()`.
- `Staircase(StairDirection dir)` — type STAIR, does not block; `direction()`.
- `Enemy(String name, int hp, int atk, int def, int goldReward, int expReward,
        java.awt.Color color)` — type ENEMY, blocks; getters
  `name() hp() atk() def() goldReward() expReward() color()`.

```java
public final class GridMap {                 // one floor
    public GridMap(int rows, int cols);
    public int rows(); public int cols();
    public Entity at(Position p);            // null == empty floor
    public boolean inBounds(Position p);
    public GridMap with(Position p, Entity e);   // returns NEW map (e==null clears)
    public GridMap without(Position p);          // convenience for clearing
    public Position findStair(StairDirection dir); // null if none
}
```

```java
public final class Player {
    // full constructor with every field, plus convenience starting-state factory.
    public int hp(); public int atk(); public int def();
    public int yellowKeys(); public int blueKeys(); public int redKeys();
    public int gold(); public int exp(); public int level();
    public Position position();
    // immutable mutators — each returns a NEW Player:
    public Player withPosition(Position p);
    public Player withHp(int hp);
    public Player addStats(int dHp, int dAtk, int dDef);
    public Player addKey(KeyColor c, int n);     // n may be negative when spending
    public Player addGold(int n);
    public Player addExp(int n);                 // also recomputes level deterministically
    public int keyCount(KeyColor c);
}
```

Leveling rule (deterministic, keep simple): `level = 1 + exp / 10`. Leveling
grants no automatic stat change — stats come only from gems. EXP/Gold are score.

## State (Task 1)

```java
public final class GameState {
    public GameState(Player player, java.util.List<GridMap> floors, int floorIndex);
    public Player player();
    public GridMap currentFloor();
    public java.util.List<GridMap> floors();   // unmodifiable
    public int floorIndex();
    public GameState withPlayer(Player p);
    public GameState withFloor(int index, GridMap floor);
    public GameState withFloorIndex(int index);
}
```

```java
public final class StateManager {
    public StateManager(GameState initial);
    public GameState current();
    public void commit(GameState next); // push current→undo, set next, clear redo
    public boolean canUndo(); public boolean canRedo();
    public GameState undo();  // returns new current (or unchanged if !canUndo)
    public GameState redo();  // returns new current (or unchanged if !canRedo)
}
```

Because `GameState`/`GridMap`/`Player` are immutable with structural sharing,
`commit` just pushes references — no deep copy needed, snapshots are cheap.

## Engine (Task 2) — pure, no Swing

```java
public final class CombatResult {
    public boolean canFight();   // false ⇒ player ATK ≤ enemy DEF ⇒ unkillable ⇒ move blocked
    public int hitsToKill();
    public int hpLost();         // total HP the player loses if the fight happens
    public boolean survivable(); // hpLost < player's current HP
}
```

**Combat formula (classic Tower-of-the-Sorcerer, player strikes first):**
- If `playerATK <= enemyDEF` → `canFight=false` (enemy invincible; bump = wall).
- `playerDamage = playerATK - enemyDEF`
- `hitsToKill = ceil(enemyHP / playerDamage)`
- `enemyDamage = max(0, enemyATK - playerDEF)`
- Enemy retaliates `hitsToKill - 1` times (it dies on the player's last strike):
  `hpLost = enemyDamage * (hitsToKill - 1)`
- `survivable = hpLost < player.hp()`  (must strictly survive; lethal ⇒ blocked)

```java
public final class CombatCalculator {
    public static CombatResult resolve(Player p, Enemy e);
}
```

```java
public final class MoveOutcome {
    public GameState state();   // resulting state (same instance if nothing changed)
    public boolean changed();   // true if the move mutated state (undo-worthy)
    public String message();    // short human description for status/log
}
```

```java
public final class MoveResolver {
    // dRow/dCol ∈ {-1,0,1}; resolves what the player bumps into.
    public static MoveOutcome resolve(GameState state, int dRow, int dCol);
}
```

`MoveResolver` rules when target cell contains:
- **null/empty & in-bounds** → move player there.
- **out of bounds / Wall** → blocked, `changed=false`.
- **Door(color)** → if player has ≥1 matching key: spend one key, clear the door,
  player does NOT advance this move (door opens in place). Else blocked.
- **KeyItem(color)** → add key, clear cell, move player onto it.
- **StatGem(gem,amount)** → apply to player stats, clear cell, move onto it.
- **Enemy** → `CombatCalculator.resolve`; if `!canFight` or `!survivable` →
  blocked; else subtract `hpLost`, add gold+exp, clear enemy, move onto it.
- **Staircase(UP/DOWN)** → change `floorIndex` (+1 up / −1 down), move player to
  the matching opposite stair on the destination floor (DOWN-stair if you went
  UP, etc.); if no matching stair, place on the destination's entry stair.

Only `changed=true` outcomes get committed to `StateManager` by the controller.

## UI (Task 3) — Swing only, no rule math

- `GamePanel(StateManager)` reads `current()` each `paintComponent` and draws the
  current floor with `Graphics2D` primitives: walls = dark gray squares, doors =
  colored squares matching key color with a keyhole, keys = small colored key
  glyph/diamond, gems = colored circle with +/letter, enemies = colored square
  with name initial and a tiny HP/ATK/DEF tag, stairs = '>'/'<' chevrons, player
  = distinct token (e.g. teal rounded square). Tile size constant ~40px.
- `StatusPanel(StateManager)` shows a strictly formatted dashboard: HP, ATK, DEF,
  Yellow/Blue/Red keys, Gold, EXP, Level. Refresh on `refresh()`.
- `GameController(StateManager)` exposes:
  `move(int dRow,int dCol)`, `undo()`, `redo()` — each calls the engine /
  StateManager and returns whether the view should repaint. It owns NO rules.
- `GameFrame` uses `BorderLayout`: `GamePanel` CENTER, `StatusPanel` EAST.
  KeyListener: arrows → `controller.move`; Ctrl+Z → undo; Ctrl+Y (or Ctrl+Shift+Z)
  → redo. After any handled key, repaint panel + refresh status. Frame is
  focusable and grabs focus so keys register.

## Data (Task 1)

`FloorFactory.initialState()` returns a `GameState` with **2–3 small demo floors
(≈11×11)** that exercise every mechanic: walls forming rooms, all three door/key
colors, at least one of each gem type, several enemies of increasing difficulty,
and UP/DOWN stairs linking the floors. Player starts ~`HP 1000, ATK 10, DEF 10`,
no keys, on floor 0. Must be **winnable deterministically** and contain at least
one enemy that is currently unkillable (ATK too low) to demonstrate the blocked
move, becoming killable after collecting an ATK gem.

## Build & run (document in tactical-nexus/README.md, Task 3)

```bash
cd tactical-nexus
javac -d out $(find src -name "*.java")
java -cp out com.whim.tacticalnexus.app.Main
```

Optionally include a tiny `engine/EngineSmokeTest.java`-style `main` (Task 2) or
a JUnit-free assertion `main` for the combat math. Keep it dependency-free.
