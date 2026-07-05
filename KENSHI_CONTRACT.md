# Kenshi (demake) — Build Contract

A standalone, **Java 8**, zero-dependency, procedural-graphics Swing demake of the
sandbox RPG **Kenshi**: a 2D top-down, real-time-with-pause squad game with a
seven-part limb-damage system, hunger/blood survival, faction AI, and a pannable
zoomable camera. Three parallel tasks build against the shared `api` package.
**This file is the single source of truth for the seams between them.**

## Hard constraints (ALL tasks)

- **Java 8 ONLY.** No `var`, no switch expressions, no text blocks, no records, no
  `List.of`/`Map.of`, no `Stream.toList()`, no local type inference. Standard
  `switch` statements and Java 8 streams/lambdas are fine. `source/target = 1.8`.
- **Zero external libraries.** Only `javax.swing`, `java.awt`, `java.util`,
  `java.io`, `java.lang`, `java.util.concurrent`. No Maven/Gradle plugins beyond
  the jar plugin already in `kenshi/pom.xml`. **No downloaded assets** — every
  visual is drawn with `Graphics2D` (shapes, polygons, gradients) or generated
  procedurally.
- **Package root:** `com.whim.kenshi` — **Source root:** `kenshi/src/`
- Every task's code **must compile** and import ONLY from `api` (and its own
  sub-package). **No task edits another task's sub-package** or the `api` package.
- Simulation runs at `Config.TICK_HZ` (20 Hz) on a **background thread**, fully
  decoupled from the Swing repaint timer.
- When done: **push your branch and `send_prompt` a short report back to the
  orchestrator task** (do NOT open a PR into main yourself).

## Package ownership

| Package | Owner | Contents |
|---|---|---|
| `com.whim.kenshi.api` | **Orchestrator (DONE — read only, do not edit)** | `Config`, `Enums`, `Views`, `GameController` |
| `com.whim.kenshi.domain` | **Task 1** | `Anatomy`, `Skills`, `Character`, `Squad`, `FactionMatrix`, `WorldNode`, `MapGrid`, `WorldState`, `WorldBuilder`, `RESEARCH.md`, `DomainSelfCheck` |
| `com.whim.kenshi.engine` | **Task 2** | `GameEngine` (implements `api.GameController`), `TickLoop`, `CommandQueue`, `Pathfinder`, `CharacterAI`, `CombatSystem`, `SurvivalSystem`, `SkillSystem`, `Snapshot` (view impls), `EngineSelfCheck` |
| `com.whim.kenshi.ui` | **Task 3** | `GameFrame`, `WorldPanel`, `Renderer`, `Camera`, `Hud`, `BodyChart`, `InputHandler`, `Palette`, `StubController` (dev only), `UiPreview` |
| `com.whim.kenshi.app` | **Orchestrator (final)** | `Main` — wires engine + ui |

## The `api` package (already committed — read it, DO NOT modify)

- **`Config`** — world constants. Key values: `TICK_HZ=20`, `DT`,
  `WORLD_SECONDS_PER_TICK=6`, map `MAP_TILES=96` × `TILE_SIZE=64` →
  `WORLD_SIZE`, viewport `VIEW_W=1000`/`VIEW_H=680`, zoom range, `CHAR_RADIUS=12`,
  `BASE_MOVE_SPEED`, `CRAWL_SPEED_MULT`, `MELEE_RANGE=22`, anatomy maxima
  (`TORSO_PART_MAX`, `LIMB_PART_MAX`), survival (`HUNGER_MAX`, `BLOOD_MAX`,
  `BLOOD_UNCONSCIOUS_AT`, decay/regen rates), combat (`BASE_HIT_CHANCE`,
  `HIT_CHANCE_PER_SKILL`, `BLEED_FROM_DAMAGE`), skills (`SKILL_MIN=1`,
  `SKILL_MAX=100`). **All coordinates are world units; (x,y) = character CENTER.**
- **`Enums`** — `BodyPart{HEAD,CHEST,STOMACH,LEFT_ARM,RIGHT_ARM,LEFT_LEG,RIGHT_LEG}`
  (each `label()`, `vital()`, `isArm()`, `isLeg()`; iteration order is canonical
  for the HUD), `SkillType{TOUGHNESS,ATHLETICS,STRENGTH,DEXTERITY,MELEE_ATTACK,
  MELEE_DEFENCE}`, `MoveState{IDLE,MOVING,CRAWLING,DOWNED,DEAD}`,
  `WeaponClass{UNARMED,ONE_HANDED,TWO_HANDED}`, `FactionId{PLAYER,HOLY_NATION,
  SHEK,DUST_BANDITS,HUNGRY_BANDITS,TRADE_GUILD,DRIFTERS}` (each `label()`),
  `Relation{ALLY,NEUTRAL,HOSTILE}`, `AiState{IDLE,WANDER,PATROL,PURSUE,ATTACK,
  FLEE,LOOT,RETURN}`, `Phase{RUNNING,PAUSED,GAME_OVER}`,
  `Terrain{SAND,SCRUB,GREEN,ROCK,ASH,WATER,TOWN}`,
  `NodeType{TOWN,BAR,SHOP,CAMP,RUIN}`, `OrderType{NONE,MOVE,ATTACK,INTERACT}`.
- **`Views`** — read-only per-frame snapshot interfaces: `CharacterView` (id,
  name, faction, x/y/heading, moveState, aiState, selected, playerControlled,
  alive, downed, `partHp/partMax/partDisabled(BodyPart)`, hunger, blood,
  bleedRate, weapon, `skill(SkillType)`, orderType, targetId), `SquadView`,
  `FactionView` (`relationTo`, `reputationWithPlayer`), `NodeView`, `MapView`
  (`terrain(col,row)`), `LogView`, `GameStateView` (phase, tick, worldSeconds,
  gameSpeed, map, characters, squads, factions, nodes, selectedIds, log).
  **UI reads ONLY these and never casts to a concrete class.**
- **`GameController`** — the single UI↔engine seam: `state()`, lifecycle
  `newGame(seed)/start()/stop()`, `setPaused/isPaused/togglePause`,
  `setGameSpeed(int)`, `setSelection(ids)`, and orders `orderMove(ids,x,y)`,
  `orderAttack(ids,targetId)`, `orderInteract(ids,nodeId)`.

**Data flow:** the UI polls `controller.state()` on a Swing `javax.swing.Timer`
(~30–60 fps) and repaints; user input calls `GameController` methods. The engine
mutates domain state on its background tick thread; `state()` returns a coherent
snapshot. The UI never casts a `*View` to a concrete domain/engine class, and the
engine never touches Swing.

---

## Core Kenshi mechanics (the ruleset all tasks implement)

These are the demake's canonical rules. Where Kenshi's exact numbers are unknown
we pick coherent, self-consistent values (documented in Task 1's `RESEARCH.md`).

1. **Anatomy (7 parts).** Every character tracks independent HP for Head, Chest,
   Stomach, Left/Right Arm, Left/Right Leg. Parts start at max and can fall to
   `-max` (the floor). A part is **disabled** at `hp <= 0`.
2. **Damage consequences.**
   - Both legs disabled → `MoveState.CRAWLING` (move speed × `CRAWL_SPEED_MULT`).
   - An arm disabled → cannot use `TWO_HANDED` weapons (effective `UNARMED`).
   - A **vital** part (Head or Chest) hitting the floor → **DEAD**.
   - Stomach disabled or blood `< BLOOD_UNCONSCIOUS_AT` → **DOWNED** (unconscious
     but alive; wakes as parts recover and blood rises).
3. **Survival.** Hunger decays at `HUNGER_DECAY_PER_SEC` (world seconds). At zero
   hunger, characters take slow starvation damage. Bleeding drains blood at
   `bleedRate`; below `BLOOD_UNCONSCIOUS_AT` they go DOWNED, at 0 blood they die.
   When fed and not in combat, parts heal (`HEAL_PER_SEC`) and blood regens.
4. **Combat.** Melee within `MELEE_RANGE`. `hitChance = clamp(BASE_HIT_CHANCE +
   HIT_CHANCE_PER_SKILL·(attackerMeleeAttack − defenderMeleeDefence),
   MIN_HIT_CHANCE, MAX_HIT_CHANCE)`. On a hit, damage is allocated to ONE body
   part chosen by weighted random (torso parts weighted higher than limbs), and a
   fraction (`BLEED_FROM_DAMAGE`) becomes added bleed rate on the victim.
5. **Skills.** 1..100. Using an action trains its skill; taking damage trains
   Toughness; moving trains Athletics. Higher skill → better hit/defence/speed.
6. **Factions.** A `FactionMatrix` holds pairwise `Relation` and a player
   reputation per faction. Hostiles attack on sight within aggro range.
7. **RTSP.** Player selects units and issues MOVE/ATTACK/INTERACT orders; the
   world can be paused at any time and orders can be queued while paused.

---

## Task 1 — Domain (`com.whim.kenshi.domain`)

Model the world state; **no threading, no Swing, no AI/combat logic** (that is
Task 2). Provide clean, mutable domain classes plus read helpers.

Deliverables:
- **`Anatomy`** — a `Map<BodyPart,Double>` current-HP store with per-part max;
  methods: `hp(part)`, `max(part)`, `damage(part, amount)` (clamps to `-max`),
  `heal(part, amount)` (clamps to max), `disabled(part)`, `bothLegsDown()`,
  `anyArmDown()`, `isDead()` (a vital part at floor), `isDowned(bloodLow)`.
- **`Skills`** — `SkillType`→level (1..100) plus fractional XP; `level(skill)`,
  `addXp(skill, amount)`.
- **`Character`** — id, name, `FactionId`, position (`x,y`), heading,
  `WeaponClass`, `Anatomy`, `Skills`, hunger, blood, bleedRate, current order
  (`OrderType`, target position, target id, node id), `AiState`, selection flag.
  Derives `effectiveWeapon()` (down-grades to UNARMED if an arm is disabled),
  `moveState()` from anatomy+blood. **No behaviour that belongs to the engine** —
  just state + derivations that Views need.
- **`Squad`** — id, name, faction, member character ids.
- **`FactionMatrix`** — pairwise `Relation` + `reputationWithPlayer(faction)`;
  seed sensible defaults (Holy Nation vs Shek hostile, bandits hostile to all,
  etc.). Provide `isHostile(a,b)`.
- **`WorldNode`** — id, name, `NodeType`, owner faction, x, y, radius.
- **`MapGrid`** — `MAP_TILES²` `Terrain` grid with `terrain(col,row)`; procedural
  generation from a seed (deserts, green belts, rock, a couple of town patches).
- **`WorldState`** — aggregates `MapGrid`, all `Character`s (by id), `Squad`s,
  `FactionMatrix`, `WorldNode`s, plus tick/worldSeconds counters and an event log
  ring buffer. Lookup helpers: `character(id)`, `charactersList()`, etc.
- **`WorldBuilder`** — `build(long seed)` → a populated `WorldState`: a 3-member
  PLAYER squad near a starting town, several bandit squads, town guards, wandering
  drifters, and the map's towns/bars.
- **`RESEARCH.md`** — the mechanics + chosen constants you implemented.
- **`DomainSelfCheck`** — a `public static void main` that builds a world, applies
  sample damage, and prints anatomy/moveState transitions to prove the model.

Task 1 must NOT depend on engine or ui. Task 2/3 depend on Task 1.

## Task 2 — Engine (`com.whim.kenshi.engine`) — high-reasoning model

Own the simulation. Implement `api.GameController`.

Deliverables:
- **`GameEngine implements GameController`** — holds the `WorldState`; `state()`
  returns a coherent `GameStateView` snapshot (see `Snapshot`); command methods
  enqueue onto a thread-safe `CommandQueue` applied at the top of each tick.
- **`TickLoop`** — a background `Thread` (or `ScheduledExecutorService`) ticking at
  `Config.TICK_HZ`, honouring pause and `gameSpeed` (run N sub-steps per tick).
  Never touches Swing.
- **`Pathfinder`** — grid A* (or greedy + obstacle avoidance) over `MapGrid`;
  water is impassable. Return waypoints in world units.
- **`CharacterAI`** — per-character state machine over `AiState`: bandits WANDER
  then PURSUE/ATTACK hostiles in aggro range, FLEE when badly hurt; guards PATROL;
  drifters WANDER; downed/dead characters are skipped. Player units follow orders,
  auto-attacking hostiles in range when idle.
- **`CombatSystem`** — resolve melee per the hit-chance formula; allocate damage to
  a weighted-random body part; apply bleed; award skill XP (attacker MELEE_ATTACK,
  defender MELEE_DEFENCE + Toughness). Emit log lines ("X hits Y in the Left Leg").
- **`SurvivalSystem`** — per world-second: hunger decay, bleed drain, starvation
  damage, healing/blood regen when safe; recompute DOWNED/DEAD.
- **`SkillSystem`** — XP → level curve (1..100), Athletics from distance moved.
- **`Snapshot`** — immutable implementations of every `Views.*` interface, built
  once per `state()` call from the live `WorldState` so the EDT reads stable data.
- **`EngineSelfCheck`** — `main` that runs a headless world for a few seconds and
  prints a summary (alive counts, a sample fight) to prove the loop + combat.

Task 2 imports `api` + `domain` only. Do NOT touch `ui`.

## Task 3 — UI (`com.whim.kenshi.ui`) — Swing presentation

Own everything visual. Read the world **only** through `GameController.state()`
and the `Views` interfaces. Never import `engine` or `domain`.

Deliverables:
- **`Camera`** — world↔screen transform with pan + zoom (`Config.MIN/MAX_ZOOM`);
  `worldToScreen`, `screenToWorld`, clamp to world bounds.
- **`WorldPanel`** (a `JPanel`) — repainted by a `javax.swing.Timer` (~30–60 fps);
  polls `controller.state()`, draws terrain tiles, world nodes, and every
  character via `Renderer`. Draws the drag-select rectangle and order feedback.
- **`Renderer`** — `Graphics2D` primitives only: terrain as tinted tiles, towns as
  labelled patches, characters as faction-coloured circles with a heading wedge,
  a selection ring, a tiny floating health pip, and DOWNED/DEAD/CRAWLING states
  visibly distinct (e.g. prone marker, greyed, red X).
- **`BodyChart`** — the bottom-left panel: the selected character's **7 body-part
  bars** in canonical `BodyPart` order, colour-graded by fraction, greyed when
  disabled, plus Hunger and Blood bars and a bleed indicator.
- **`Hud`** — bottom-center squad portraits (one chip per player unit, click to
  select, showing worst-part colour + downed state), top bar with game
  speed/pause + clock + selected faction reputation readout, and a scrolling
  event log corner.
- **`InputHandler`** — left-click select, left-drag box-select (calls
  `setSelection`), right-click context: ground→`orderMove`, hostile
  character→`orderAttack`, node→`orderInteract`; mouse-wheel zoom, arrow/edge pan,
  Space pause, 1/2/4 speed.
- **`Palette`** — central `Color` constants (faction colours, terrain tints, HUD).
- **`StubController implements GameController`** — a dev-only fake with a handful of
  hand-placed characters and a trivial drift so the UI runs standalone before the
  engine lands. **Not used by `Main`.**
- **`UiPreview`** — `main` that launches the UI against `StubController`.

Task 3 imports `api` only. Build/verify against `StubController`.

---

## Integration (orchestrator, after all three report)

`com.whim.kenshi.app.Main`: construct `GameEngine`, `newGame(seed)`, hand it to
the UI (`GameFrame(controller)`), `start()`. Review each child PR against this
contract, merge into the orchestrator branch, compile the whole module with
`mvn -q -f kenshi/pom.xml clean package`, run `DomainSelfCheck` + `EngineSelfCheck`
headless, launch once, then open the final PR into `main`.
