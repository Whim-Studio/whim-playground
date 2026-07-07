# Babylon 5 Wars — Architecture Plan (Phase 2)

Implementation target: **Java 8 language level, standard library + Swing only**, plus one
JSON library (**Gson**) for data loading. No JavaFX, no game engines, no other GUI deps.
Decisions locked in Phase 1: **hex grid**, **armor-default / shield-flag defense**, **d20 to-hit**.

> Faithfulness-first: all `[APPROXIMATED]` rule numbers live in JSON/data, never hardcoded in
> engine logic, so the ruleset can be corrected against the real rulebook by editing data.

---

## 1. Module / package structure

Base package `com.whim.b5wars` under `babylon5wars/src/main/java/...`:

```
com.whim.b5wars
├── Main.java                 // entry point: load data, build scenario, launch Swing UI
├── model/                    // pure data types, no engine/UI dependencies
│   ├── Hex.java              // axial hex coord (q,r) + geometry helpers
│   ├── Facing.java           // enum of 6 hexsides + rotation/arc math
│   ├── Race.java             // enum / id of factions
│   ├── DefenseType.java      // ARMOR | SHIELD
│   ├── Weapon.java           // type, arcs, range brackets, toHit, damage, reload, traits
│   ├── WeaponArc.java        // set of Facings a mount can cover
│   ├── Ship.java             // stat header + mutable in-play state (pos, facing, speed, damage)
│   ├── ShipClass.java        // immutable printed template a Ship is instantiated from
│   ├── SystemBox.java        // one damageable system/structure box in the SCS diagram
│   ├── Faction.java          // race + its available ship classes
│   └── Scenario.java         // map size, ship placements, victory condition
├── data/                     // load model objects from resources
│   ├── DataLoader.java       // Gson-backed loaders for factions/ships/scenarios
│   └── (resources) *.json
├── engine/                   // rules — deterministic given an RNG seed
│   ├── GameState.java        // full mutable game state (ships, turn, impulse, initiative)
│   ├── TurnManager.java      // the impulse/initiative state machine (§3)
│   ├── MovementEngine.java   // thrust, turn mode, pivot, sideslip, drift, impulse cadence
│   ├── CombatEngine.java     // to-hit, arc/range checks, damage → armor → structure → systems
│   ├── CriticalTable.java    // system-damage results (data-driven)
│   ├── Dice.java             // seedable RNG wrapper (d20, dN, damage rolls)
│   └── GameEvent.java        // structured log events (fire, hit, crit, move, phase change)
├── ai/                       // stretch goal
│   └── SimpleAI.java         // heuristic opponent (optional; hot-seat is the MVP)
├── save/
│   └── SaveGame.java         // JSON serialization of GameState (Gson)
└── ui/                       // Swing — reads engine, sends commands, never owns rules
    ├── MainWindow.java       // JFrame; wires panels + menu (New/Save/Load)
    ├── PlayAreaPanel.java    // Graphics2D hex field: ships, facing, vectors, arc overlays
    ├── ShipSheetPanel.java   // Ship Control Sheet: stats, damage diagram, weapons table
    ├── TurnBarPanel.java     // initiative / turn / impulse indicator + phase controls
    ├── WeaponFireDialog.java // arc/target/range selection for firing
    ├── PowerEwPanel.java     // power + EW allocation for the active ship
    └── LogPanel.java         // scrolling combat/rules log fed by GameEvent
```

**Dependency rule:** `ui → engine → model`, `data → model`, `engine → model`. Model has no
outward deps. UI never mutates rules state directly; it calls engine methods and re-renders.

---

## 2. Core domain classes (contracts)

- **`Hex`** — axial `(q, r)`; helpers: `distance`, `neighbor(Facing)`, `line`, pixel<->hex.
- **`Facing`** — `F, FR, BR, B, BL, FL` (6 sides); `rotate(±1)`, `opposite`, `arcContains`.
- **`ShipClass`** (immutable template) — `id, name, race, points, maxSpeed, turnMode,
  thrust, power, initiativeBonus, crewQuality, sensorRating, ewRating,
  Map<Facing,Integer> armor, Map<Section,Integer> structure, DefenseType defenseType,
  List<Weapon> weapons, List<Special> specials`.
- **`Ship`** (mutable instance) — references a `ShipClass`; holds `owner(side)`, `Hex pos`,
  `Facing facing`, `int speed`, current `armor`/`structure`/system damage, `powerAllocation`,
  `ewOffensive/ewDefensive`, per-weapon reload state, `destroyed`/`crippled` flags.
- **`Weapon`** — `name, type, WeaponArc arcs, int[] rangeBrackets, int baseToHit,
  DamageProfile damage, int reloadTurns, Set<Trait> traits` (traits: `ARMOR_PIERCING`,
  `RAKING`, `INTERCEPTOR`, `GUIDED`, `BALLISTIC`).
- **`Scenario`** — `name, mapWidth, mapHeight, List<Placement>(shipClassId, side, Hex,
  Facing, speed), VictoryCondition`.

---

## 3. Game loop / state machine (impulse turns)

`TurnManager` is an explicit finite-state machine; the UI advances it via a single
`advance()` / phase-specific command methods. States mirror §1 of the rules doc:

```
INITIATIVE → POWER_ALLOCATION → EW_ALLOCATION → IMPULSE_LOOP → END_OF_TURN → (next turn)
```

- **INITIATIVE** — `Dice.d6 + initiativeBonus` per side; set move order.
- **POWER_ALLOCATION / EW_ALLOCATION** — per active ship; UI panels write into `Ship`.
- **IMPULSE_LOOP** — iterate `impulse = 0..N-1` (N = data-driven, default 8 `[APPROXIMATED]`):
  for each impulse, each ship due to move (per the speed→impulse cadence table) executes its
  queued movement via `MovementEngine`; then `CombatEngine` resolves fire declared this
  impulse. Positions during the loop drive range/arc, so interleaving matters.
- **END_OF_TURN** — reloads tick, shields (if any) regenerate, crippled/destroyed removed,
  drift applied to powerless ships, victory check.

Determinism: all randomness flows through one seeded `Dice`, so games are reproducible and
unit-testable.

---

## 4. Data-driven definitions (external resources)

`babylon5wars/src/main/resources/`:

```
factions/earth-alliance.json      // ship classes for EA (Hyperion, Artemis, ...)
factions/narn-regime.json         // ship classes for Narn (G'Quan, Sho'Kos, ...)
weapons/weapons.json              // optional shared weapon library referenced by id
scenarios/border-skirmish.json    // the Phase-1 duel: Hyperion vs G'Quan
tables/impulse-cadence.json       // speed → impulses-moved-on  [APPROXIMATED]
tables/critical-hits.json         // crit table entries        [APPROXIMATED]
```

Every `[APPROXIMATED]` numeric block carries a `"_note": "APPROXIMATED, unverified vs rulebook"`
field so it is self-documenting. `DataLoader` validates required fields and fails loudly on
malformed data.

---

## 5. Save / load

`SaveGame` serializes `GameState` to JSON via Gson (simple, human-readable, diffable).
`ShipClass`/`Weapon` templates are referenced by id and reloaded from resources; only mutable
per-game state (positions, damage, turn/impulse, RNG seed+cursor) is persisted, so saves stay
small and forward-compatible with data corrections.

---

## 6. Placeholder graphics
No copyrighted B5/AOG art. Ships rendered as **faction-colored triangles/silhouettes** via
`Graphics2D` (facing = triangle nose), a procedurally drawn **starfield** background, and
vector/arc overlays. All original, generated at runtime.

---

## 7. Testing
JUnit-style tests (plain, no heavy deps) for the deterministic engine:
- `MovementEngine`: turn-mode enforcement, thrust cost, drift, impulse cadence.
- `CombatEngine`: arc/range gating, to-hit modifier stack, armor→structure→systems flow,
  armor-piercing/interceptor traits, crit application.
- Seeded `Dice` makes every assertion reproducible.

---

## 8. Build & run
Plain `javac`/`java` compatible; a minimal Maven `pom.xml` (Java 8 target, Gson + JUnit) is
provided for convenience. `Main` loads the border-skirmish scenario and opens the window.
Hot-seat 2-player is the MVP; `SimpleAI` is an optional stretch.

---

## 9. Proposed implementation order (Phase 3 — for approval, not yet started)
1. `model/` + `data/` loaders + EA/Narn JSON (2 factions, 2–3 ships each) + the duel scenario.
2. `engine/` movement (impulse/thrust/turn-mode/drift) + unit tests.
3. `engine/` combat (to-hit/arc/range/damage/crits) + unit tests.
4. `ui/` play area + ship sheet + turn bar + fire dialog + power/EW + log.
5. Hot-seat wiring end-to-end on the duel scenario; save/load.
6. (Stretch) `SimpleAI`.

### Suggested delegation (Phase 3)
Because the seams above have clean file ownership, Phase 3 parallelizes well into child
tasks with no file overlap: **(A)** model + data + JSON, **(B)** engine + tests (depends on A's
types), **(C)** UI (depends on A's types, mocks engine). I'll finalize this split and confirm
branch/interface contracts with you at the Phase 3 checkpoint.
```
