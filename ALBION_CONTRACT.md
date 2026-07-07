# Albion (1995) Recreation — Build Contract

A standalone, **Java 8**, zero-dependency, procedural-graphics **Swing** clean-room
recreation of the Blue Byte RPG **Albion (1995)**: a 2D top-down overworld, grid-based
**first-person** dungeons, turn-based tactical grid combat, topic-based dialogue, a
party with stats/skills/leveling, inventory/equipment, and a multi-school magic system.

Three parallel tasks build against the shared `com.whim.albion.api` package (already
committed by the orchestrator). **This file is the single source of truth for the seams
between them.** Everyone compiles against `api` ONLY — no task imports another task's
concrete classes.

## Clean-room / legal constraints (ALL tasks)

- **No copyrighted Albion (1995) assets or text.** No original sprites, maps, music,
  story dialogue, character names, or place names copied. Mechanics, formulas, UI
  layout patterns, and stat systems are fair game (not copyrightable).
- All visuals are **procedural Java2D** (shapes, gradients, strokes, colors) keyed by
  string `spriteKey`/`decorKey`/`portraitKey`. No image/audio files on disk.
- Invent your own flavor names for NPCs, items, places, and quests. Magic school names
  are the generic `PSIONIC / DESTRUCTION / NATURE / RESTORATION`.

## Hard technical constraints (ALL tasks)

- **Java 8 ONLY.** No `var`, no switch expressions, no text blocks, no records, no
  `List.of`/`Map.of`, no `Stream.toList()`, no local type inference. `maven.compiler
  .source/target = 1.8`.
- **Zero external libraries.** Only `javax.swing`, `java.awt`, `java.util`, `java.io`,
  `java.util.*`. JUnit allowed for tests only.
- **Package root:** `com.whim.albion`  •  **Source root:** `albion/src/`
- Must **compile** from the `albion/` dir: `mvn -q compile`.
- Each task imports ONLY from `com.whim.albion.api` and its own sub-package(s). Do NOT
  edit `api` or another task's package. If you believe `api` is missing something,
  **report the exact interface change back to the orchestrator via `send_prompt` — do
  not edit `api` yourself.**
- Do **not** open a pull request. Push your branch and report back to the orchestrator.

## The `api` package (already committed — read it, do NOT modify)

Read every file under `albion/src/com/whim/albion/api/`. Key types:

- `Enums` — `GameStateType, MapType, TileType, Direction(dx/dy/left/right/opposite),
  StatType, SkillType, DamageType, SpellSchool, EquipSlot, ItemType, TargetType,
  CombatActionType, EnemyBehaviorType, SpellEffectType, QuestStatus`.
- `GridPos` — immutable (x,y) with `translate`, `manhattan`, equals/hashCode.
- `ActionResult` — `ok()/ok(msg)/fail(msg)`, `isSuccess()`, `message()`.
- `Defs` — immutable content templates with builders: `ItemDef`, `SpellDef`,
  `MonsterDef`, `MapMeta`.
- `Views` — read-only projections for the UI: `GameStateView` (root), `WorldView`,
  `TileView`, `PlayerView`, `NpcView`, `PartyView`, `CharacterView`, `ItemView`,
  `SpellView`, `CombatView`, `CombatantView`, `DialogueView`, `JournalView`,
  `QuestEntryView`.
- `GameController` — THE UI↔engine seam (state + input intents + `ChangeListener`).
- Model seams (engine↔model): `Combatant`, `WorldModel` (extends WorldView),
  `PartyModel` (extends PartyView), `Content` (+ `DialogueTree`), `GameContext`,
  `GameModel` (+ `JournalModel`), `ModelFactory`.

## Package ownership

| Package(s) | Owner | Responsibility |
|---|---|---|
| `com.whim.albion.api` | **Orchestrator (DONE — do not edit)** | All seams above |
| `com.whim.albion.world`, `.entities`, `.items`, `.magic`, `.data` | **Task 1 — Model & Content** | The whole model + content |
| `com.whim.albion.engine`, `.combat`, `.dialogue`, `.persistence` | **Task 2 — Engine & Systems** | Game flow, combat, dialogue runner, save/load |
| `com.whim.albion.ui` | **Task 3 — UI & Rendering** | Swing frame, renderers, all panels |
| `com.whim.albion.app` | **Orchestrator (final)** | `Main` wires Task1 factory + Task2 engine + Task3 frame |

---

## Task 1 — Model & Content  (`world`, `entities`, `items`, `magic`, `data`)

**Goal:** implement the entire mutable model + a content pack, exposed purely through
`api` interfaces so the engine and UI never see your concrete classes.

**Implement these `api` interfaces:**
- `ModelFactory` → concrete `AlbionModelFactory` with a **public no-arg constructor**.
  `newGame(seed)` returns a fully-populated `GameModel`.
- `GameModel` + `GameModel.JournalModel`, `WorldModel`, `PartyModel`, `Content` +
  `Content.DialogueTree`, and `Combatant` (adapters for party members AND spawned
  enemies).
- Read-only views produced by your model: `CharacterView`, `ItemView`, `SpellView`,
  `PartyView` (via `PartyModel`), `WorldView`/`TileView`/`PlayerView`/`NpcView` (via
  `WorldModel`), `JournalView` (via `JournalModel`).

**Entities:** `Character` with the 8 `StatType`s, 4 `SkillType`s, level/xp, LP/SP,
5 `EquipSlot`s, a backpack (slot list), known spells, a profession label. Implement
XP→level curve and stat/LP/SP growth on level-up. `awardXp` distributes to all living
members. Provide a starting party of **3–4 members** with distinct professions
(e.g. warrior, ranger, healer, mage) and at least two different magic schools.

**Items/economy:** `Inventory` (slot-limited pack + equip slots), equip/unequip/use
resolving stat & LP/SP effects; gold. Author ~10 `ItemDef`s (weapons, armor, shield,
helmet, potions, a scroll, a key, a quest item).

**Magic:** `SpellBook` per caster; `canCast(school)` gated by profession; author ~8
`SpellDef`s spread over the 4 schools (damage, heal, buff, utility) with `spCost`,
`levelReq`, `talentReq`.

**World:** uniform `TileMap` grid model with `MapType`. Author at least:
- one **OUTDOOR_2D** town map (with NPCs, a shop-keeper NPC, a chest, a dungeon
  entrance transition), and
- one **INDOOR_3D** dungeon map (walls/floor/door/stairs, one combat encounter cell,
  a locked door needing the key, treasure).
Implement `stepPlayer`, `turnPlayer`, `loadMap`, `transitionAt`, `interactableAt`,
`encounterAt`/`clearEncounter`. Tiles carry `walkable`/`blocksSight` (the UI's
first-person renderer relies on `blocksSight` + `TileType`).

**Content registry:** `Content.item/spell/monster` lookups, `spawnEncounter(id)`
returning fresh `Combatant` enemies (author 2–3 `MonsterDef`s with the 3
`EnemyBehaviorType`s), and `dialogue(id)` returning a navigable `DialogueTree` whose
options mutate state via `GameContext` (start a quest, give the key, open the shop
flag). Author **one quest** ("recover X from the dungeon") wired through dialogue +
a dungeon interactable + `JournalModel`.

**Combatant adapters:** party members and enemies both implement `Combatant`
(`takeDamage` applies `defense` mitigation + `defending` bonus; `attackPower`/`ranged`
from equipped weapon or monster def).

**Deliver:** compiles via `mvn -q compile`; a tiny `main` or JUnit smoke test proving
`newGame(0)` yields a party, a loadable town map, a spawnable encounter, and a dialogue
tree. Add a `docs/task1-notes.md` on design choices + assumptions.

---

## Task 2 — Engine & Systems  (`engine`, `combat`, `dialogue`, `persistence`)

**Goal:** the game brain. Own the state machine and all flow; drive Task 1's model
through `api`. **Depend on `api` ONLY** — get the model via a `ModelFactory` injected
in your constructor: `public GameEngine(ModelFactory factory)`.

**Implement `GameController` as `GameEngine`:**
- Hold current `GameStateType`; `state()` returns a `GameStateView` snapshot wiring:
  `world()`→`model.world()` (null unless OVERWORLD/DUNGEON), `party()`→`model.party()`,
  `journal()`→`model.journal()`, `combat()`/`dialogue()` → your own view objects,
  `gold()`→party gold, `menuOptions()` for TITLE/MENU/GAME_OVER.
- `newGame(seed)` calls `factory.newGame(seed)`, sets state to OVERWORLD (or TITLE→
  first map). `move/moveTo/interact` drive `WorldModel`; stepping onto a transition
  loads the next map; stepping onto an `encounterAt` starts combat; `interact()` opens
  dialogue or loots an `Interactable`.
- Fire `ChangeListener.onStateChanged()` after every mutation.

**Combat (`combat`):** tactical grid (suggest 6 cols × 5 rows; party bottom rows,
enemies top). Build combatants from `model.party().asCombatants()` +
`content.spawnEncounter(id)`. Initiative ordered by `StatType.SPEED` each round.
Resolve `CombatActionType`: `ATTACK` (melee needs adjacency/front, ranged any),
`CAST` (spend SP, apply `SpellDef` effect by `TargetType`), `ITEM`, `MOVE` (reposition),
`DEFEND` (set defending), `FLEE` (chance by SPEED). Enemy turns use `EnemyBehaviorType`
(`AGGRESSIVE` close+strike weakest reachable, `RANGED` hold + target lowest LP,
`SUPPORT` heal/buff allies). Provide `CombatView`/`CombatantView`. On victory: award
xp/gold/loot via `PartyModel`, return to prior map; on party wipe: `GAME_OVER`.
Formulas (document them): hit% from attacker skill/DEX vs defender DEX; damage =
attackPower + STR mod − defense (min 1); crit via CRITICAL skill + LUCK.

**Dialogue (`dialogue`):** run a `Content.DialogueTree` — track current node, expose
`DialogueView`, `selectDialogueOption(i)` applies the option's `GameContext` effects and
advances (or ends). Implement `GameContext` here (flags, gold, items via `PartyModel`,
quests via `JournalModel`, `startCombat`, `teleport`, `notify`).

**Persistence (`persistence`):** save/load the model + flags/quests to disk under a
saves dir using `java.io` serialization or a hand-rolled text format (no libraries).
`saveSlots()/saveGame(slot)/loadGame(slot)`.

**Deliver:** compiles via `mvn -q compile`; a `StubModelFactory`/JUnit test (or reuse
Task 1's once merged) exercising a full loop: new game → move → trigger combat → win →
dialogue → save → load. `docs/task2-notes.md` with formulas + assumptions.

> Until Task 1 lands you can test against a minimal in-package fake `ModelFactory`.
> The production wiring uses Task 1's `AlbionModelFactory`.

---

## Task 3 — UI & Rendering  (`ui`)

**Goal:** the Swing shell. Talk to `api.GameController` + `Views` **only**. Ship a dev
`StubController implements GameController` returning hand-built fake state so the UI
runs standalone before the engine lands.

**Build:**
- `GameFrame` (JFrame, fixed logical size e.g. 960×640, `BorderLayout`) hosting a
  `CardLayout`/state-switch over `GameStateType`.
- Regions per the design: CENTER **viewport**, EAST **party portraits** (LP/SP bars) +
  **minimap/compass**, SOUTH **action bar** (Look/Use/Talk buttons) + status/gold.
- `TopDownRenderer` (JPanel + Graphics2D) for `MapType.OUTDOOR_2D`: draw tiles by
  `TileType`/`decorKey`, NPCs by `spriteKey`, player by `facing`. Mouse click →
  `controller.moveTo(x,y)`; arrows/WASD → `controller.move(dir)`.
- `FirstPersonRenderer` for `MapType.INDOOR_3D`: grid-based pseudo-3D corridor view
  from `player()` position+facing using `tileAt`/`blocksSight` (draw receding wall
  quads for a few depth steps + left/right walls; simple shaded polygons, no textures).
  ↑/↓ step, ←/→ turn.
- `CombatPanel`: render the battlefield grid + combatants from `CombatView`; action
  buttons for `availableActions()`; click a target combatant/cell; show the log.
- `DialoguePanel`: speaker + text + numbered option buttons → `selectDialogueOption`.
- `InventoryPanel`, `CharacterSheetPanel`, `JournalPanel`, `TitleScreen`, and a
  save/load `MenuPanel` driven by `menuOptions()`/`saveSlots()`.
- A small procedural-art helper (`SpriteFactory`) mapping `spriteKey`→Graphics2D
  drawing so keys resolve consistently.
- Register as `controller.addChangeListener(...)` and repaint on the EDT.

**Deliver:** `GameFrame` + `StubController` run via a temporary `UiMain` you can delete
later (the real entry is the orchestrator's `app.Main`). `mvn -q compile` clean.
`docs/task3-notes.md` on layout + the first-person projection approach.

---

## Integration (orchestrator)

After all three land: merge branches, write `com.whim.albion.app.Main` wiring
`new GameEngine(new AlbionModelFactory())` into `new GameFrame(engine)`, `mvn -q compile`
the whole tree, run the vertical slice (title → town → dialogue/quest → dungeon →
combat → save/load), and update the root `README.md` with build/run instructions.
