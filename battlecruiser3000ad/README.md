# Battlecruiser 3000AD — Java 8 / Swing recreation

A clean-room homage to *Battlecruiser 3000AD* (3000AD Inc. / Derek Smart, 1996).
No original assets — all graphics are procedurally drawn with Java2D.

See [`../docs/BC3K_Phase1_Design.md`](../docs/BC3K_Phase1_Design.md) for the research
and design doc, including which facts are sourced vs. labelled design approximations.

## Status — Phase 2 (Core Architecture)

Runnable skeleton in place:

- **Build:** Maven, Java 8 only (matches the sibling recreations in this repo).
- **Game loop:** `GameFrame` runs a ~60 FPS Swing `Timer`; the sim advances via
  `GameController.tick(dt)`.
- **Screen/state framework:** each bridge console is a `Mode`/`Screen`; `ScreenManager`
  switches on `view().mode()`. `F1`–`F8` switch consoles, `Esc` → menu, `P` pauses.
- **Engine seam:** `com.whim.bc3k.api.GameController` is the only UI↔engine boundary;
  `engine.Engine` is a small **live** stub (hull, shields, finite reactor power budget).
- **Live demo of the seam:** the **POWER** console (F4) really allocates reactor power
  through the engine (arrows to adjust); `Shift+R` restarts the reactor, `Ctrl+S`
  requests a tow — both verified original shortcuts.
- **Placeholder art:** procedural starfield + vector HUD; other consoles show a
  labelled placeholder body until their phase lands.

## Status — Post-review (save/load + promoted combat)

- **Save/load**: `save.SaveManager` persists a flat `Properties` snapshot per slot.
  Autosaves to `auto` on new game / jump / objective; `F9` saves manually; the menu's
  `[C] Continue` loads it. `Engine.snapshot()/restore()` round-trip the full state.
- **Fighter combat**: launching a FIGHTER (FLIGHT DECK, `L`) during Xtreme Carnage now
  commits it to a dogfight — wings attrite and strafe the enemy capital ship; TACTICAL
  shows both wings.
- **Ground combat**: FLIGHT DECK `D` deploys ATVs into a playable planetary skirmish on
  a new GROUND screen (`SPACE` to assault; forces also trade fire over time).
- **Review fixes folded in** (`/code-review` high pass, 6 findings): crew no longer sticks
  in a phantom "walking" state (#3); enemy shields no longer self-decay (#4); combat now
  degrades subsystems so ENG repair matters mid-fight (#5); a reactor scram on critical
  hull makes the `Shift+R` restart meaningful (#1); the NAV star map is clipped to its
  panel (#6); view projections are now cached so the render path allocates nothing in
  steady state (#2). All six findings addressed.

## Status — Phase 6 (Integration & Known-Issues)

- All three modes distinct and playable: **Advanced Campaign** now has a live dynamic
  ticker — a rising Gammulan **threat** and rotating GALCOM **objectives** shown on
  COMMS (F5); `Enter` marks an objective complete and relieves threat.
- Full known-issues / simplifications ledger: [`../docs/BC3K_Phase6_KnownIssues.md`](../docs/BC3K_Phase6_KnownIssues.md).
- New module `sim.campaign.Campaign` + tests. Seam verified consistent: all 18
  controller intents and 13 view methods implemented across 30 source files.

> **Not compiled here** — this container has no JDK/Maven. Run `mvn test` on JDK 8 to
> confirm; see the known-issues doc for the verification caveat.

## Status — Phase 5 (UI Polish & Placeholder Art)

All eight bridge consoles are now live (no remaining pure placeholders):

- **COMMS (F5)** — mode-aware GALCOM briefing + channel traffic log; `H` hails the
  nearest station, `Ctrl+S` requests a tow.
- **CARGO (F6)** — logistics manifest (credits, fuel bar, spare parts, ordnance);
  `R` refuels at a starstation. **Jumps now consume fuel** (15/jump), so navigation
  and logistics are coupled.
- **FLIGHT DECK (F8)** — fighter/shuttle/ATV complement; `Up/Down` select, `L` launch,
  `R` recall, bounded by how many are carried.
- **Polish** — a transient mission **flash banner** under the HUD (auto-clears after
  ~4s), consistent console chrome, and the always-on hull/shield/reactor/alert HUD.

Logistics/comms/flight-deck are covered by `test/com/whim/bc3k/LogisticsTest.java`.

## Status — Phase 4 (Game Modes)

The Phase 3 sim modules are now wired into `Engine` behind the same `GameController`
seam, and two of the three modes are playable:

- **Free Flight** — no hostiles. NAV shows a live star map; press `1..9` to jump to a
  linked system. PERSONNEL shows the live crew roster (health/hunger/fatigue/location);
  `G` sends the selected crew to the galley, `Q` to quarters, `C` clones from DNA.
  POWER (F4) and ENGINEERING (F3, integrity + `R` to repair) are live.
- **Xtreme Carnage** — spawns a Gammulan raider; TACTICAL (F2) shows enemy hull/shields,
  `SPACE` fires a volley whose damage scales with WEAPONS power (set it on PWR). The
  enemy auto-fires; losing routes to the end screen.
- **Advanced Campaign** — boots as Free Flight for now; its dynamic ticker is a later
  phase (flagged in-game and here, not silently dropped).

New module `sim.combat.CombatSim` (deterministic ship-to-ship resolution) with tests.

## Status — Phase 3 (Core Systems)

Three standalone, headless-tested simulation modules under `com.whim.bc3k.sim`:

- **`sim.ship.ShipSystems`** — finite reactor power budget + per-system allocation,
  hull/shields, damage (shields-then-hull spillover), subsystem integrity/breach,
  engineering repair, and shield regen scaled by power × integrity.
- **`sim.crew`** — `CrewMember` (health/fatigue/hunger, skills, walking between
  named `ShipLocation`s, starvation death), `CrewRoster` (hire, DNA vault on death,
  cloning, best-fit assignment).
- **`sim.galaxy`** — `Galaxy` + `StarSystemNode`: a fixed connected sector with jump
  links, legal-jump enforcement, visited tracking, neighbours.

These are intentionally **not yet wired into `Engine`** — that integration happens in
Phase 4 when Free Flight / Xtreme Carnage consume them, so the working Phase 2 shell
stays intact. Tests: `test/com/whim/bc3k/sim/{ShipSystems,Crew,Galaxy}Test.java`.

## Package layout

```
com.whim.bc3k
├── app       // Main, GameFrame (loop+input), ScreenManager, Menu/Console/End screens
├── api       // GameController seam, Views (read-only), Screen, Enums, ActionResult
├── engine    // Engine — Phase 2 live stub sim (no Swing imports)
├── sim       // Phase 3 core systems — ship / crew / galaxy (no Swing imports)
└── render    // Palette, UiKit, Starfield (Java2D helpers)
```

`engine` and `api` never import Swing/AWT, so the simulation is unit-tested headlessly
(`test/com/whim/bc3k/EngineTest.java`).

## Build & run

Requires JDK 8 and Maven (not installed in this container — build on a desktop):

```
mvn -q compile         # compile
mvn -q test            # run headless engine tests
mvn -q exec:java -Dexec.mainClass=com.whim.bc3k.app.Main   # or run Main from your IDE
```
