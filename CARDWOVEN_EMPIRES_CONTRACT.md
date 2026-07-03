# Cardwoven Empires — Build Contract

A standalone, **Java 8**, zero-dependency, procedural-graphics Swing adaptation of the
4X deckbuilder **Cardwoven Empires**. Three parallel tasks build against the shared
`api` package. This file is the single source of truth for the seams between them.

## Hard constraints (all tasks)

- **Java 8 ONLY.** No `var`, no switch expressions, no text blocks, no records, no
  `List.of`/`Map.of`, no Streams-only-in-Java-9 APIs, no local type inference.
  `maven.compiler.source/target = 1.8`.
- **Zero external libraries.** Only `javax.swing`, `java.awt`, `java.util`, `java.io`.
  JUnit is allowed for tests only (already in root `pom.xml` style).
- **No downloaded images.** All visuals via `java.awt.Graphics2D` (shapes, colors,
  gradients, strokes). No image files, no icons on disk.
- **Package root:** `com.whim.cardwoven`
- **Source root:** `cardwoven-empires/src/`
- Every task must ensure its code **compiles** and imports ONLY from `api` (and its
  own sub-package). No task edits another task's sub-package.

## Package ownership

| Package | Owner | Contents |
|---|---|---|
| `com.whim.cardwoven.api` | **Orchestrator (DONE — do not edit)** | `Enums`, `Views`, `GameController`, `ActionResult` |
| `com.whim.cardwoven.domain` | **Task 1** | `Card`, `Deck`, `DiscardPile`, `GridMap`, `Tile`, `Building`, `Attachment`, `Faction*`, `PlayerState`, `Resources`, `GameState`, `CardLibrary` |
| `com.whim.cardwoven.engine` | **Task 2** | `GameEngine` (implements `api.GameController`), `TurnManager`, `EconomyCalculator`, `CombatResolver`, `SinLogic`, `VictoryMonitor`, `AiPlayer` |
| `com.whim.cardwoven.ui` | **Task 3** | `GameFrame`, `MapPanel`, `HandPanel`, `SidePanel`, `LogPanel`, `Renderer`, `UiColors`, `StubController` (dev only) |
| `com.whim.cardwoven.app` | **Orchestrator (final)** | `Main` — wires engine + ui |

## The `api` package (already committed — read it, do not modify)

- `Enums`: `Faction`, `BuildingType`, `TerrainType`, `CardType`, `AttachmentType`,
  `ResourceType`, `GamePhase`, `VictoryType`.
- `Views`: read-only interfaces `CardView`, `AttachmentView`, `BuildingView`,
  `TileView`, `MapView`, `PlayerView`, `GameStateView`.
- `GameController`: the ONE seam between UI and logic. Methods:
  `state()`, `playBuilding(cardId,row,col)`, `attachCard(cardId,buildingId)`,
  `playCard(cardId,row,col)`, `resolveCombat(cardId,row,col)`, `endTurn()`,
  `newGame(faction)`.
- `ActionResult`: `ok(msg)` / `fail(msg)`, `isSuccess()`, `message()`.

**Data flow:** UI renders `controller.state()` (a `GameStateView`). User input calls a
`GameController` action. On success the engine mutates domain state; UI re-reads
`state()` and repaints. UI never casts a `*View` to a concrete domain class.

## Ruleset the domain + engine must encode

- **Map:** rectangular grid (target 10×8) of `Tile`s with `TerrainType`. Ports require
  a WATER-adjacent tile; Farms prefer PLAINS; other buildings on any land tile.
- **Factions (3):**
  - *Lands of the King* — balanced, **high draw** (base hand 6).
  - *Babylon* — **building-focused** (cheaper buildings / building yield bonus).
  - *The Unfaithful* — plays powerful ECONOMY/MILITARY cards that **add SIN cards**
    to its own deck as a cost (dead cards that clog draws).
- **Cards:** `CardType` = BUILDING, ATTACHMENT, MILITARY, ECONOMY, EXPLORE, SIN.
  Deck supports dynamic size + shuffle; discard pile reshuffles into deck when empty.
- **Buildings & attachments:** playing a BUILDING card places a `Building` on a tile.
  Buildings hold **nested attachments** up to a capacity. Attachment yields per turn:
  - WORKER on CITY → +Gold
  - IDOL on TEMPLE → +card draw
  - WITCH on TEMPLE/CITY → +Command Points
  Attachments apply **buff modifiers** to the building's per-turn yield.
- **Economy:** `Resources` tracks GOLD and COMMAND_POINTS. Yields accrue in the YIELD
  phase from every building's attachments (+ faction/terrain modifiers).
- **Combat:** MILITARY cards resolve against neutral **Orcish raiders** (per-tile
  `raiderStrength`) or rival buildings. Deterministic strength comparison + defense.
- **Turn phases (in order):** DRAW → MAIN → COMBAT → YIELD → DISCARD → END.
- **Victory (5 types, faction-weighted):** ECONOMIC, MILITARY, EXPANSION, FAITH,
  DOMINANCE. `VictoryMonitor` checks at end of every turn; `PlayerView.victoryProgress`
  reports 0..1 progress per type; `pursuableVictories()` lists the faction's paths.

## Task 1 — Domain & State (owns `domain`)

Deliver concrete classes implementing the `api.Views` interfaces:
- `Card` (impl `CardView`), `Attachment` (impl `AttachmentView`),
  `Building` (impl `BuildingView`) with a nested attachment list + capacity + buffs,
  `Tile` (impl `TileView`), `GridMap` (impl `MapView`), `PlayerState` (impl `PlayerView`),
  `GameState` (impl `GameStateView`).
- `Deck` + `DiscardPile`: dynamic size, `shuffle()` (seeded `java.util.Random` for
  determinism), draw, reshuffle-discard-into-deck when empty.
- `Resources`: get/add/spend GOLD & COMMAND_POINTS with `canAfford`.
- `CardLibrary`: static factory building each faction's starting deck (buildings,
  attachments, military, economy, explore) + the SIN card factory.
- Faction profiles: base hand size, starting resources, deck composition, building
  cost/yield modifiers, and the list of `VictoryType` each faction pursues.
- Provide simple mutation methods the engine needs (place building, add attachment,
  add/remove cards, set explored, set raider strength, set phase/turn/winner, append log).
- **No engine logic, no Swing.** Pure model + minimal helpers. Deterministic via a
  `Random` you accept in the constructor.
- Add a tiny `main`-free self-check or JUnit test proving deck shuffle/draw/reshuffle
  and attachment-buff math work.

## Task 2 — Logic, Economy, Combat, AI, Victory (owns `engine`)

- `GameEngine implements api.GameController`. Constructed with a `GameState` (from
  Task 1's factory). Implements every `GameController` method with full validation,
  returning `ActionResult.ok/fail`.
- `TurnManager`: sequences DRAW→MAIN→COMBAT→YIELD→DISCARD→END; enforces hand limits.
- `EconomyCalculator`: computes per-turn yields from each building's attachments plus
  faction/terrain modifiers; applies to `Resources`.
- `CombatResolver`: deterministic resolution of MILITARY vs raiders / enemy buildings
  (attack vs strength+defense), updates the map and logs outcomes.
- `SinLogic`: when The Unfaithful plays a powerful card, inserts SIN card(s) into its
  deck; SIN cards are un-playable dead weight that must be discarded.
- `VictoryMonitor`: after each turn computes 0..1 progress for all 5 `VictoryType`s and
  sets a winner when a threshold is met. Feed results back through `PlayerView`.
- `AiPlayer`: simple heuristic opponents (expand, attach, defend, attack) so `endTurn`
  advances rivals.
- **Imports only from `api` and `domain`.** No Swing.
- Include a headless smoke `main` (or JUnit) that plays several full turns end-to-end
  and prints the log, proving yields/combat/victory monitors fire.

## Task 3 — Swing UI & Procedural Graphics (owns `ui`)

- `GameFrame` (`JFrame`, `BorderLayout` or `GridBagLayout`): world map center,
  hand dashboard bottom, side panels for Gold / Command Points / Deck & Discard counts
  and victory progress, plus a small event-log feed.
- `MapPanel`: `Graphics2D` render of the grid — distinct geometric shapes/colors per
  `TerrainType`, per `BuildingType`, and small glyphs for attached units and raiders.
  Handles click-to-select tile.
- `HandPanel`: renders the current hand as procedurally-drawn cards; supports
  click-to-select a card then click a tile/building to play (click-and-drop style).
- Side/`SidePanel` + `LogPanel`: resource readouts, deck/discard counts, victory
  progress bars, recent log.
- Reads state via `controller.state()` ONLY; triggers logic via `GameController`
  actions ONLY. **No imports from `domain` or `engine`.**
- Provide a `StubController implements api.GameController` (dev-only, in `ui`) with a
  hand-built fake `GameStateView` so the UI runs before Task 2 lands. Final `Main`
  will swap in the real engine.
- Clean, readable palette in `UiColors`; responsive to resize.

## Reporting

Each task: implement, ensure `javac` compiles your package against `api` (+ `domain`
for Task 2, + a stub for Task 3), **push your branch and open a PR**, then
`send_prompt` the orchestrator with: what you built, key class list, how you verified
(compile/test output), and any interface friction. Report blockers to the orchestrator,
not via comments.
