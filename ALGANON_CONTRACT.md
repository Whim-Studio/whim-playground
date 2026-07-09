# Alganon (single-player reimagining) — Build Contract

A standalone, **Java 8**, zero-dependency, procedural-graphics **Swing** single-player
reimagining of the MMORPG **Alganon (2009)**: a guided race → family → class character
creation wizard, six classes with stances/schools/resources, top-down zone exploration,
real-time-with-cooldowns combat, static + procedural quests, an offline **"Study"**
progression system, gather → process → craft tradeskills with an auction-house vendor,
and a **simulated background faction war** replacing PvP.

Three parallel tasks build against the shared **`com.whim.alganon.api`** package (already
committed by the orchestrator). **This file is the single source of truth for the seams
between them.** Every task compiles against `api` ONLY — no task imports another task's
concrete classes. The authoritative design/scope spec is **`/app/DESIGN.md`** (read it).

## Clean-room / legal constraints (ALL tasks)

- **No copyrighted Alganon assets or text.** Class/race/family/school names may mirror
  Alganon terminology for authenticity, but **all descriptive/flavor text must be written
  fresh in your own words** — do not reproduce Alganon marketing copy or lore verbatim.
- All visuals are **procedural Java2D** (shapes, gradients, colors) keyed by string
  `spriteKey`. No image/audio files on disk. Sound is stub hooks only.
- Any gameplay rule not anchored in DESIGN.md is your own design decision — label such
  decisions in a code comment where they materially drive a system (per DESIGN.md).

## Hard technical constraints (ALL tasks)

- **Java 8 ONLY.** No `var`, no switch expressions, no text blocks, no records, no
  `List.of`/`Map.of`, no `Stream.toList()`, no local type inference. `maven.compiler
  .source/target = 1.8`.
- **Zero external libraries.** Only `javax.swing`, `java.awt`, `java.util`, `java.io`,
  `java.text`. JUnit allowed for tests only.
- **Package root:** `com.whim.alganon`  •  **Source root:** `alganon/src/`
- Must **compile from `alganon/`**: `mvn -q compile`.
- Import ONLY from `com.whim.alganon.api` and your own sub-package(s). Do NOT edit `api`
  or another task's package. **If you believe `api` needs a change, report the exact
  change back to the orchestrator via `send_prompt` — do not edit `api` yourself.**
- Push your branch and **report back to the orchestrator via `send_prompt`. Do NOT open a
  pull request.**

## The `api` package (already committed — read every file, do NOT modify)

Under `alganon/src/com/whim/alganon/api/`:

- `Enums` — GameStateType, Faction, FamilyArchetype, ClassId, ResourceType, Stance,
  School, StatType, SkillType, DamageType, TargetType, AbilityKind, EquipSlot, ItemType,
  TileType, MobBehavior, QuestStatus, ObjectiveType, ChatChannel, Direction, ControlState.
- `GridPos`, `ActionResult` — value types.
- `Defs` — immutable content templates: RaceDef, FamilyDef, ClassDef, AbilityDef, ItemDef,
  MobDef, LootDrop, ObjectiveDef, QuestDef, RecipeDef, GatherNodeDef, ZoneMeta.
- `Views` — read-only UI projections rooted at `GameStateView` (CharacterView, AbilityView,
  ItemView, WorldView, NpcView/MobView/GatherView/PortalView, FactionWarView, CombatView,
  QuestView, StudyView, CraftingView, AuctionView, FamilyView, ChatLineView, CreationView).
- `GameController` — THE UI↔engine seam (intents + state() + ChangeListener).
- Model seams (engine↔model): `Combatant`, `CharacterModel`, `WorldModel` (+ NpcEntity /
  MobEntity / NodeEntity / Portal), `Content`, `GameModel`, `GameContext`, `ModelFactory`.

## Package ownership

| Package(s) | Owner | Responsibility |
|---|---|---|
| `com.whim.alganon.api` | **Orchestrator (DONE — do not edit)** | All seams above |
| `com.whim.alganon.model`, `.content`, `.data` | **Task 1 — Model & Content** | Whole mutable model + full content pack |
| `com.whim.alganon.engine`, `.combat`, `.quest`, `.study`, `.craft`, `.worldsim`, `.persistence` | **Task 2 — Engine & Systems** | GameController impl, combat, quests, Study, crafting, faction-war sim, save/load |
| `com.whim.alganon.ui` | **Task 3 — UI & Rendering** | Swing frame, wizard, all HUD panels, procedural renderers |
| `com.whim.alganon.app` | **Orchestrator (final)** | `Main` wires factory + engine + frame |

---

## Task 1 — Model & Content (`model`, `content`, `data`)

**Goal:** implement the entire mutable domain model + a content pack, exposed purely
through `api` interfaces so engine and UI never see your concrete classes.

**Implement:**
- `ModelFactory` → `AlganonModelFactory` (public no-arg ctor). `content()` returns the
  registry; `newGame(seed)` returns a `GameModel`; `applyCreation(...)` wires
  race/family/class/name → starting stats, starting abilities, starting inventory,
  starting zone + pos.
- `GameModel`, `CharacterModel` (also the player `Combatant`), `WorldModel` (+ NpcEntity /
  MobEntity[Combatant] / NodeEntity / Portal), `Content`.

**Content to author (all flavor text fresh):**
- **2 races** (Human/Asharr, Talrok/Kujix) with stat mods and descriptions.
- **10 families** (5 per faction) across the 5 archetypes.
- **6 classes** with resources per DESIGN.md (Champion=Fury+stances, Reaver=Fury,
  Ranger=Focus+pet/trap/track, Magus=Mana+Flame/Frost/Storm, Mystic=Mana+Words/Touches,
  Cabalist=Mana). **~4–6 abilities each** as `AbilityDef`s (mix of damage/heal/buff/dot/
  pet/trap/stance), with level gating.
- **~15 items** (weapons, armor per slot, consumables, materials, a couple quest items).
- **~6 mob defs** across the 3 behaviors with loot tables.
- **3 zones** (ZoneMeta + tile grids + placed NPCs/mobs/nodes/portals): a faction start
  zone (safe/tutorial), a frontier zone (mobs + gather nodes + faction-war objectives),
  and one instanced dungeon.
- **Static quests** (≥3 wired to giver/turn-in NPCs) + implement `Content.generateQuest`
  (procedural KILL/GATHER/TRAVEL biased by archetype).
- **Crafting:** ~6 `RecipeDef`s forming gather → process → craft chains + `GatherNodeDef`s.

**Deliver:** compiles via `mvn -q compile`; a tiny `main` or JUnit smoke test proving
`newGame(0)` + `applyCreation(...)` yields a placed character with abilities, a loadable
zone with mobs and nodes, and a spawnable/killable mob.

---

## Task 2 — Engine & Systems (`engine`, `combat`, `quest`, `study`, `craft`, `worldsim`, `persistence`)

**Goal:** implement `GameController` as `GameEngine` (public ctor taking a `ModelFactory`),
driving all rules and building the immutable `Views.GameStateView` snapshots the UI reads.

**Implement:**
- **State machine + tick loop.** TITLE → CHARACTER_CREATION → PLAYING (+ overlay states).
  A `javax.swing.Timer` fixed-step tick advances cooldowns, resource regen, mob AI,
  gather-node respawn, and the faction-war sim, then fires `onStateChanged()`.
- **Creation flow** (`beginCreation`/`chooseRace`/`chooseFamily`/`chooseClass`/`setName`/
  `finishCreation`) delegating to `ModelFactory.applyCreation`.
- **Combat** (`combat`): targeting, per-class resource spend, ability cooldowns, cast times,
  DOT/HOT ticks, stance/school modifiers, simple mob AI (aggro/attack/flee by behavior),
  death → loot/XP → level-up (apply xp curve, stat/HP/resource growth).
- **Quests** (`quest`): accept/track/turn-in for static quests; wire `generateProceduralQuest`
  to `Content.generateQuest`; progress hooks from kills/gathers/talks/travel.
- **Study** (`study`): assign/clear a study skill; on `loadGame`, compute
  `elapsedRealMillis = now - GameModel.lastSaveEpochMillis()`, clamp to the **8h cap**,
  and grant banked skill progress (1 study slot in v1). Use-based skill gain from active play.
- **Crafting** (`craft`): `gather`/`craft`/`auctionBuy`/`auctionPost` with skill checks and
  an NPC-seeded auction listing set.
- **Faction-war sim** (`worldsim`): drift `ControlState` of contested objectives over time,
  update `asharr/kujixWarScore`, surface via `FactionWarView` + chat lines.
- **Persistence** (`persistence`): zero-dep human-readable save/load (key=value or simple
  JSON-ish, your call) capturing full character progression + `lastSaveEpochMillis`. Save
  slots per `saveSlots()`.

**Dev aid:** you may run against a trivial in-package fake model for unit tests, but the
production path uses Task 1's `ModelFactory`.

**Deliver:** compiles via `mvn -q compile`; a JUnit/`main` smoke test proving new-game →
creation → move → kill a mob → gain XP → accept+progress a quest → assign study → save →
reload grants offline progress.

---

## Task 3 — UI & Rendering (`ui`)

**Goal:** the entire Swing front end, reading only `Views.GameStateView` and calling
`GameController`. Ship a self-contained `StubController implements GameController` with
canned views so the UI runs standalone before the engine lands.

**Implement:**
- `GameFrame(GameController)` — top-level `JFrame`, registers a `ChangeListener`, repaints
  on `onStateChanged()`, routes key/mouse input to controller intents (WASD/arrows move,
  click-to-target, number keys = action bar).
- **Title screen** (New Game / Load / Settings / Quit).
- **Character-creation wizard** — race → family → class → name, each step with the def's
  description text and a live preview; Back/Next; confirm.
- **Main HUD** (a `WorldPanel` + docked panels): character panel (name/level/class/HP/
  resource bar/XP/stats), **action bar** (abilities with cooldown sweep + resource cost),
  **minimap** placeholder, **quest tracker**, **chat/log panel** with channel tabs
  (System/Say/Family/Faction/Combat/Loot), **inventory** grid.
- **World renderer** — top-down tile grid via `Graphics2D`, player + NPCs + mobs + gather
  nodes + portals as procedural sprites keyed by `spriteKey`; combat feedback.
- **Overlay panels**: Study (assign skill, banked-hours/cap readout), Crafting (recipe list
  + craftable state), Auction (listings + buy/post), Family (archetype bonus + NPC members),
  Library/Codex (renders race/class/family/system reference from Content — a good place to
  note single-player substitutions in-world), Settings, character sheet.
- **Sound hooks:** an extensible `SoundHooks` no-op stub (no audio required).

**Deliver:** compiles via `mvn -q compile`; running `GameFrame` on the `StubController`
shows the title → wizard → HUD with all panels drawn (no engine dependency).

---

## Integration (orchestrator, after all three merge)

Write `com.whim.alganon.app.Main` wiring `AlganonModelFactory` → `GameEngine` → `GameFrame`.
Review combined diff for seam mismatches, compile `mvn -q compile`, run the smoke path,
then report. `README.md` (build/run) lives in `alganon/`.
