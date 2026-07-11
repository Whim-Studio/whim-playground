# Battlecruiser 3000AD — Java 8 / Swing Recreation

## Phase 1 — Research & Design Document

**Status:** Research complete. No gameplay code written yet. Awaiting review before Phase 2.

> **Sourcing note.** Live web search is disabled in this environment. Facts below are drawn
> from (a) the [Wikipedia article on Battlecruiser 3000AD](https://en.wikipedia.org/wiki/Battlecruiser_3000AD)
> (fetched and verified this session), and (b) the well-documented anchor facts in the project
> brief, which match the game's widely-reported design. Anywhere a specific mechanic/number
> could not be verified from an accessible primary source (original manual scans, fan wikis —
> the fandom wiki returned HTTP 402), it is **explicitly labelled `[EXTRAPOLATION]`** and treated
> as a reasonable design approximation, not sourced fact, per the no-fabrication constraint.

---

## 1. Game Modes

BC3K ships three distinct modes. Rules below combine verified descriptions with labelled design decisions for our recreation's win/loss states.

### 1.1 Free Flight
- **Verified:** Unrestricted exploration to learn controls *"without fear of attack by alien craft or pirates."*
- **Our rules:** No hostiles spawn. No fail state. Player can fly the battlecruiser, launch/pilot fighters/shuttles/ATVs, dock at stations, and exercise every console. Purpose = a sandbox tutorial.
- **Win/Loss:** None. Exit is voluntary. `[EXTRAPOLATION]` We add optional guided "learn this console" prompts (dismissible) — an aid, not in the original.

### 1.2 Advanced Campaign Mode (ACM)
- **Verified:** Dynamic, persistent campaign where *"changes can occur rapidly and often through political and military influences."* GALCOM vs. the Gammulan Empire; war between the two factions.
- **Our rules:** A persistent galaxy with a simulated clock. Missions are issued; faction relationships, station control, and threat levels evolve over time whether or not the player acts.
- **Fail states:** Battlecruiser destroyed (and no clone/rescue), or a court-martial condition (`[EXTRAPOLATION]`: repeated mission failure / hostile acts against GALCOM assets).
- **Win state:** `[EXTRAPOLATION]` The original is famously open-ended with no single "you win" screen. For v1 we define a milestone objective (e.g. repel a Gammulan incursion in the home region) as a soft campaign goal, clearly marked as our design choice.

### 1.3 Xtreme Carnage
- **Verified:** A stripped-down combat simulator, *"both planetbound and in free flight."*
- **Our rules:** Instant-action combat arena. Player picks a scenario (space dogfight, capital engagement, or ground ATV skirmish), gets dropped into a fight with no campaign persistence.
- **Win/Loss:** Destroy all hostiles = win; player craft destroyed = loss. Score tracked per run.

---

## 2. Bridge / UI Layout

- **Verified:** The player commands from the bridge through *"the large array of controls and computer interfaces"* — the notoriously menu-heavy UI. The brief confirms consoles for navigation, tactical/weapons, engineering, power allocation, communications, cargo/logistics, and personnel.
- `[EXTRAPOLATION]` The exact per-console field layout is not recoverable from accessible sources, so the console *set* below is design-accurate but the specific widgets are our reconstruction.

Proposed console set (each a switchable screen):

| Console | Displays | Controls |
|---|---|---|
| **NAV** (Navigation) | Star map, current system, jump targets, autopilot status | Set course, jump, dock/undock, autopilot |
| **TAC** (Tactical/Weapons) | Contact list, targeting, shield facing, weapon status | Select target, fire, cycle weapons, shield balancing |
| **ENG** (Engineering) | System health, damage per subsystem, repair queue | Assign repairs, reactor restart, damage-control teams |
| **PWR** (Power) | Reactor output, per-system power draw sliders | Allocate power to shields/weapons/engines/life-support |
| **COMM** (Communications) | Message log, hailing, mission briefings | Hail contact, accept/decline mission, request tow |
| **CARGO** (Logistics) | Cargo bays, fuel, spare parts, ordnance | Buy/sell/jettison, transfer to craft |
| **PERS** (Personnel) | Crew roster, location, health/fatigue, assignments | Assign to station/craft, hire, clone from DNA |
| **LAUNCH** (Flight Deck) | Fighters/shuttles/ATVs available | Launch/recall, take manual control |

**Screen switching:** A persistent top/side console bar plus function-key shortcuts (see §3). One console visible at a time (matches the era); a compact status HUD (hull, shields, power, alert level) is always on screen.

---

## 3. Controls Scheme

- **Verified (from brief):** DOS shortcuts existed such as `SHIFT+R` (restart reactor) and `CTRL+S` (request a tow). Millennium Gold later added mouse-based flight.
- `[EXTRAPOLATION]` Most specific keybindings below are our modern mapping, since a full verified keymap isn't accessible. Kept mnemonic and DOS-flavoured.

| Action | Key (proposed) | Notes |
|---|---|---|
| Switch console | `F1`–`F8` | One per console in §2 |
| Restart reactor | `Shift+R` | Verified original shortcut |
| Request tow | `Ctrl+S` | Verified original shortcut |
| Fire weapon | `Space` | |
| Cycle target | `Tab` | |
| Throttle up/down | `+` / `-` | |
| Launch/recall craft | `L` / `Shift+L` | |
| Pause | `P` | |
| Flight (craft) | `Mouse` + WASD | Mouse-flight per Millennium Gold |

Mouse: all console buttons/sliders clickable; keyboard shortcuts are accelerators, not the only path.

---

## 4. Ship Systems Simulation

- **Verified (from brief):** Systems are damageable/repairable via a parts/engineering database; decks can suffer radiation leaks or reactor breaches requiring evacuation.
- **Model (`[EXTRAPOLATION]` for numbers):**
  - **Reactor:** produces a power budget per tick. Can breach (→ radiation, forced deck evac) or shut down (→ `Shift+R` restart).
  - **Power allocation:** finite budget distributed across shields, weapons, engines, life-support, sensors. Over-draw degrades systems.
  - **Shields:** directional (fore/aft/port/starboard `[EXTRAPOLATION]`), regenerate from power, deplete under fire.
  - **Weapons:** energy (draw from power) and ordnance (draw from cargo).
  - **Damage model:** each subsystem has hit points; damage reduces effectiveness and can trigger cascading failures (e.g. radiation leak on a deck).
  - **Repair:** engineering teams + spare parts consumed over time from a repair queue.
  - **Fuel:** consumed by jumps/thrust; refuel at stations.

All numbers are approximations, not sourced values — flagged as such.

---

## 5. Crew / Personnel Simulation

- **Verified (from brief):** Crew are individually simulated — stats (health, fatigue, skill/AI attributes), move between named ship locations (quarters, galley, bridge, engineering…), can be injured, die of hunger, be reassigned to fighters/ATVs by skill, cloned from stored DNA, or hired at starstations.
- **Model:**
  - Each crew member: `health`, `fatigue`, `hunger`, `skill set`, `current location`, `assignment`.
  - **Movement:** crew path between locations to reach assignments; galley visits reduce hunger.
  - **Death:** health→0 (injury/radiation) or hunger→max. `[EXTRAPOLATION]` exact rates.
  - **Assignment:** skill-gated (e.g. pilot skill for fighters, engineering for repairs).
  - **Replacement:** clone from stored DNA, or hire at a starstation.

---

## 6. Combat Model

- **Verified:** Combat spans *"space, planetary, air and ground"*; both ship-to-ship and craft-level.
- **Space:** battlecruiser vs. capital ships/pirates; launched fighters dogfight. Resolution uses the power/shield/weapon model in §4.
- **Ground/planetary:** ATVs and marines deployed to surfaces for skirmishes. `[EXTRAPOLATION]` We model this as a simplified top-down/arena engagement in v1.
- **Resolution:** deterministic, testable combat math (attacker accuracy vs. target evasion/shields), so it can be unit-tested headlessly.

---

## 7. Galaxy / Campaign Structure

- **Verified (v2.0 scale):** 13 alien races, 25 castes; 25 star systems, 75 planets, 145 moons. GALCOM (formed 2044AD) vs. Gammulans; war ongoing.
- **Our v1 scope (`[EXTRAPOLATION]` on exact counts):** a reduced but structurally-faithful galaxy — a handful of star systems, each with planets/moons/stations, a few factions with relationship values, and simple trading (buy low/sell high across stations).
- **ACM dynamics:** a background simulation ticks faction relations, station ownership, and threat levels; missions are generated from this evolving state.

---

## 8. Known Ambiguities / Gaps

| Area | Gap | Our decision |
|---|---|---|
| Console widget layouts | No accessible field-level spec | Reconstruct plausible layouts; mark as design |
| Full keymap | Only 2 shortcuts verified | Provide modern mnemonic map |
| Win condition for ACM | Original open-ended | Add a soft milestone goal, labelled |
| Exact simulation numbers | Not verifiable | Use approximations, flagged in code/comments |
| Ground combat depth | Under-documented | Simplified arena for v1 |

---

## 9. Proposed Architecture (for Phase 2)

**Build tool: Gradle** — recommended over Maven for cleaner incremental builds, a simpler multi-module setup for the sim/UI split, and easy `application` + `test` wiring. (One-time justification; will stay consistent.)

**Package layout (MVC-ish, sim is display-free and unit-testable):**

```
com.bc3k
├── app            // main(), window bootstrap
├── engine         // game loop, GameState, screen/state manager
├── sim            // NO Swing imports — pure model + unit-tested
│   ├── ship       // reactor, power, shields, weapons, damage/repair
│   ├── crew       // crew, locations, assignment, needs
│   ├── combat     // deterministic resolution
│   ├── galaxy     // systems, planets, stations, factions, trade
│   └── campaign   // ACM dynamic state ticker
├── modes          // FreeFlight, XtremeCarnage, AdvancedCampaign controllers
└── ui             // Swing only: console screens, HUD, star map, input map
    └── consoles   // NAV/TAC/ENG/PWR/COMM/CARGO/PERS/LAUNCH panels
```

Hard rule: nothing in `com.bc3k.sim` imports `javax.swing`/`java.awt`, so all core systems test headlessly.

---

## 10. Prioritized Feature List

**Must-have (v1 playable):**
- Game loop + console/screen switching framework
- Ship sim: power allocation, reactor, shields, weapons, damage/repair (+ tests)
- Crew sim: roster, needs, movement, assignment (+ tests)
- Galaxy + NAV star map; Free Flight mode fully playable
- Xtreme Carnage space-combat arena (+ combat tests)
- Swing consoles with procedural placeholder graphics + status HUD

**Should-have:**
- Advanced Campaign Mode with dynamic faction/mission ticker
- Trading/logistics; hiring/cloning crew
- Ground/ATV combat (simplified)

**Stretch:**
- Deeper mission scripting, save/load, richer procedural art, sound.

---

## 11. Graphics Plan

Procedurally-drawn placeholders only (Graphics2D vector shapes + text-console-style panels reminiscent of the era). No original assets or box art. Any external art would be public-domain with attribution, or drawn at runtime; if a license can't be verified, we draw a placeholder instead.

---

## End-of-Phase Summary

- **Built:** Research/design doc, architecture, prioritized scope. No code.
- **Deferred:** All implementation (Phase 2+).
- **Open questions for you before Phase 2:**
  1. OK to proceed with **Gradle**?
  2. Agree with the **v1 "Must-have" cut** (Free Flight + Xtreme Carnage first, ACM as Should-have)?
  3. Accept the labelled **ACM soft-win milestone**, or keep it fully open-ended?
