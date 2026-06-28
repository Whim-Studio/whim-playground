# Nobunaga's Ambition: Zenkokuban — Interface Contract

A standalone Java 8 + Swing adaptation of the 1986 grand-strategy classic
*Nobunaga's Ambition: Zenkokuban (Country-Wide Edition)*. Turn-based Sengoku-era
unification sim: 50 provinces, fief economy, loyalty/rebellions, random disasters,
and grid-based tactical battles. No external libraries; all visuals are drawn
procedurally with `Graphics2D`.

This file is the **binding contract** between the three parallel child tasks.
**Do not change a public signature defined here without reporting back to the
orchestrator first.** Cross-package code is mergeable ONLY because every signature
below is fixed.

## Hard constraints (ALL tasks)

- **Java 8 only.** No `var`, no switch *expressions*, no text blocks, no records,
  no `List.of`/`Map.of`, no Java 9+ APIs. Classic `switch` statements are fine.
- **Only** `javax.swing`, `java.awt`, `java.util`, `java.lang`, `java.util.Random`.
- RNG **is allowed and required** (disasters, battles, AI). Use the single
  `Random` instance exposed by `GameState.rng` so runs are reproducible by seed.
  Do **not** create new `Random` objects elsewhere; do not use `Math.random()`.
- Source root: `nobunaga/src`. Package root: `com.whim.nobunaga`.
- Compile target 1.8. Plain `javac`/`java` (see Build section). No Maven/Gradle.
- UI never computes game rules; it reads domain state and calls `GameEngine`.
- Engine never imports `javax.swing`/`java.awt.*` EXCEPT `java.awt.Color`
  (Color lives on domain objects and is fine to read).

## Package / file ownership (NO two tasks edit the same file)

```
com.whim.nobunaga
├── domain/      [TASK 1] core model + interfaces (no engine/ui imports)
│   ├── Season.java            enum SPRING, SUMMER, FALL, WINTER
│   ├── Province.java          fief state
│   ├── Daimyo.java            warlord
│   ├── GameState.java         whole-world snapshot + seeded Random + clock
│   ├── BattleUnit.java        one tactical unit on the battle grid
│   ├── BattleState.java       a tactical battle in progress
│   ├── GameEngine.java        INTERFACE implemented by Task 2
│   └── GameLoopManager.java   season/year progression; calls GameEngine
├── map/         [TASK 1] static world data
│   └── ProvinceData.java      builds the 50-province GameState (factory)
├── engine/      [TASK 2] pure logic, implements GameEngine
│   ├── GameEngineImpl.java    implements com.whim.nobunaga.domain.GameEngine
│   ├── EconomyEngine.java     harvest / tax / upkeep / attrition
│   ├── EventEngine.java       disasters (typhoon/ninja/plague) + rebellions
│   ├── BattleEngine.java      tactical resolution: melee, move, supply, victory
│   ├── AiEngine.java          rival-daimyo decision tree
│   └── EngineSmokeTest.java   dependency-free main() asserting the math
├── ui/          [TASK 3] Swing only; reads state, calls engine
│   ├── MapPanel.java          CENTER — draws the 50-province map (Graphics2D)
│   ├── DashboardPanel.java    EAST — dense retro stats for selected province
│   ├── ActionPanel.java       SOUTH — Tax/Cultivate/FloodCtrl/Recruit/Transfer/War/End Season
│   ├── BattlePanel.java       modal grid tactical battle view
│   ├── StartScreen.java       Daimyo selection screen
│   ├── GameController.java    wires GameState + GameLoopManager + GameEngine
│   └── GameFrame.java         JFrame + BorderLayout
└── app/         [TASK 3]
    └── Main.java              EDT bootstrap: ProvinceData.newGame → GameEngineImpl → GameFrame
```

## Daimyo roster (FIXED — all tasks rely on these ids/colors/abbrev)

| id | name              | abbrev | Color                    | home province id |
|----|-------------------|--------|--------------------------|------------------|
| 0  | Oda Nobunaga      | ODA    | `new Color(200,40,40)`   | 20 (Owari)       |
| 1  | Takeda Shingen    | TAK    | `new Color(40,80,200)`   | 17 (Kai)         |
| 2  | Uesugi Kenshin    | UES    | `new Color(70,160,210)`  | 14 (Echigo)      |
| 3  | Hojo Ujiyasu      | HOJ    | `new Color(225,140,30)`  | 11 (Sagami)      |
| 4  | Mori Motonari     | MOR    | `new Color(40,150,70)`   | 38 (Aki)         |
| 5  | Shimazu Yoshihisa | SHI    | `new Color(170,60,170)`  | 48 (Satsuma)     |
| 6  | Date Terumune     | DAT    | `new Color(120,90,160)`  | 5  (Mutsu-S)     |
| 7  | Chosokabe Kunichika| CHO   | `new Color(210,190,40)`  | 44 (Tosa)        |

Player picks one of these on the start screen (default id 0). Provinces not owned
by a roster daimyo are **neutral** (`ownerId == -1`, drawn gray, weakly garrisoned).

## domain/ signatures [TASK 1]

### `enum Season { SPRING, SUMMER, FALL, WINTER }`
- `Season next()` — cyclic successor (WINTER → SPRING).
- `String label()` — e.g. "Spring".

### `class Province`
Constructor: `Province(int id, String name, int x, int y)`
Fields are **private** with public getters/setters (`getGold()/setGold(int)` …):
- `int id` (final), `String name` (final)
- `int x, y` — map layout coords in a 0..1000 virtual space (Task 3 scales).
- `int ownerId` — daimyo id, or `-1` neutral. default -1.
- `int gold` — treasury. `int rice` — provisions (koku).
- `int loyalty` — peasant loyalty 0..100. default 70.
- `int taxRate` — 0..100, default 40.
- `int floodControl` — 0..100, default 20. (mitigates disaster damage)
- `int cultivation` — 0..100, default 20. ("wealth"/yield base; drives gold+rice)
- `int soldiers` — stationed troops.
- `List<Integer> getAdjacent()` — mutable list of reachable province ids
  (war + transfer). Populated by `ProvinceData`.
- convenience: `boolean isNeutral()` → `ownerId == -1`.

### `class Daimyo`
Constructor: `Daimyo(int id, String name, String abbrev, Color color, int age, int health)`
- `int id`(final), `String name`(final), `String abbrev`(final), `Color color`(final)
- `int age` (default per-roster ~30-50), `int health` 0..100 (default 90)
- `boolean player` (set by ProvinceData/Main for the chosen daimyo)
- `List<Integer> getProvinceIds()` — mutable list of owned province ids
- `boolean isAlive()` → `health > 0 && !getProvinceIds().isEmpty()`

### `class GameState`
- `List<Province> provinces` — index-aligned so `provinces.get(id).getId()==id`.
- `List<Daimyo> daimyos` — index-aligned to id (8 entries).
- `int year` (start 1560), `Season season` (start SPRING).
- `int playerDaimyoId`.
- `final Random rng` — the single shared RNG.
- Constructor: `GameState(List<Province> provinces, List<Daimyo> daimyos, int playerDaimyoId, long seed)`
- `Province province(int id)`, `Daimyo daimyo(int id)`, `Daimyo player()`.
- `List<Province> provincesOf(int daimyoId)`.
- `void advanceClock()` — `season = season.next(); if (season==SPRING) year++;`

### `class BattleUnit`
Constructor: `BattleUnit(int id, int daimyoId, boolean attacker, boolean commander, int col, int row, int troops, String abbrev, Color color)`
- getters for all; `int troops` and `int morale`(0..100, default 100) are settable.
- `int col, row` settable (grid position). `boolean isAlive()` → `troops > 0`.
- `boolean isCommander()`, `boolean isAttacker()`.

### `class BattleState`
- `int cols` (=14), `int rows` (=10).
- `List<BattleUnit> units`.
- `int day` (starts 1).
- `int attackerProvId, defenderProvId, attackerDaimyoId, defenderDaimyoId`.
- `int attackerRice, defenderRice` — supplies; decrement daily.
- `Integer winnerDaimyoId` — `null` while ongoing; set to winning daimyo id when decided.
- `String log` — last-day human-readable summary (Task 3 displays it).
- Constructor:
  `BattleState(int attackerProvId, int defenderProvId, int attackerDaimyoId, int defenderDaimyoId)`
- `BattleUnit unitAt(int col,int row)` — or null. `boolean inBounds(int c,int r)`.

### `interface GameEngine`  (implemented by Task 2's `GameEngineImpl`)
All macro actions mutate `GameState` in place and return a one-line result string
(success or reason for failure — UI shows it). They must validate gold/rice/ownership.
```java
String setTax(GameState s, int provinceId, int rate);          // rate 0..100
String cultivate(GameState s, int provinceId);                 // spend gold → +cultivation
String floodControl(GameState s, int provinceId);              // spend gold → +floodControl
String recruit(GameState s, int provinceId, int soldiers);     // spend gold+rice → +soldiers
String transfer(GameState s, int fromId, int toId, int gold, int rice, int soldiers);
java.util.List<String> endSeason(GameState s);                 // economy+events+rebellions+AI for current season. Does NOT advance the clock.
// Battle:
BattleState startBattle(GameState s, int attackerProvId, int defenderProvId, int committedSoldiers, int committedRice);
void issueOrder(BattleState b, int unitId, int targetCol, int targetRow); // queue a player move/attack for next day
void battleAdvanceDay(BattleState b);                          // resolve one DAY: enemy(+un-ordered) AI moves, melee, daily supply burn, set winnerDaimyoId if decided
boolean battleResolved(BattleState b);                         // winnerDaimyoId != null
void applyBattleOutcome(GameState s, BattleState b);           // apply troop losses; transfer province if attacker won
```

### `class GameLoopManager`
- Constructor: `GameLoopManager(GameEngine engine)`.
- `List<String> endSeason(GameState s)` — `List<String> log = engine.endSeason(s); s.advanceClock(); return log;`
- `String seasonHeader(GameState s)` — e.g. `"1560 Spring"`.

## map/ signatures [TASK 1]
### `class ProvinceData`
- `static GameState newGame(int playerDaimyoId, long seed)` — builds the **50**
  `Province` objects (ids 0..49) with believable Japanese province names, `(x,y)`
  layout coords forming a recognizable Honshu→Kyushu→Shikoku arrangement, and a
  **connected adjacency graph** (each province adjacent to its geographic
  neighbors; war/transfer only along adjacency). Creates the 8 roster `Daimyo`,
  assigns each its home province + 1-2 nearby provinces, leaves the rest neutral
  (lightly garrisoned). Sets `player=true` and `playerDaimyoId` for the choice.
  Must guarantee the graph is connected and every owned province list is consistent
  with each `Province.ownerId`.

## engine/ rules [TASK 2] (formulas are guidance — keep them balanced & deterministic-by-seed)
- **Fall harvest** (only in FALL): `rice += round(cultivation * 12 * (0.6 + 0.01*loyalty))`.
- **Tax** (every season): `gold += round(cultivation * taxRate * 0.05 * (loyalty/100.0))`;
  loyalty drifts toward `(100 - taxRate)`: `loyalty += sign * min(5, |target-loyalty|)`.
- **Upkeep** (every season): `rice -= ceil(soldiers/100.0 * 4)`; if rice < 0, soldiers
  starve: `soldiers -= attrition`, rice clamps to 0 (military attrition).
- **Events** (EventEngine, chance per province/season, scaled by `1 - floodControl/120`):
  Typhoon (rice/cultivation damage, worse with low floodControl), Plague (loyalty+soldiers),
  Ninja (gold theft / sabotage). Log each.
- **Rebellion**: if `loyalty < 20`, chance of revolt → destroys a chunk of gold/rice
  and soldiers; very low loyalty can flip a province to neutral. Log it.
- **AI** (AiEngine): for each non-player living daimyo — adjust tax toward stable
  loyalty, cultivate/flood when rich, recruit when safe, and invade the weakest
  adjacent enemy/neutral province when troop advantage is sufficient (auto-resolve
  battles via BattleEngine).
- **Battle victory** (BattleEngine), checked end of each day, in order:
  1. a side's **commander** unit dead → other side wins;
  2. a side has **no living units** (full retreat) → other side wins;
  3. a side's **rice hits 0** (starvation) → that side loses;
  4. hard cap (e.g. day > 30) → defender wins (siege repelled).
  Melee: adjacent enemy units exchange `troops` damage scaled by troops*morale and
  small RNG; losing morale can rout a unit (removed). Daily supply burn proportional
  to surviving troops.

## ui/ requirements [TASK 3]
- `StartScreen` → pick a daimyo (8 buttons, procedural flag swatches) → builds game.
- `GameFrame` BorderLayout: `MapPanel` CENTER, `DashboardPanel` EAST, `ActionPanel` SOUTH,
  header label with `GameLoopManager.seasonHeader`.
- `MapPanel`: draw each province as a filled polygon/rounded rect at scaled `(x,y)`,
  tinted by owner `Daimyo.color` (gray if neutral); draw adjacency lines; draw a clan
  marker (colored square + `abbrev`) and soldier count; click selects a province
  (highlight). Expose a listener so the controller learns the selection.
- `DashboardPanel`: dense monospaced readout for the selected province — owner, Gold,
  Rice, Loyalty, Tax%, Flood, Cultivation(Wealth), Soldiers, adjacency list.
- `ActionPanel`: buttons Tax, Cultivate, Flood Control, Recruit, Transfer, **War**,
  **End Season**. Each calls the matching `GameEngine`/`GameLoopManager` method via
  `GameController`, shows the returned log (e.g. in a scrolling text area / dialog),
  and repaints. War on an adjacent enemy/neutral province opens `BattlePanel`.
- `BattlePanel`: render the `cols×rows` grid; draw `BattleUnit`s as colored squares
  with abbrev + troops; show day, both rice supplies, and the log. Let the player
  click-select a friendly unit then click a target tile (→ `issueOrder`), and a
  "Next Day" button (→ `battleAdvanceDay`); when `battleResolved`, call
  `applyBattleOutcome`, show result, and close.
- **Verification target:** running `Main` shows the start screen; picking a daimyo
  shows the map; selecting provinces updates the dashboard; actions change stats;
  End Season advances the header and produces an event log; War launches a battle
  that can be fought to a decision.

## Build & run (Task 3 documents this in `nobunaga/README.md`)
```bash
cd nobunaga
javac -d out $(find src -name "*.java")
java -cp out com.whim.nobunaga.app.Main
# engine self-check:
java -cp out com.whim.nobunaga.engine.EngineSmokeTest
```

## Branching / handoff (all child tasks)
- Branch from `whim-wd-325` (already on origin). Create **only** files inside your
  owned directory list above. You may create throwaway local stubs of other
  packages to type-check, but **do not commit** anything outside your ownership.
- Push your branch and open a PR into `whim-wd-325`. Report completion (or any
  blocker / proposed signature change) back to the orchestrator via `send_prompt`,
  not via task comments.
