# Star Command: Galaxies — Build Contract & Design Brief

A standalone, **Java 8**, zero-dependency, procedural-graphics **Swing** clean-room
recreation of the real-time-with-pause starship sim **Star Command Galaxies**
(Warballoon, early-access ~2015–2016). Three parallel tasks build against the
shared `com.whim.scg.api` package and a runnable `com.whim.scg.app` shell —
**both already committed by the orchestrator**. This file is the single source of
truth for the seams between the tasks.

---

## 1. Design Brief (researched mechanics → decisions)

> **Research note:** web tools were unavailable in the build container, so this
> brief is grounded in prior knowledge of Star Command Galaxies and its mobile
> predecessor *Star Command* (2013), plus the well-documented FTL-style
> pause-and-play starship subgenre it sits in. Where a specific detail could not
> be verified, it is marked **[ASSUMPTION]** and a reasonable, internally
> consistent substitute is used. No original art, text, names, or code are used.

Core pillars we recreate:

| Pillar | Source-game behaviour | Our recreation |
|---|---|---|
| **Ship command** | Modular rooms/systems on a ship grid (bridge, engines, weapons, shields, medbay, teleporter, oxygen, quarters, cargo). | Grid ship of `RoomView`s, each an optional powered system with integrity, power, fire/breach state. Two player hulls (Corvette → Cruiser). |
| **Crew management** | Named crew with roles/stats; assign to stations to man systems; happiness affects performance. | `CrewView` with role→primary skill, 0..100 skills, level/xp, happiness, HP. Drag crew onto a room to man it; a manned+powered system performs better. **[ASSUMPTION]** manning bonus = `+skill%` to that system's effect. |
| **Exploration** | Travel a sector/galaxy map; random events, trade, missions. | Node-graph galaxy (`StarSystemView` with links). Jump between linked systems; each may hold an `EventType` (derelict/distress/merchant/hazard/pirate ambush/story). Starports allow trade/repair/recruit/upgrade. |
| **Space combat** | Real-time, pausable; weapons vs shields; targeted room damage → fires, breaches, system failures, crew loss on breach. | Continuous-space duel, **space bar pauses**. Weapons charge over time, target a specific enemy room; ion pierces shields; torpedoes bypass shields; hull hits can ignite fires / open breaches; breaches vent oxygen and hurt crew in that room. |
| **Boarding / away** | Teleport crew onto enemy ship/planet; direct-control room-by-room combat. | When enemy shields are down, teleport a party into a tile-grid `BoardingView`; select crew, move (arrows/click), attack adjacent hostiles; win by clearing hostiles or reaching the objective. |
| **Progression** | Upgrade systems, research tech, earn currency from missions/salvage. | Credits from victories/events/salvage; a 7-track `TechView` tree (weapons/shields/engines/hull/medbay/teleporter/sensors); buy weapons & a bigger hull at starports. |
| **Save/load** | — | JSON save to `saves/` (slot `auto` + named), engine-owned. |

**Scope simplifications (stated up front):** single player ship at a time;
combat is 1-v-1; galaxy is a modest node graph (not infinite); tech tree is
linear per track; no multiplayer, no crafting depth, no full campaign narrative
beyond light story events + a final boss encounter. These keep the vertical
slice fully playable end-to-end.

---

## 2. Architecture

- **Game loop:** `app.GameFrame` runs a `javax.swing.Timer` at ~60 FPS. Each tick
  it calls `controller.tick(dt)` (unless paused) then `screen.update(dt)` then
  repaints. Rendering is immediate-mode Java2D.
- **State machine:** `Enums.Mode` (MENU / SHIP_INTERIOR / GALAXY_MAP / STARPORT /
  SPACE_COMBAT / BOARDING / GAME_OVER / VICTORY). The active `Screen` is whichever
  registered screen matches `view().mode()`.
- **Seam:** UI ↔ engine talk **only** through `api.GameController` (intents in)
  and `api.Views.*` (read-only projections out). The shell instantiates the
  engine reflectively (`com.whim.scg.engine.Engine`, public no-arg ctor) and
  falls back to `app.StubController` if absent — so everything compiles and the
  window runs before any task lands.
- **Packages:** `api` (seam, committed), `app` + `render` (shell, committed),
  `engine`/`model`/`save`/`content` (Task 1), `ui.galaxy`/`ui.combat` (Task 2),
  `ui.ship`/`ui.crew`/`ui.boarding` (Task 3).

---

## 3. Hard constraints (ALL tasks)

- **Java 8 ONLY.** No `var`, switch-expressions, text blocks, records,
  `List.of`/`Map.of`, `Stream.toList()`. `maven.compiler.source/target = 1.8`.
- **Zero external libraries** beyond the JDK. JUnit for tests only. Parse JSON
  with a tiny hand-written parser in `content` (Task 1) — do **not** add a JSON
  dependency.
- **Package root** `com.whim.scg` • **source root** `starcommandgalaxies/src/`.
- All graphics **procedural Java2D**; no image/audio files. Use `render.Palette`
  and `render.UiKit` so screens read as one system.
- Compile from `starcommandgalaxies/`: `mvn -q compile`.
- Each task imports ONLY from `com.whim.scg.api`, `com.whim.scg.render`,
  `com.whim.scg.app` (read-only), and its own package(s). **Do NOT edit `api`,
  `app`, or `render`, and do NOT import another task's package.** If you need an
  `api` change, **report it to the orchestrator via `send_prompt` — do not edit
  `api` yourself.**
- **Do NOT open a pull request.** Push your branch and report back to the
  orchestrator via `send_prompt`.
- No copyrighted assets/text/names from the original game. Invent flavour names.

---

## 4. Screen ownership (class names are load-bearing — the shell resolves them reflectively)

| Mode | Screen class (exact) | Owner |
|---|---|---|
| MENU | `com.whim.scg.app.MenuScreen` | orchestrator ✅ |
| GAME_OVER / VICTORY | `com.whim.scg.app.EndScreen` | orchestrator ✅ |
| SHIP_INTERIOR | `com.whim.scg.ui.ship.ShipInteriorScreen` | **Task 3** |
| BOARDING | `com.whim.scg.ui.boarding.BoardingScreen` | **Task 3** |
| GALAXY_MAP | `com.whim.scg.ui.galaxy.GalaxyMapScreen` | **Task 2** |
| STARPORT | `com.whim.scg.ui.galaxy.StarportScreen` | **Task 2** |
| SPACE_COMBAT | `com.whim.scg.ui.combat.SpaceCombatScreen` | **Task 2** |

Every screen implements `api.Screen` and has a **public constructor
`(api.GameController)`**. Draw from `controller.view()`, send intents via the
controller. Also provide a crew roster panel (Task 3, inside ShipInteriorScreen
is fine) covering stats/level/happiness/role.

---

## 5. Package ownership

| Package(s) | Owner | Responsibility |
|---|---|---|
| `com.whim.scg.api`, `.app`, `.render`, `data/*.json` | orchestrator ✅ | seams, shell, render kit, seed content |
| `com.whim.scg.engine`, `.model`, `.save`, `.content` | **Task 1** | `Engine implements GameController`; ship/crew/galaxy/combat/boarding model & rules; content JSON loader (`content`); economy + tech; JSON save/load (`save`). Owns the whole simulation. |
| `com.whim.scg.ui.galaxy`, `.ui.combat` | **Task 2** | Galaxy map screen (navigate/scan/dock/events), starport screen (repair/recruit/buy tech & hull/undock), space-combat screen (weapon charge/target/fire, power, shields/hull/fires/breaches, begin-boarding, flee). |
| `com.whim.scg.ui.ship`, `.ui.crew`, `.ui.boarding` | **Task 3** | Ship-interior screen (room grid, drag-and-drop crew assignment, power allocation UI, crew roster with stats/level/happiness/role), boarding screen (tile grid, select/move/attack, objective/win). |

**Task 1 is the keystone:** Tasks 2 & 3 render real data only once `Engine`
exists. Until then they run against `StubController` (null sub-views) — so
**every UI screen MUST null-check `view().playerShip()`, `.combat()`,
`.boarding()`, `.galaxy()`** and draw a graceful "no data" state. This keeps the
build green at every merge order.

---

## 6. Engine behaviour contract (Task 1 — what the UI relies on)

- `newGame(captain, ship)` → Corvette from `ships.json`, a starting crew (~4:
  captain, pilot, engineer, gunner) assigned to rooms, a generated galaxy
  (≈10–14 systems, current has no event), credits ≈ 150, day 1, mode stays as
  the caller sets it.
- `tick(dt)`: advances only meaningful sim in SPACE_COMBAT (charge weapons, move
  projectiles, apply damage, spread/extinguish fires, vent breaches, run enemy
  AI) and BOARDING (hostile AI acts on a cadence). Other modes may idle. Respect
  `paused()`.
- Combat: weapon fires when charged (auto if `powered>0` and target set, or via
  `fireWeapon`); damage reduces target room hp then hull; `piercesShields`
  ignores shields. Fires tick damage + spread; breaches vent oxygen and damage
  occupying crew. `combat().canBoard()` true when enemy shields==0. Victory →
  credits + salvage, `over()/playerWon()`.
- Boarding: `beginBoarding` builds a `BoardingView` grid from the enemy ship with
  the party placed at the teleporter; `moveBoarder`/`boarderAttack` with adjacency
  rules; clearing hostiles or reaching objective → `playerWon()`; recall via
  `endBoarding`.
- Progression/economy: victories & events grant credits; `buyTech` raises a
  track and applies its effect (e.g. SHIELDS→+max shields, HULL→+max hull); a
  starport lets you `repairAll`, `recruitCrew`, buy a bigger hull. `save/load`
  round-trips full state as JSON in `saves/`.
- **Never throw** from an intent — return `ActionResult.fail(msg)`.

---

## 7. Build & run

```bash
cd starcommandgalaxies
mvn -q compile            # or: javac -d out $(find src -name '*.java')
mvn -q exec:java -Dexec.mainClass=com.whim.scg.app.Main   # or java -cp out:src com.whim.scg.app.Main
```
The shell is headless-safe (prints a notice under CI). On a desktop it opens a
1120×720 window at the main menu.

---

## 8. Definition of done (per task)

- `mvn -q compile` clean from `starcommandgalaxies/` with your package added.
- No import of another task's package; no edits to `api`/`app`/`render`.
- Your screens/engine null-check absent sub-views and never throw on the shell.
- Push your branch; **report back to the orchestrator via `send_prompt`** with a
  one-line status + your branch name. Do not open a PR.
