# OG Galaxy — Build Contract

A standalone, **Java 8**, zero-dependency, procedural-graphics **Swing** single-player
recreation of the browser strategy MMO **OG Galaxy** (itself an OGame clone). The MMO /
alliance / tournament layer is replaced by computer-controlled AI empires that play by the
same rules. Two parallel tasks build against the shared, already-committed `api` package.
This file is the single source of truth for the seams between them.

## Hard constraints (ALL tasks)

- **Java 8 ONLY.** No `var`, no `switch` expressions, no text blocks, no records, no
  `List.of`/`Map.of`, no `Stream.toList()`, no local type inference. Standard `switch`
  statements, lambdas, method refs and Java 8 streams are fine. `Map.getOrDefault`,
  `computeIfAbsent`, `EnumMap` are fine (Java 8).
- **Zero external libraries.** Only `javax.swing`, `java.awt`, `java.util`, `java.io`,
  `java.text`, `java.lang`. No Maven/Gradle plugins beyond the jar plugin already in
  `oggalaxy/pom.xml`. **No downloaded art** — every visual is drawn with `Graphics2D`
  (shapes, gradients, polygons) or is public-domain/self-drawn.
- **Package root:** `com.whim.oggalaxy` — **Source root:** `oggalaxy/src/`
- Every task's code **must compile under `--release 8`** and import ONLY from `api`
  (`com.whim.oggalaxy.api` and `com.whim.oggalaxy.api.demo`) and its own sub-package.
  No task edits another task's sub-package or the `api` package.
- Simulation runs on a **background thread**; all Swing mutation happens on the EDT. The
  engine must never block the EDT.
- When done: **push your branch and `send_prompt` a short report back to the orchestrator
  task** (do NOT open a PR into main yourself; the orchestrator integrates).

## Package ownership

| Package | Owner | Contents |
|---|---|---|
| `com.whim.oggalaxy.api` | **Orchestrator (DONE — do not edit)** | `Ids`, `Cost`, `GameConfig`, `Formulas`, `Requirement`, `BuildingDef`, `TechDef`, `ShipDef`, `DefenseDef`, `ClassDef`, `Catalog`, `Views`, `Result`, `NewGameSetup`, `FleetOrder`, `GameController` |
| `com.whim.oggalaxy.api.demo` | **Orchestrator (DONE — do not edit)** | `DemoController` — a canned `GameController` the UI develops against |
| `com.whim.oggalaxy.model` `.engine` `.combat` `.ai` `.expedition` `.persistence` | **Task 1 — SIMULATION** | the real game engine; `engine.GameEngine implements api.GameController` |
| `com.whim.oggalaxy.ui` (+ sub-packages) | **Task 2 — UI** | every Swing screen; procedural Java2D art |
| `com.whim.oggalaxy.app` | **Orchestrator (final)** | `Main` — wires `GameEngine` + UI |

## The `api` contract (already committed — read it, DO NOT modify)

- **`Ids`** — every enum: `ResourceType` (METAL/CRYSTAL/DEUTERIUM/ENERGY/DARK_MATTER),
  `BuildingType` (17), `TechType` (15), `ShipType` (17 incl. LEVIATHAN & DEATHSTAR),
  `DefenseType` (8), `MissionType`, `PlayerClass` (EXPLORER/MINER/GENERAL),
  `Difficulty` (EASY/MEDIUM/HARD/RANDOM), `Phase`, `LogCategory`.
- **`Cost`** — immutable (metal, crystal, deuterium, energy). `plus`, `scale`, `structurePoints`.
- **`GameConfig`** — universe geometry (4 galaxies × 100 systems × 15 positions), the clock
  (`SECONDS_PER_TICK = 3600`, 1 tick = 1 in-game hour), `ECONOMY_SPEED`/`FLEET_SPEED = 6`,
  start resources/fields, combat tunables, debris/moon/plunder constants.
- **`Formulas`** — cost scaling, mine/solar/fusion production, storage capacity, build /
  research / ship-build time, flight time & distance & fuel, tech multipliers. USE THESE so
  UI and engine agree exactly.
- **`Catalog.standard()`** — the full static database of every `BuildingDef`, `TechDef`,
  `ShipDef` (stats + rapid-fire tables), `DefenseDef`, `ClassDef`. Numbers follow the OGame
  family; OG-Galaxy-specific items (LEVIATHAN, class bonuses) are labelled approximations.
- **`Views`** — read-only per-poll snapshot interfaces the UI consumes and the model
  implements: `ResourceView`, `QueueItemView`, `PlanetView`, `EmpireView`,
  `FleetMovementView`, `GalaxyCellView`, `LogEntryView`, `CombatReportView`,
  `ExpeditionReportView`, `GameStateView`. UI reads ONLY these + `Catalog`; never casts to a
  model class.
- **`GameController`** — the single UI↔engine seam (lifecycle, clock/speed, `advance(ticks)`,
  player commands returning `Result`, save/load, listeners). `state()` and `catalog()` are
  EDT-safe.
- **`api.demo.DemoController`** — a working canned controller. The UI builds & runs against
  `new DemoController()` from day one; `app.Main` swaps in the real `GameEngine` later.

**Data flow:** UI polls `controller.state()` on a Swing `javax.swing.Timer` (~4–10 Hz) and
repaints; input calls `GameController` methods. The engine mutates model state on its
background tick thread and publishes a fresh `GameStateView` atomically. UI never casts a
`*View` to a concrete class.

---

## Task 1 — SIMULATION (`engine.GameEngine implements api.GameController`)

Owns ALL mutable state and timing. Deliver these sub-packages:

### `model`
Serializable runtime classes implementing the `Views` interfaces:
- `ResourceStore` (impl `ResourceView`) — holds amounts; computes net production & energy
  ratio from a planet's buildings via `Formulas`.
- `Planet` (impl `PlanetView`) — coords, moon flag, temp, fields, building-level `EnumMap`,
  ship & defense `EnumMap`s, `ResourceStore`, current construction, shipyard queue.
- `Empire` (impl `EmpireView`) — human or AI; class; difficulty; tech `EnumMap`; planets;
  current research; score; alive flag; per-empire seeded `Random` for AI/moon rolls.
- `FleetMovement` (impl `FleetMovementView`) — owner, mission, origin/target coords, ships,
  cargo, depart/arrival/return ticks, returning flag.
- `LogEntry`, `CombatReport`, `ExpeditionReport` (impl their views).
- `GameState` (impl `GameStateView`) — tick, all empires, all fleets, logs, reports, selected
  planet, phase, master seed. Serializable graph = the save file.

### `engine`
- `GameEngine implements GameController`. Holds a `GameState`. Runs a `TickLoop` on a
  background `Thread` (or `ScheduledExecutorService`): each real interval applies `speed`
  ticks. `advance(n)` applies n ticks synchronously (for the "Advance Turn" button).
  Publish snapshots so `state()` is always safe to read from the EDT (swap a volatile
  reference, or synchronize). Deliver push events to listeners via
  `SwingUtilities.invokeLater`.
- Per-tick order: (1) production & storage caps & energy ratio per planet; (2) advance build
  / research / shipyard queues, completing finished jobs; (3) advance fleets, resolving
  arrivals (attack→combat, transport/deploy, colonize, recycle, espionage, expedition
  return); (4) run each AI empire's controller; (5) update scores; (6) check victory/defeat.
- Validate every player command (requirements met, resources available, fields free, fleet
  slots free) and return a descriptive `Result`. Charge resources on enqueue.
- Requirements: a build/research/ship is allowed only if all its `Requirement`s
  (building levels on that planet / empire tech levels) are met — read from `Catalog`.
- Fleet slots: `maxFleetSlots = 1 + COMPUTER_TECHNOLOGY level`.
- Colonies: max colonies = `1 + Formulas.maxColoniesFromAstro(astroLevel)` planets.

### `combat` — DETERMINISTIC combat (no RNG affects the outcome)
`CombatEngine.resolve(attackerFleet, defenderPlanet or defenderFleet, techs, classes)` →
`CombatReport`. Model (this is the authoritative spec — implement exactly):
- Each unit's **effective** stats: `attack = base.weapon * (1 + 0.1*weaponsTech) * ownerCombatBonus`,
  `shield = base.shield * (1 + 0.1*shieldTech)`, `hull = base.hull * (1 + 0.1*armourTech)`.
  Defender includes ships on the planet **and** defenses (defenses use the same model; shield
  domes are single units with huge shield/hull).
- Up to `GameConfig.COMBAT_MAX_ROUNDS` (6) rounds. Each round:
  1. All surviving units' shields regenerate to full.
  2. Using a **snapshot taken at the start of the round** (so both sides fire
     simultaneously), each side deals damage to the other:
     `damage(side) = Σ over firing stacks f: count_f * attack_f * rapidMult(f, enemyComposition)`
     where `rapidMult(f, enemy) = 1 + Σ over enemy type t: (enemyCount_t / enemyTotalCount) * max(0, rf(f→t) - 1)`
     (a deterministic expected-value model of OGame rapid fire; `rf` from `ShipDef`/defense tables).
  3. Distribute a side's `damage` across the enemy's surviving stacks in proportion to each
     stack's share of total enemy hull. For a stack of `n` units with per-unit `shield` and
     `hull`: `shieldAbsorbed = n * shield` (shields reset each round); `hullDamage = max(0, dmgToStack - shieldAbsorbed)`;
     `destroyed = min(n, floor(hullDamage / hull))`. Remove destroyed units.
  4. If either side has zero units, stop early.
- **Outcome:** attacker wins if defender has 0 units left and attacker has >0; defender wins if
  attacker has 0; otherwise draw (attacker retreats). Produce `roundSummaries` (one readable
  line per round) and loss maps.
- **Debris field:** `DEBRIS_FIELD_RATIO` (0.30) × (metal+crystal of ALL destroyed *ships*
  from both sides). Add to the target position's debris field (harvestable by RECYCLER/REAPER/
  PATHFINDER).
- **Plunder** (attacker win vs a planet only): loot `min(MAX_PLUNDER_FRACTION × each stored
  resource, total free cargo capacity of surviving attacker ships)`, split across resources.
- **Defenses rebuild:** deterministically restore `round(DEFENSE_REBUILD_CHANCE × destroyed)`
  of each destroyed defense type after a defence.
- **Moon creation** may use the empire/master **seeded** `Random` (reproducible, not affecting
  the battle result): probability `min(MOON_CHANCE_CAP, debris.structurePoints() * MOON_CHANCE_PER_DEBRIS)`.

### `expedition`
`ExpeditionEngine.resolve(fleet, expeditionBonus, seededRandom)` → `ExpeditionReport`.
Weighted outcome table (use the seeded `Random`; larger/stronger escorts shift odds toward
good outcomes and cap find sizes). Outcomes: **Resources** (metal/crystal/deut scaled by fleet
cargo & `expeditionBonus`), **Dark Matter**, **Fleet found** (gain ships), **Nothing**,
**Delay** (fleet returns later), **Pirates/Aliens** (small combat → possible ship losses),
**Black hole** (rare total loss). Document the table in a `RESEARCH.md` under `expedition`.

### `ai` — `AIController` per difficulty
Runs each tick for each AI empire, off the EDT (it's inside the engine tick). Same rules as
the player. Behaviour:
- **EASY:** economy-only, slow. Builds mines/solar in a fixed order, minimal tech, tiny
  defense, essentially never attacks. Low decision frequency.
- **MEDIUM:** balanced. Grows economy, some `RESEARCH_LAB`/`WEAPONS/SHIELD/ARMOUR`, builds a
  modest fleet, occasionally scouts and attacks a clearly weaker neighbour (including the
  player) when its fleet strength exceeds the target's estimated defense.
- **HARD:** aggressive & efficient. Prioritises an optimised mine/tech order, keeps energy
  positive, expands via colony ships, maintains a strong mixed fleet, actively scouts and
  attacks weaker empires (player and other AIs) opportunistically, harvests resulting debris.
- **RANDOM:** resolved to one of the above at game creation (log which). Keep it fixed per game.

### `persistence`
`SaveLoad.save(File, GameState)` / `load(File)` via `ObjectOutputStream`/`ObjectInputStream`.
The whole `GameState` graph is `Serializable`. `Catalog` is rebuilt via `Catalog.standard()`
on load (don't serialize it). Provide an `EngineSelfCheck` `main` that creates a game, runs
~50 ticks, dispatches a fleet, resolves a battle, saves & loads, and prints a summary — so the
engine is verifiable headless without the UI.

**Task 1 develops fully headless.** Do not depend on any `ui` class.

---

## Task 2 — UI (all Swing screens; procedural Java2D art)

Reads ONLY `api` + `api.demo`. Build & run against `new com.whim.oggalaxy.api.demo.DemoController()`.
Never cast a `*View` to a model class. Poll `controller.state()` on a `javax.swing.Timer`.

Deliver:
- `ui.Palette` + `ui.SpaceArt` — Java2D helpers: starfield background, procedural planet
  (seeded by coords), ship/defense glyphs per `ShipType`/`DefenseType`, resource icons.
- `ui.StartScreen` — commander name field; opponent-count spinner (1–7); per-opponent
  difficulty selector (Easy/Medium/Hard/Random); player-class chooser (Explorer/Miner/General);
  "Start Game" that validates (non-empty name, count in range) then builds a `NewGameSetup` and
  calls `controller.newGame(...)`. Show the resolved difficulties in the log.
- `ui.MainFrame` — top resource/status bar (metal/crystal/deut/energy/dark matter, production
  rates, current tick, speed controls: Pause / Advance Turn / speed selector), a left planet
  selector, a center tabbed area, and a bottom **log/event panel** (colour-coded by
  `LogCategory`).
- Center tabs / screens:
  - **Overview** — selected planet: procedural planet art, fields, temperature, resource
    detail, current construction with progress bar.
  - **Buildings** — grid of `BuildingType` with level, next-level cost (via `Catalog`+`Formulas`),
    requirements state, and a build button (disabled + reason when not allowed).
  - **Research** — `TechType` tree with levels, costs, requirements, in-progress research bar.
  - **Shipyard** — build ships & defenses (count spinner), queue display, current fleet on planet.
  - **Galaxy** — galaxy/system navigator (spinners for galaxy & system) showing the 15
    positions with owner/planet/debris/moon; select a target and open the dispatch dialog.
  - **Fleet dispatch dialog** — choose mission (attack/transport/deploy/colonize/expedition/
    recycle/espionage), pick ships (spinners), cargo (metal/crystal/deut), speed %, then
    `controller.dispatchFleet(order)`. Also a fleet-movements list with recall.
  - **Combat report / battle log** — list of `CombatReportView`s with per-round detail, losses,
    debris, plunder, moon; and `ExpeditionReportView`s.
- Everything on the EDT; long nothing — the controller does the work. Provide a `UiPreview`
  `main` that launches `MainFrame` on `DemoController` so the UI is runnable standalone.

**Style:** dark space theme, readable, functional mirror of the real game's layout. Keep it
clean and legible over flashy.

---

## Integration (orchestrator, after both tasks report)

`app.Main` constructs the real `engine.GameEngine`, shows `ui.StartScreen`, and on start hands
the engine to `ui.MainFrame`. Orchestrator compiles the whole tree under `--release 8`, runs
`EngineSelfCheck`, fixes any seam mismatches, and commits.

## Reporting protocol

Push your branch. Then `send_prompt` the orchestrator task a short report: what you built,
how you verified (compile + self-check), and any seam notes. Do not open a PR.
