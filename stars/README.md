# Stars! — Clean-room Java 8 / Swing recreation

A faithful, playable recreation of **Stars!** (1995), the turn-based 4X space-strategy
game by Jeff Johnson & Jeff McBride. Personal/educational preservation project — no
copyrighted assets; all graphics are drawn procedurally with `Graphics2D`.

> Clean-room: mechanics and terminology (PRTs/LRTs, habitability bands, the six tech
> fields, mineral production, warp = ly²/year) follow published community references.
> Every formula whose exact source value is uncertain is isolated in
> `model/formulas/Formulas.java` and tagged `SOURCE:` (confirmed) or `RECONSTRUCTED:`
> (best reconstruction, pending balance verification). See `docs/design-reference.md`.

## Requirements

- JDK 8 (language level 8 only — no `var`, records, switch expressions, or modules).
- No external runtime dependencies; Swing + the JDK standard library only.

## Build & run

Plain JDK (no Maven needed):

```bash
cd stars
mkdir -p out
find src -name '*.java' > sources.txt
javac -d out @sources.txt

# Launch the desktop UI (needs a display):
java -cp out com.whim.stars.app.Main

# On a headless machine, Main runs a 10-year console simulation instead.
```

With Maven (optional):

```bash
mvn package
java -jar target/stars-recreation-1.0.0-SNAPSHOT.jar
```

## Verify (dependency-free self-tests)

Each layer ships a runnable, JUnit-free self-test:

```bash
java -cp out com.whim.stars.model.SelfTest     # Phase 1 — data model (23 checks)
java -cp out com.whim.stars.sim.SimSelfTest    # Phase 2 — turn engine (re-runs Phase 1 + 11 sim checks, incl. determinism)
java -cp out com.whim.stars.io.SaveGameTest    # Phase 3 — persistence round-trip (10 checks)
java -cp out com.whim.stars.sim.ai.AiSelfTest  # Phase 7 — AI expansion + determinism (6 checks)
```

## Playing the demo

`Game ▸ New Game` opens a wizard (universe size, race name + primary trait, number of AI
opponents, 1–3). You start blue; AI rivals get their own colours. Click a planet to inspect
it in the **Planet Report** panel; press **Generate Turn ▶** (or `F9`) to advance a year —
the AI takes its turn first. Drag to pan the star map, scroll to zoom.

The **Commands** menu opens the full screens:

- **Research** (`F5`) — per-field levels, cost-to-next, current field, budget, next-field policy.
- **Ship Designs** — browse/create designs: pick a hull, fill slots from compatible
  components, watch mass/warp/firepower/cost update live.
- **Production** — edit the selected planet's build queue (factories, mines, defenses,
  scanner, ships, auto-build; reorder/remove).
- **Fleets** — composition/fuel/cargo, add waypoints (target/warp/task), and transfer
  colonists/minerals to or from an orbited colony.
- **Relations / Battle Plans** — set Friend/Neutral/Enemy toward each rival.

`Game ▸ Save…/Load…` writes a compressed `.starsave` file.

## Architecture (MVC)

| Package | Role | Notes |
|---|---|---|
| `com.whim.stars.model` | Model (data) | Pure Java, **no `javax.swing` imports**. Serializable object graph rooted at `Galaxy`. |
| `com.whim.stars.model.formulas` | Formulas | All uncertain numbers live here, one-line correctable. |
| `com.whim.stars.sim` | Model (engine) | `TurnEngine.generateTurn()` — deterministic 7-step yearly resolution. |
| `com.whim.stars.io` | Persistence | Java serialization, GZIP + magic/version header. |
| `com.whim.stars.sim.ai` | Model (AI) | `SimpleAi` — deterministic heuristic opponent; issues orders the engine resolves normally. |
| `com.whim.stars.ui` | View + Controller | `Graphics2D` star map + the research/design/production/fleet/report/relations screens and the new-game wizard. |
| `com.whim.stars.app` | Bootstrap | `Main` (entry point), `GameSetup` + `GalaxyFactory` (galaxy generation), `DemoGalaxy` (default scenario). |

## Implemented vs. TODO

**Implemented (Phases 1–7):** core data model (races/PRTs/LRTs, habitability, planets,
fleets, ship designs, tech); deterministic turn engine (mining, production queues incl.
auto-build, population growth/die-off, research level-ups, fuel-limited fleet movement,
colonization, simplified combat); save/load; a pannable/zoomable Swing star map; the full
screen set (research, ship-design editor, production-queue editor, fleet management +
cargo transfer, planet report, relations/battle plans); a new-game wizard (size, race,
AI count); and a deterministic heuristic **AI opponent** that expands and colonizes.

**TODO / simplified (see `docs/design-reference.md` for the full gap list):** the real
10×10 tactical battle board (current combat is a deterministic attrition exchange), the
full component/hull tech tables, minefields/packets/stargates, ground invasion,
a terraforming action, victory conditions & scoring, and a stronger strategic AI
(the current one is expansion-focused, not military). These are the next build phases.
