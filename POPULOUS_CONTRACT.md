# Populous (1989) — Build Contract

A standalone, **Java 8**, zero-dependency, procedural-graphics Swing adaptation of the
Bullfrog god game **Populous**. Three parallel tasks build against the shared
`com.whim.populous.api` package (already committed by the orchestrator). This file is
the single source of truth for the seams between them.

## Hard constraints (ALL tasks)

- **Java 8 ONLY.** No `var`, no switch expressions, no text blocks, no records, no
  `List.of`/`Map.of`, no Java 9+ Stream collectors, no local type inference.
  `maven.compiler.source/target = 1.8`.
- **Zero external libraries.** Only `javax.swing`, `java.awt`, `java.util`, `java.io`,
  `java.util.concurrent`. JUnit allowed for tests only.
- **No downloaded assets.** All visuals via `java.awt.Graphics2D` (shapes, gradients,
  strokes, colors). No image files, no icons on disk.
- **Package root:** `com.whim.populous`  •  **Source root:** `populous/src/`
- Each task imports ONLY from `api` and its own sub-package. No task edits another
  task's sub-package. Everything must **compile** (`cd populous && mvn -q compile`).

## Package ownership

| Package | Owner | Contents |
|---|---|---|
| `com.whim.populous.api` | **Orchestrator (DONE — do not edit)** | `Enums`, `Views`, `GameController`, `ActionResult` |
| `com.whim.populous.domain` | **Task 1** | `MapGrid`(impl `MapView`), `Tile`(impl `TileView`), `Follower`(impl `FollowerView`), `Settlement`, `PapalMagnet`(impl `PapalMagnetView`), `GameState`(impl `GameStateView`), `GameStateManager`, `TerrainRules`, `SettlementRules` |
| `com.whim.populous.engine` | **Task 2** | `SimulationEngine`(impl `GameController`), `SimLoop`, `FollowerAI`, `DivinePowers`, `RivalAI`, `ManaSystem`, `VictoryMonitor` |
| `com.whim.populous.ui` | **Task 3** | `GameFrame`, `MapPanel`, `DashboardPanel`, `Renderer`, `UiColors`, `StubController` (dev only, impl `GameController`) |
| `com.whim.populous.app` | **Orchestrator (final)** | `Main` — wires `SimulationEngine` + `GameFrame` |

## The `api` package (already committed — read it, do NOT modify)

- `Enums`: `Allegiance{NEUTRAL,GOOD,EVIL}`, `TerrainType{WATER,SHALLOW,SAND,GRASS,HILL,MOUNTAIN,ROCK,LAVA,SWAMP}`,
  `SettlementType{NONE,TENT,HUT,HOUSE,TOWER,CASTLE}`, `GodPower{RAISE_LAND,LOWER_LAND,PAPAL_MAGNET,EARTHQUAKE,SWAMP,VOLCANO,FLOOD,ARMAGEDDON}`
  (each GodPower carries `label()`, `manaCost()`, `targeted()`).
- `Views`: read-only `TileView`, `MapView`, `FollowerView`, `PapalMagnetView`, `GameStateView`.
- `GameController`: the ONE UI↔engine seam. `state()`, `selectPower(p)`, `primaryClick(col,row)`,
  `secondaryClick(col,row)`, `castGlobal(p)`, `start()`, `stop()`, `newGame(seed)`, `tickOnce()`,
  `addChangeListener/removeChangeListener` + nested `ChangeListener.onStateChanged()`.
- `ActionResult`: `ok(msg)` / `fail(msg)`, `isSuccess()`, `message()`.

**Data flow:** UI renders `controller.state()` (a `GameStateView`). Input calls a
`GameController` method. Engine mutates domain on its sim thread, then fires
`onStateChanged()`; UI repaints on the EDT by re-reading `state()`. UI NEVER casts a
`*View` to a concrete class.

**Coordinates:** `col`=x (0..cols-1), `row`=y (0..rows-1). Follower `x()/y()` are
fractional tile coordinates for smooth movement.

## Ruleset the domain + engine must encode

### Landscape
- Grid target **64×64** tiles. Each `Tile` has signed integer `elevation`. `seaLevel`
  is a fixed reference (recommend 0). Terrain derives from elevation:
  `<seaLevel-1`→WATER, `seaLevel-1`→SHALLOW, `seaLevel`..`seaLevel`→SAND(coast),
  `+1..+2`→GRASS, `+3..+4`→HILL, `+5..+6`→MOUNTAIN, `+7+`→ROCK. SWAMP/LAVA are
  transient overrides set by powers.
- **Terraforming** (RAISE_LAND / LOWER_LAND): a click raises/lowers a small brush of
  tiles by 1 step, smoothing toward neighbours (classic "pull up a column" feel).
  Land only becomes buildable when **flat** (a plateau of equal elevation above sea).

### Followers (Walkers)
- A follower has `allegiance`, `health` 0..100, `stamina` 0..100, fractional position.
- Autonomous loop: **seek the largest nearby flat plateau of own/neutral land → walk to
  it → found/upgrade a settlement**. If none, help flatten by wandering to rough land.
- Followers **breed** at settlements over time (spawn a new follower while under the
  side's population cap), **migrate** when their settlement is overcrowded, and are
  drawn toward their side's **Papal Magnet** when active.
- A follower **drowns** (dies) if it ends a tick on a WATER/SHALLOW/SWAMP tile; loses
  health on LAVA. Stamina drains while walking, recovers at a settlement.

### Settlements (flat-area tiers — Task 1 owns exact numbers, target table)
Measured as the size of the contiguous flat plateau (same elevation, above sea, own/neutral)
around the build tile:

| Flat tiles | SettlementType | Rel. pop | Rel. mana |
|---|---|---|---|
| 1     | TENT   | 1 | 1 |
| 2–3   | HUT    | 2 | 2 |
| 4–8   | HOUSE  | 4 | 4 |
| 9–15  | TOWER  | 7 | 7 |
| 16+   | CASTLE | 12 | 12 |

Bigger/flatter → faster breeding and more mana per tick.

### Mana & Divine Powers
- Each side accrues **mana per tick ∝ its total population** (sum of settlement mana
  weights). `maxMana` is the full-bar reference. A power fires only if the caster can
  afford `manaCost()`; casting deducts it.
- Powers (see `GodPower`): RAISE/LOWER (free terraform trickle), PAPAL_MAGNET (set rally
  point, followers converge), EARTHQUAKE (crack/lower a jagged fault, topple settlements),
  SWAMP (mark tiles SWAMP that swallow walkers), VOLCANO (raise a tall cone at target +
  scatter ROCK/LAVA), FLOOD (global: sea level rises N steps for a while, drowning
  low-elevation walkers), ARMAGEDDON (global: summon all followers of both sides to a
  central battlefield for a final fight).

### Rival (Evil) Deity — state machine
`EXPAND` (flatten land near its followers) → `GROW` (let settlements mature, bank mana) →
`ATTACK` (once mana ≥ threshold, cast a disaster at the player's densest settlement
cluster) → back to EXPAND. Also maintains its own Papal Magnet to herd followers.

### Victory
Game is **won by GOOD when EVIL population reaches 0** (and vice-versa). `GameStateView.gameOver()`
true, `winner()` set. Engine stops the loop.

## Cross-task interface rules
- Task 1 exposes concrete domain constructors + a `GameStateManager` factory that Task 2
  drives. Task 2's `SimulationEngine` OWNS a `GameStateManager`, runs the loop, and
  implements `GameController`. Task 3 depends ONLY on `api` (it gets a `GameController`
  injected; `StubController` fakes one for standalone UI runs).
- Provide a tiny `main`-less self-check where practical (`DomainSelfCheck`, `EngineSmokeTest`)
  that constructs state and asserts invariants without a display.

## Reporting
When done, each task must: ensure `cd populous && mvn -q compile` is clean for its package,
push its branch, open a PR into `whim-wd-407`, and **report back to this orchestrator task
via `send_prompt`** with the PR link + a one-paragraph summary + any interface friction.
