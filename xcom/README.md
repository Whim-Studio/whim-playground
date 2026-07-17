# UFO: Enemy Unknown — clean-room Java 8 / Swing recreation

A from-scratch desktop recreation of **UFO: Enemy Unknown** (*X-COM: UFO Defense*,
MicroProse / Mythos Games, 1994). It contains **Phase 0: the foundation** — a clean,
layered, data-driven, ruleset-pluggable architecture — and **Phase 1: the
Battlescape** — a playable turn-based tactical mission rendered in an isometric
Swing view. The Geoscape and meta layers are built in later phases on top of this.
It is now a **complete, winnable-and-loseable campaign** (Phases 0–8).

## How to play / how to win (the short version)

**New Game** starts the Geoscape. Compress time until a **red UFO** is detected,
**click it** to scramble an interceptor, and win the crash-site Battlescape.
Respond fast to **magenta TERROR MISSIONS** (ignoring one is a heavy score hit).
Carry the **Stun Rod** and **capture a Sectoid Soldier and a Sectoid Leader alive**
(keep an Alien Containment with room). In **Base**, interrogate both, then research
*Alien Origins → The Martian Solution → Cydonia or Bust!* and press the red
**Cydonia or Bust!** button; win the two-stage final assault and **kill the Alien
Brain** to win the game. Let performance stay net-negative for **two straight
months** and the Council terminates X-COM — you lose. In-game **How to win** and
**UFOpaedia** buttons on the Geoscape explain the rest.

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

## Breadth (Phase 4)

- **Grenades, explosions & terrain destruction** — each soldier carries grenades;
  press **G** and click a tile in range to lob one. The blast damages units in a
  radius (high-explosive, via the ruleset damage model) and destroys terrain:
  walls and UFO hull become walkable rubble, vegetation and boulders are cleared,
  which also opens new lines of sight. A flash effect marks the detonation.
- **Difficulty** — choose Beginner … Superhuman from the menu's **Options**; the
  tier scales alien Time Units, health, accuracy and reactions around the
  Experienced baseline and feeds through to every crash-site assault.
- **Procedural sound** — `SynthAudioManager` synthesises short beeps with
  `javax.sound.sampled` (no audio assets); it is best-effort and silently no-ops
  on a device-less machine, so headless runs and CI are unaffected.

Explosives and destruction are exposed on the engine as `BattleGame.throwGrenade`
and `BattleGame.explode`, so future weapons (rockets, blaster bombs) reuse them.

## Psionics & mixed alien crews (Phase 5)

- **Psionics** — a new pluggable ruleset strategy `PsiModel` (default
  `Ruleset1994Psi`) sits alongside the accuracy/damage/reaction/TU models on the
  `Ruleset` seam. Psi-capable aliens (Sectoid Leaders and above) can assault a
  soldier's mind on their turn; success depends on attacker psi strength, the
  soldier's psi resistance and distance. A panicked soldier **cowers and loses its
  next turn**, so keeping your squad's morale up matters.
- **Mixed alien crews** — crash-site assaults are no longer all Sectoids: a psi
  **leader** joins the crew, and tougher races (Floaters, then Mutons) appear as
  difficulty rises, each with the stats and psi values from the data pack.

`PsiModel` is a clean example of the Phase 0 design paying off: a whole new combat
subsystem was added by defining one strategy interface and its default, with no
change to the engine's other formulas.

## Soldier loadouts (Phase 6)

The research → manufacture → **equip** → deploy loop is now closed. In the **Base**
screen each soldier has a chosen weapon and armour, shown in the roster table.
Use the new **Equip** row (pick a soldier, a weapon and an armour, then *Apply
Loadout*) to change it. What a soldier may equip is data-driven:

- **Weapons** — the basic issue (Pistol, Rifle, Heavy/Auto Cannon) plus anything
  in your stores, so a **manufactured Laser Rifle** becomes equipable once built.
- **Armour** — "None (jumpsuit)" plus any armour in stores (e.g. **Personal
  Armour** after you make it).

Each soldier's loadout persists in the save file and is carried into the next
crash-site assault: the unit deploys wielding exactly the weapon and wearing the
armour you assigned (verified end-to-end — a soldier equipped with a laser rifle
and personal armour deploys with them). `Ruleset` gained `hasWeapon`/`hasArmor`
safe-lookup predicates so stale or unknown ids fall back to defaults instead of
throwing.

## The endgame — win & lose (Phase 7)

The campaign is now **winnable and loseable end to end**, per DESIGN §3.5.

**Live capture & Alien Containment.** Soldiers carry a **Stun Rod** (a `STUN`-type
melee weapon — basic issue, equipable from the Base screen). Stun damage accrues
into a unit's stun pool; when it reaches the unit's health the alien falls
**unconscious** (alive, out of the fight) instead of dying. An alien left
unconscious on a field X-COM wins is **captured alive** — provided the base has an
**Alien Containment** facility with free capacity (the default base ships one;
without it the captive is lost). Live aliens are stored as `live_<race>` items and
shown as "Live aliens" in the Geoscape sidebar.

**Research path to Cydonia.** Interrogating a live captive is a data-driven
research project gated on the captive in stores (consumed when the project starts):

```
Interrogate Sectoid Soldier ─┐
                             ├─► Alien Origins ─┐
Interrogate Sectoid Leader ──┴─────────────────┴─► The Martian Solution ─► Cydonia or Bust!
```

All of it lives in `data/rules1994.json` (new `requiredItems` field on a research
node) — no engine change. Completing **"Cydonia or Bust!"** reveals the red
**Cydonia or Bust!** button on the Geoscape.

**How to WIN.** Shoot down UFOs, stun-capture a **Sectoid Soldier** and a **Sectoid
Leader** (both appear in crash-site crews), interrogate both, research *Alien
Origins → The Martian Solution → Cydonia or Bust!*, then press **Cydonia or Bust!**.
That runs the **two-stage Cydonia assault** — the Martian surface, then the alien
base — which holds the immobile **Alien Brain**. **Kill the Brain** and a victory
screen ends the campaign.

**How to LOSE.** The monthly **Council report** now tracks performance: a month with
net-negative score (or a funding collapse into the red) is a *poor month*, and
**two poor months in a row** trigger **Council termination** and a defeat screen.

Save/load round-trips all of it (containment, live aliens, the bad-month counter and
the win/lose flags). New headless tests cover capture eligibility, containment
gating, the research chain, and the win/lose triggers (`CaptureTest`,
`ResearchGatingTest`, `EndgameTest`) — **57 tests green** (`cd xcom && mvn -q test`).

## Terror missions, the UFOpaedia & the polish pass (Phase 8)

Phase 8 completes the missing feature and hardens the game into a finished slice.

**Terror missions.** A distinct Geoscape alien-mission type: a landed force
attacking a **city** (`TerrorSite`), drawn as a pulsing **magenta marker** with a
countdown. Unlike a UFO it does not fly and is always visible. It **must** be
answered — leaving it until it expires lets the aliens finish and costs a heavy
**−150 score** (and the funding fallout that follows), so terror sites drive the
Council loss condition. Clicking one launches a Battlescape assault against a
**larger, night-time crew** (a psi leader plus a race mix that escalates with
difficulty); winning defends the city for a big score and salvage, and live
captures still flow into containment. Terror frequency and crew size **scale with
difficulty** (Beginner ≈ one every ~14 game-days, Superhuman ≈ every ~6). The seam
reuses `MissionLauncher`/`AssaultHandler` exactly as the Phase 7 note anticipated:
`GeoGame.buildTerrorAssault` / `resolveTerror` and `AssaultHandler.assaultTerror`.

**UFOpaedia (in-game encyclopedia).** A new Swing screen (**UFOpaedia** button on
the Geoscape and Base) lists the entries you have unlocked — weapons, armour,
aliens, UFOs and facilities — each with a description **generated from the data
pack** and a small procedurally-drawn glyph. Gating lives in the pure, testable
`com.whim.xcom.meta.Ufopaedia`: basic gear and your own facilities are always
readable; **new research and captures open new entries** (laser tech after *Laser
Weapons*, sectoid dossiers after an autopsy/interrogation, the general races after
*Alien Origins*, the Brain after *The Martian Solution*).

**Balance, hardening & polish.**
- UFO spawns now genuinely **bias toward smaller craft** (the lower of two rolls),
  so the early game faces killable scouts rather than random unkillable capital
  ships; higher difficulty shifts the mix upward.
- Missions **never launch an empty squad**: an all-wounded or empty roster falls
  back to a scratch squad instead of crashing/auto-losing.
- **End-of-campaign summary** (months survived, score, research, roster) on the
  win/lose screen, and an in-app **How to win** briefing.
- The pure engine and `Ufopaedia` catalog stay **headless-safe**; a new
  `CampaignSmokeTest` runs the whole Geoscape loop (auto-resolving every crash
  site and terror mission) across **all five difficulties** and round-trips the
  save, proving the campaign never dead-ends.

New headless tests cover terror penalties/rewards/spawn, UFOpaedia gating, the
empty-roster guard and the full-loop smoke (`TerrorMissionTest`, `UfopaediaTest`,
`HardeningTest`, `CampaignSmokeTest`) — **69 tests green** (`cd xcom && mvn -q test`).

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
