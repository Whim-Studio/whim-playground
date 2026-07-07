# Babylon 5 Wars — Java 8 / Swing recreation (unofficial)

A standalone desktop recreation of the tabletop space-combat wargame **Babylon 5 Wars**,
written in pure **Java 8 + Swing** (only external runtime dependency: Gson, used by the data
layer). This module is a fan-made, non-commercial reimplementation for learning purposes.

> ⚠️ **All ship/weapon/rules numbers are APPROXIMATED and unverified against the real
> rulebook.** They live in the JSON under `src/main/resources/` and are clearly marked with an
> `"_note": "APPROXIMATED, unverified vs rulebook"`. Do not treat them as authoritative. No
> copyrighted Babylon 5 / Agents of Gaming artwork is used — every ship is drawn as an original
> faction-colored triangle.

## What it is

A hot-seat, 2-player (Side A vs Side B) tactical duel on a hex map. The bundled scenario,
`/scenarios/border-skirmish.json`, pits an **Earth Alliance Hyperion** heavy cruiser against a
**Narn G'Quan** heavy cruiser. The app renders the battle, lets each side maneuver and fire, and
narrates the result in a combat log — all driven by the already-tested `model` / `data` / `engine`
layers (this UI only *composes* their public APIs).

The window has five parts:

- **Play area** (custom `Graphics2D`): procedural starfield, hex field, each ship as a
  faction-colored triangle (nose = facing), its movement **vector**, and — for the selected ship's
  selected weapon — a firing-**arc** overlay plus dashed **range rings**.
- **Turn bar**: turn #, phase, impulse #, initiative winner, and all the action buttons.
- **Ship Control Sheet**: stat header, a per-facing armor + per-section structure **damage-box
  diagram** (green = intact, dark = destroyed), and a **weapons table**.
- **Power / EW panel**: adjust the selected ship's thrust and offensive/defensive EW.
- **Combat log**: color-coded, scrolling, fed by the engine's `GameEvent`s.

## Build & run

Requires a JDK (the module targets Java 8; it also builds and runs under JDK 17). From the
`babylon5wars/` directory:

```bash
# compile
mvn -q -DskipTests compile

# run the test suite (engine + a headless UI smoke test)
mvn -q test

# run the app (opens the window on a machine with a display)
mvn -q -DskipTests compile
mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/classes:$(cat cp.txt)" com.whim.b5wars.Main
```

On a **headless** machine (no display, or `-Djava.awt.headless=true`) `Main` does **not** open a
window: it loads the scenario, builds the game state, prints a one-line-per-ship summary, and
exits. This is what CI exercises:

```bash
java -Djava.awt.headless=true -cp "target/classes:$(cat cp.txt)" com.whim.b5wars.Main
```

If you have the Maven exec plugin available you can instead run:

```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.whim.b5wars.Main
```

## Controls

The game is a per-turn finite-state machine: **INITIATIVE → POWER → EW → IMPULSE → (End Turn)**.

1. **Advance Phase** walks the machine. On INITIATIVE it rolls initiative; on POWER it sets each
   ship's thrust; on EW it sets a default EW split.
2. During **POWER / EW**, tweak the selected ship's thrust / EW in the right-hand panel.
3. During **IMPULSE**, select a ship (click it on the map), then use the toolbar:
   - **Forward** — advance one hex along the nose (bounded by Speed).
   - **Turn ⟲ / ⟳** — rotate one hexside (needs enough straight hexes for the ship's turn-mode
     and available thrust).
   - **Slip ◀ / ▶** — sideslip one hex laterally (costs thrust).
   - **Accel + / Decel −** — change Speed (costs thrust, bounded by max speed).
   - **Fire…** — open the weapon-fire dialog: pick a weapon and target, see the computed to-hit
     and whether the target is in arc/range, and resolve the shot.
4. **End Turn ▶** (the Advance button during IMPULSE) resolves end-of-turn bookkeeping (shield
   regen, powerless drift, victory check) and rolls to the next turn.

Click any ship to select it; click a weapon row in the ship sheet to show that weapon's arc and
range rings on the map. **Game ▸ New Game** restarts from the fixed seed;
**Game ▸ Save Snapshot…** writes a readable text dump of the current state.

### Notes / limitations

- **Determinism**: the game uses a fixed dice seed (`Main.DEFAULT_SEED = 424242`), so a given
  sequence of actions is reproducible.
- **Save/Load**: only a text *snapshot* is provided. A full binary save/load round-trip is
  intentionally omitted — the `model`/`engine` types are not `Serializable` and are owned by other
  tasks, so it cannot be added from the UI layer without changing them.
- **Manual IMPULSE**: the UI drives movement and fire itself rather than using the engine's
  headless auto-resolver, composing the engine's existing public methods (`MovementEngine`,
  `CombatEngine`, `TurnManager.advancePhase` / `endTurn`). No engine methods were added or changed.
