# Battlecruiser 3000AD — Recreation: Known Issues & Simplifications

Phase 6 deliverable. This is the honest ledger of what the Java 8 / Swing recreation
**approximates, simplifies, or omits** relative to the 1996 original, with rationale.
Nothing here is a bug to "fix later" unless marked so — most are deliberate scope cuts
for a v1 playable milestone. Cross-reference: `BC3K_Phase1_Design.md` (research/design).

## Verification status (important)

The build was authored in a container **without a JDK or Maven**, so it has **not been
compiled or run here**. All ~37 unit tests were written and hand-traced but need
`mvn test` on a JDK 8 machine to confirm. Two self-inflicted issues found during
authoring were fixed (an `alert()` self-recursion; a dangling `describe()` reference).
Treat "playable" claims as *design-complete and logic-traced*, not *executed*.

## What is implemented (v1)

| System | Status |
|---|---|
| Bridge shell: 8 switchable consoles, always-on HUD, ~60 FPS loop | Done |
| Free Flight mode (explore, no hostiles) | Done |
| Xtreme Carnage mode (ship-to-ship combat loop) | Done |
| Advanced Campaign mode (threat ticker + rotating objectives) | Done (thin slice) |
| Ship sim: reactor budget, power allocation, hull/shields, damage/repair, breach | Done |
| Crew sim: health/fatigue/hunger, movement, starvation, hiring, DNA cloning, best-fit | Done |
| Galaxy/nav: 5-system sector, jump links, fuel cost, visited tracking | Done |
| Logistics: credits, fuel, parts, ordnance, refuel | Done |
| Flight deck: fighter/shuttle/ATV launch & recall (as counts) | Done |
| Procedural placeholder art (starfield + vector HUD); no original assets | Done |

## Deliberate simplifications vs. the original

1. **Galaxy scale.** Original v2.0: ~25 systems / 75 planets / 145 moons. Ours: a fixed
   5-system sector. *Why:* enough to exercise navigation/fuel without content bloat.
   *Extensible:* `Galaxy.defaultSector()` is the single seam to enlarge.

2. **Crew simulation depth.** We model health/fatigue/hunger, walking between named
   locations, death, and DNA cloning. We do **not** model the original's full per-crew
   AI, radiation-specific injury tables, per-deck pathfinding, or morale politics.
   *Why:* the felt loop (feed/rest/assign/replace) is present; the rest is content.

3. **Combat is abstracted, not spatial.** Xtreme Carnage resolves as deterministic
   volley exchange (damage = weapon power × integrity) rather than 3D dogfighting with
   projectile flight, facing, and range. *Why:* keeps combat unit-testable and matches
   the "practice combat" intent; **fighter dogfights and planetary/ATV ground combat
   are represented only as launch/recall counts, not playable engagements** (biggest cut).

4. **Advanced Campaign is a thin slice.** The original ACM was a sprawling dynamic sim of
   politics and fleet movement. Ours models one felt dynamic: a rising Gammulan **threat**
   the player pushes back by resolving rotating **objectives**. No faction diplomacy,
   economy simulation, or emergent events. *Why:* makes Campaign genuinely distinct from
   Free Flight within v1 scope; flagged as approximation, not the real ACM.

5. **Consoles are functional, not exhaustive.** Each console exposes its core loop but
   not every original field/sub-menu (e.g. no detailed per-part engineering database UI;
   repair is a single "restore integrity" action). *Why:* the original's notorious
   menu depth is explicitly out of scope for a clean v1.

6. **Controls are a modern mapping.** Only two original shortcuts are verified
   (`Shift+R` reactor restart, `Ctrl+S` tow) and are honored; everything else
   (`F1`–`F8` consoles, arrows, `SPACE` fire, crew/craft keys) is our mnemonic scheme,
   not the DOS original. *Why:* the full original keymap was not recoverable from
   accessible sources.

7. **No save/load, no sound.** Out of scope for v1. *Why:* not needed to demonstrate the
   structure and loops; `saves/` and a `SaveManager` seam can be added like the sibling
   projects in this repo.

8. **Numbers are design approximations.** Every magnitude (reactor 100 units, 15 fuel/jump,
   volley coefficients, need rates) is invented for balance and labelled as such — the
   original's exact values were not verifiable. *Why:* the no-fabrication rule.

## Known open items / follow-ups

- **[compile]** Build & run `mvn test` on JDK 8 to confirm the recreation compiles and the
  suite passes; fix any issues surfaced (none expected from tracing, but unverified here).
- **[balance]** Free Flight shields out-regen enemy fire at default power; intentional
  (player is tanky unless they divert power to weapons), but worth tuning after playtest.
- **[content]** Enlarge the galaxy, add planets/moons and starstation services beyond refuel.
- **[combat]** If desired later: promote fighter/ATV counts into actual playable engagements.

## Package map (final)

```
com.whim.bc3k
├── app       // shell: Main, GameFrame, ScreenManager, Menu/Console/End screens
├── api       // GameController seam, Views, Screen, Enums, ActionResult
├── engine    // Engine — binds sim modules behind the seam
├── sim       // pure model (no Swing): ship, crew, galaxy, combat, campaign
└── render    // Palette, UiKit, Starfield (Java2D helpers)
```
