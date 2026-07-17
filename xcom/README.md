# UFO: Enemy Unknown — clean-room Java 8 / Swing recreation

A from-scratch desktop recreation of **UFO: Enemy Unknown** (*X-COM: UFO Defense*,
MicroProse / Mythos Games, 1994). It contains **Phase 0: the foundation** — a clean,
layered, data-driven, ruleset-pluggable architecture — and **Phase 1: the
Battlescape** — a playable turn-based tactical mission rendered in an isometric
Swing view. The Geoscape and meta layers are built in later phases on top of this.

## Playing the Battlescape (Phase 1)

Launch the app and choose **New Game** to drop into a skirmish: 4 X-COM soldiers
vs 4 Sectoids on a procedurally generated map. All combat math (to-hit, TU costs,
damage, reaction fire) runs through the Phase 0 ruleset models.

Controls:
- **Left-click a soldier** — select it (yellow ring).
- **Left-click a floor tile** — move there (spends Time Units; may draw reaction fire).
- **Left-click a visible alien** — fire the current mode at it.
- **1 / 2 / 3** — Snap / Aimed / Auto fire mode. **K** — kneel (accuracy bonus).
- **Space** — cycle to the next soldier. **Enter** — end turn (aliens act).
- Hovering an alien shows the live hit % and TU cost in the HUD.

Win by eliminating all aliens; lose if the squad is wiped out. Fog of war hides
undiscovered tiles and unseen aliens; day/night changes sight range.

## The Geoscape campaign (Phase 2)

**New Game** now starts the Geoscape: a drawn world map with your radar-equipped
base, funding nations, and real-time-with-pause flow. Use the six 1994 time
controls (Pause / 5 Secs / 1 Min / 5 Mins / 30 Mins / 1 Hour / 1 Day) to
compress time. UFOs spawn and fly; your radar detects them (chance derived from
the base's radar facilities). **Click a red (detected) UFO** to scramble an
interceptor; if it shoots the UFO down, a crash site hands off to a live
Battlescape assault. Winning the ground battle awards score and salvage; the
monthly Council report adjusts each nation's funding based on your performance.

The Geoscape ↔ Battlescape seam is `com.whim.xcom.geo.MissionLauncher` (a headless
auto-resolver is provided) and `GeoScreen.AssaultHandler` (the interactive path).
Run the Geoscape standalone for testing via `GeoScreen` with a stub handler.

## The meta loop (Phase 3)

Click **Base** on the Geoscape to open base management (`com.whim.xcom.meta`):

- **Research** — assign your scientists to tech-tree projects (`ResearchNode`);
  progress accrues in scientist-days as Geoscape time passes, and completing a
  project unlocks its follow-ons.
- **Manufacturing** — build unlocked items (`ManufactureNode`); the up-front cost
  is debited from funds and units accrue in engineer-hours into your stores.
- **Soldier roster** — a persistent squad that carries between missions. Survivors
  of a won assault gain stats and rank up; the killed are removed; the wounded sit
  out in the infirmary until healed. Assaults deploy your fittest soldiers.
- **Save / Load** — the whole campaign meta-state (funds, score, clock, research,
  manufacturing, stores, roster) round-trips to `xcom-savegame.json` via Gson.

Research and manufacturing advance on the same Geoscape clock, so compressing time
also advances the labs and workshops.

> **Clean-room / no original assets.** All code is original. Every rule and number
> is reconstructed from public documentation (UFOpaedia, OpenXcom) — see
> [`DESIGN.md`](DESIGN.md) and [`CREDITS.md`](CREDITS.md). All art is drawn
> procedurally with Java2D. The original game's code, assets and data files are
> never used or required.

## Requirements
- **JDK 8+** (bytecode is compiled with `--release 8`; builds fine on newer JDKs).
- **Maven 3.6+**.
- One runtime dependency: **Gson 2.10.1** (Apache License 2.0) for data-pack
  loading. Test scope: **JUnit 4.13.2**. Everything else is the JDK (Swing/Java2D).

## Build & run

```bash
cd xcom

# Compile the pure engine + view
mvn -q compile

# Run the headless-safe unit tests (accuracy, TU cost, damage, reactions, data load)
mvn -q test

# Build a self-contained runnable jar (Gson shaded in)
mvn -q -DskipTests package

# Launch the Swing main menu (needs a display)
java -jar target/xcom-recreation.jar
# ...or via Maven:
mvn -q exec:java

# Verify wiring on a display-less machine (prints a content summary, exits 0):
java -jar target/xcom-recreation.jar --headless
# Optional: seed the RNG deterministically
java -jar target/xcom-recreation.jar --seed=12345
```

`Main` opens a title screen with **New Game / Options / Quit** (the first two are
Phase 1+ placeholders). On a headless machine it prints a startup summary instead of
throwing, so CI can smoke-test it.

## Architecture

Strict layering — the pure layers have **no Swing imports** and are fully
unit-testable headless.

```
com.whim.xcom
├── rng/            Rng (interface) + SeededRng — deterministic, injectable. Used by ALL rules.
├── model/          Pure value types/enums (FireMode, DamageType, Difficulty).
├── rules/
│   ├── def/        Content interfaces: WeaponDef, ArmorDef, AlienDef, FacilityDef,
│   │               ResearchNode, ManufactureNode, UfoDef  (all extend GameDef).
│   ├── model/      Formula strategies: AccuracyModel, ReactionModel, DamageModel,
│   │               TimeUnitModel  + the Ruleset1994* default implementations.
│   ├── data/       Immutable Data*Def impls + Gson-backed DataRulesetLoader/RulesetData.
│   ├── Ruleset            aggregate registry: hands out defs AND formula strategies.
│   ├── Ruleset1994        the DEFAULT ruleset (1994 rules + bundled data pack).
│   └── DefRegistry        insertion-ordered id → def map.
├── app/            Main (entry point), AudioManager (interface) + NoopAudioManager (stub).
└── view/           Swing: MainWindow, MainMenuPanel (Java2D placeholder art). NO game logic.

src/main/resources/data/rules1994.json   the default, editable data pack.
```

### Design principles
- **`model` / `rules` are pure.** No `java.awt`/`javax.swing` imports; deterministic
  given a seeded `Rng`. This is what the unit tests exercise.
- **The ruleset is the only seam to the engine.** All formulas, constants and
  content are fetched from a `Ruleset`. The 1994 rules are the *default*; a variant
  supplies a different `Ruleset` (or data pack) with **zero engine edits**.
- **Formulas are pluggable strategies.** `AccuracyModel`, `ReactionModel`,
  `DamageModel`, `TimeUnitModel` are interfaces with `Ruleset1994*` defaults; swap
  one without touching the others.
- **Content is data.** Weapons/armour/aliens/facilities/research/manufacture/UFOs
  load from JSON, so mods/data-packs are possible.
- **Determinism everywhere.** The single `Rng` seam makes runs reproducible
  (replays, tests, future lockstep multiplayer).
- **Audio is behind an interface** (`AudioManager`) so sound can be added later.

## Extending the ruleset

**Add a weapon / alien / facility / UFO (no code):** append an entry to
`src/main/resources/data/rules1994.json`. Example weapon:

```json
{ "id": "plasma_pistol", "name": "Plasma Pistol", "power": 52, "damageType": "PLASMA",
  "twoHanded": false, "weight": 3, "clipSize": 26,
  "snapAccuracy": 65, "snapTu": 30, "aimedAccuracy": 85, "aimedTu": 60,
  "autoAccuracy": -1, "autoTu": -1, "autoShots": 1 }
```
(`-1` accuracy/TU = the weapon does not support that mode.) It is then reachable via
`ruleset.weapon("plasma_pistol")`.

**Ship a whole mod / total conversion:** author your own `mypack.json` and load it:
```java
RulesetData data = DataRulesetLoader.create().loadFromClasspath("data/mypack.json");
Ruleset ruleset = Ruleset1994.fromData(data);
```

**Change a formula (rule variant):** implement the strategy interface and expose it
from a `Ruleset`. For example a house-rule accuracy model:
```java
public final class HouseAccuracy implements AccuracyModel { /* ... */ }
```
Subclass `Ruleset1994` (or implement `Ruleset`) and return your model from
`accuracy()`; everything else stays 1994.

**Add a new content *category* (later phase):** define a `FooDef extends GameDef`
interface + a `DataFooDef` impl, add a `DefRegistry<FooDef>` and accessor to
`Ruleset`, and a list to `RulesetData`.

## Phase 0 vs deferred

**Delivered in Phase 0**
- Maven build (Java 8, JUnit, Gson) + runnable shaded jar.
- Layered, pure, testable `model`/`rules` with the full def + strategy interface set
  and a `Ruleset1994` default loaded from a JSON data pack.
- **Real 1994 formulas** for accuracy, TU cost, reaction score and damage, each with
  passing unit tests (23 tests).
- Seedable `Rng`, `AudioManager` stub, and a launching Swing main menu with
  procedurally-drawn placeholder art.
- `DESIGN.md` with cited formulas and flagged assumptions.

**Deferred to later phases**
- Geoscape (world map, time engine, radar/detection, interception, base building,
  funding/Council, alien-mission scripting).
- Battlescape (tile map, LOS/fog, TU-driven turn engine, reaction fire resolution,
  morale/panic, terrain destruction, explosives, stun/capture, inventory UI).
- Meta loop (soldier experience tables & promotion, item recovery pipeline, research
  and manufacturing progression UI, save/load, Cydonia endgame).
- Real audio backend; richer art.

See [`DESIGN.md`](DESIGN.md) for the full spec these phases implement.
