# Kenshi (demake) — standalone Java 8 Swing

A zero-dependency, procedural-graphics 2D top-down demake of the sandbox RPG
**Kenshi**. Squad management, real-time-with-pause tactics, a brutal seven-part
limb-damage system, hunger and blood-loss survival, faction AI, and a pannable
zoomable camera — all drawn with `Graphics2D` primitives, no external assets.

## Run

```bash
cd kenshi
mvn -q clean package
java -jar target/kenshi-1.0.0.jar
```

Java 8+. No libraries beyond the JDK.

## Controls

- **Left-click** a unit to select; **left-drag** a box to select many.
- **Right-click** ground to move, an enemy to attack, a town to interact.
- **Space** toggles pause (real-time-with-pause). **1 / 2 / 4** set game speed.
- **Mouse wheel** zooms; **arrow keys / edge-drag** pan the camera.

## Architecture

The code is split into four packages behind a single UI↔engine seam
(`api.GameController`). See `../KENSHI_CONTRACT.md` for the full contract.

- `com.whim.kenshi.api` — shared `Config`, `Enums`, `Views`, `GameController`.
- `com.whim.kenshi.domain` — characters, anatomy, squads, factions, world.
- `com.whim.kenshi.engine` — tick loop, AI, pathfinding, combat, survival.
- `com.whim.kenshi.ui` — camera, renderer, HUD, input.
- `com.whim.kenshi.app` — `Main` wiring engine + UI.
