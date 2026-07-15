# Progress log

Phase-by-phase build log per the project plan. Each entry records what is
runnable, what is stubbed, what was deferred, and any assumptions made.

## Phase 0 — Scaffold ✅ (2026-07-15)

**Implemented & runnable**
- Package layout under `com.whim.settlers.{app,engine,map,buildings,economy,transport,military,ai,ui,io}`.
- Plain `javac`/`java` build (documented in `README.md`); no external deps. Compiles clean with `javac --release 8 -Xlint:all`.
- Fixed-timestep game loop (`engine/GameLoop`): 60 Hz simulation via an accumulator, decoupled from render rate, with active rendering through an AWT `Canvas` + `BufferStrategy` (double-buffered) — not passive `repaint()`.
- Empty/placeholder tile map (`map/TileMap`, `map/TerrainType`) with a deterministic filler landscape (grass + a lake + scattered forest/mountain) so there is terrain to view.
- Pan/zoom camera (`engine/Camera`): WASD/arrow pan, mouse-wheel zoom-to-cursor, right/middle-drag pan; world↔screen transforms with viewport culling in the renderer.
- Headless self-test (`engine/SelfTest`) exercising map, camera round-trip, zoom-anchor invariance, and world-clock stepping. Runs automatically when no display is present.

**Stubbed / placeholder**
- Terrain generation is a simple deterministic filler, not the real seeded generator (Phase 1).
- No buildings, settlers, roads, players, or HUD beyond a debug overlay.

**Deferred / notes**
- Rendering is top-down square tiles for the scaffold; the original is isometric. Isometric projection is deferred — it is a `Camera`/`Renderer` change that does not affect the simulation model, and can be introduced in a later phase.
- Verified by compiling with JDK 17 using `--release 8` to pin the Java 8 language/API level (no JDK 8 was available in the container). The desktop window itself was not opened in this environment (headless container); the loop, camera, and map are covered by the self-test.

**Verification:** `javac --release 8 -Xlint:all` — clean; headless self-test — 5/5 checks pass.

## Phase 1 — Terrain & map ✅ (2026-07-15)

**Implemented & runnable**
- Terrain model extended (`map/TerrainType`): grass, forest, desert, water, and four mountain-by-resource variants (coal/iron/gold/stone), each with a placeholder colour, a `buildable` flag, `isMountain()`/`isWater()`, and a stable single-char `code()`/`fromCode()` for the text map format.
- Seeded procedural generator (`map/MapGenerator`): dependency-free fractal (fBm) value noise with elevation + moisture + mineral fields and a radial edge falloff (land-centred maps). Elevation is renormalised so water/land/mountain always appear. Same seed ⇒ identical map.
- Hand-built map loading (`io/MapLoader`): human-editable text format (`#` comments + one char per tile); loads from file or classpath resource. Sample scenario at `maps/tutorial-valley.map`.
- CLI map selection in `Main`: `--seed <n>` (generated) or `--map <file>` (hand-built), default generated seed 1993.
- Minimap (`ui/Minimap`): cached scaled overview in the bottom-right with the live camera viewport outlined; **left-click to recentre** the camera. Wired through `Renderer`/`InputHandler`; added `Camera.centreOn`.

**Stubbed / placeholder**
- No rivers, biome smoothing, or resource-density gradients within a mountain — mineral is per-tile. Fine for gameplay; can be refined in Phase 8.

**Deferred / notes**
- Still top-down square tiles (isometric deferred, as in Phase 0).
- `TileMap.flat()` retained as a simple filler helper; `Main` now uses `MapGenerator`.

**Verification:** `javac --release 8 -Xlint:all` — clean; headless self-test — 8/8 (adds generator-determinism, terrain-variety, and loader-parsing checks); `maps/tutorial-valley.map` loads to 26×20 with all terrain types present.

## Phase 2 — Buildings & construction — _not started_
## Phase 2 — Buildings & construction — _not started_
## Phase 3 — Economy simulation — _not started_
## Phase 4 — Roads & transport — _not started_
## Phase 5 — Military & territory — _not started_
## Phase 6 — AI opponent(s) — _not started_
## Phase 7 — UI/UX polish & meta — _not started_
## Phase 8 — Hardening — _not started_
