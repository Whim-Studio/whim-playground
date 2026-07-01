# War Room: Tactical Sandbox

A standalone **Java 8 + Swing** real-time tactical sandbox. A top-down 2D
battlefield where you — the commander — design a scenario in **Simulation Mode**
(generate terrain, place cross-era units, draw synchronized movement routes with
arrival times, drop markers), then press **Play** to hand control to a real-time
**Battle Mode** engine that moves units along their routes, fires synchronized
detonations, resolves combat, and drives morale, panic and rout via tactical
stances. No external libraries and no build system — every visual is drawn
procedurally with `Graphics2D` (no image files are ever loaded).

## Build & run

```bash
cd warroom
javac -d out $(find src -name "*.java")
java -cp out com.whim.warroom.app.Main
```

This compiles every source file under `src/` into `out/` and launches the
sandbox. A Java 8 (or newer) JDK is required.

Engine self-check (no UI):

```bash
java -cp out com.whim.warroom.engine.EngineSmokeTest
```

## How to use

The window opens in **Simulation Mode** with a small demo scenario already laid
out (a generated map, BLUE advancing from the west and RED holding the east,
routes converging on a central objective, and one RED heavy charging in with a
timed detonation) — press **Play** to watch it unfold immediately.

1. **Editor palette (left)** — the Simulation-mode toolbox:
   - **Terrain** — pick a *dominant* biome and **Regenerate** the whole map
     (`MapState.generate`, deterministic per seed), or select a biome brush and
     use the **Paint Land** tool to paint tiles. Elevation is shown as shading.
   - **Faction / Stance** — choose the faction (BLUE / RED / NEUTRAL) and stance
     (Offensive / Defensive / Retreat) applied to newly placed units.
   - **Tools** — the active battlefield mouse tool: Place Unit, Draw Route,
     Select Box, Marker, Paint Land.
   - **Unit Catalog** — every archetype from `UnitCatalog`, grouped by **Era**
     (Antiquity / Medieval / Modern) with a stat glance (Health / Attack /
     Range). Clicking one selects it and switches to the Place-Unit tool.
2. **Battlefield (center)** — the map. Mouse:
   - **Place Unit** tool: click to drop the selected unit.
   - **Draw Route** tool: click a unit to start its route, click to add each
     waypoint (arrival ticks are auto-paced from the unit's speed), then
     **right-click** to finish.
   - **Select Box** tool: drag a box to select units.
   - **Marker** tool: click to drop a labeled commander marker.
   - **Middle-drag** (or Alt-drag) to pan; **mouse wheel** to zoom; `F` fits the
     map to the viewport.
3. **Transport bar (bottom)** — playback controls:
   - **▶ Play** — switch to Battle Mode and start the engine (`SimEngine.play()`).
   - **❙❙ Pause** — freeze at the current tick.
   - **⏩** — Fast-Forward: cycles the playback speed (0.25 / 0.5 / 1 / 2 / 4×)
     via `setSpeed`; this is wall-clock only and never changes tick math.
   - **timeline** — scrub/rewind across `0..getMaxSimTick()` (`seek`).
   - **◀ Editor** — reset the engine to tick 0 and return to Simulation Mode.
   - **🎬 Cinema** — hide all chrome (editor + transport bar) for a clean view.
     Restore it with the `C` hotkey, `Esc`, or by hovering the very top edge of
     the battlefield.

Hotkeys: **Space** play/pause, **C** toggle cinema, **Esc** exit cinema, **F**
fit map.

The whole run is reproducible from its seed: a single shared `Random` (owned by
the domain `SandboxState`) seeds terrain generation and the engine, so rewind and
seek return the exact same frames.

## Architecture

Three packages, split for a parallel build against a fixed interface contract
(`WAR_ROOM_CONTRACT.md` at the repo root):

- `com.whim.warroom.domain` — core model, the `SimEngine` / `SimListener`
  interfaces, the map generator, the cross-era `UnitCatalog`, and the immutable
  `SimSnapshot` render frame. No Swing/AWT (except reading `java.awt.Color`).
- `com.whim.warroom.engine` — pure simulation logic implementing `SimEngine`
  (route movement, combat, morale/rout, detonations) on a background tick thread
  that publishes immutable snapshots. No Swing/AWT.
- `com.whim.warroom.ui` / `.app` — this layer. Swing only; it renders domain
  state and snapshots and calls the engine through a thin `SandboxController`. It
  never computes a game rule and never blocks the EDT — engine frames arrive on
  the background thread and are marshaled onto the EDT with
  `SwingUtilities.invokeLater`.

UI files: `WarRoomFrame` (mode switching + cinema), `BattlefieldPanel`
(Graphics2D render + all mouse input), `EditorPanel` (west tool palette),
`PlaybackBar` (south transport), `SandboxController` (wiring + `SimListener`),
`ThemeUI` (shared colors/fonts), and the `Main` EDT bootstrap.
