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

## Phase 2 — Buildings & construction ✅ (2026-07-15)

**Implemented & runnable**
- Full building roster (`buildings/BuildingType`, 25 types) grouped by category (HQ, Wood, Stone, Food, Mines, Metal, Tools, Military, Shipping), each with footprint size, placement rule, build time, glyph/colour, and (for Phase 3) planks/stone costs `// approximate`.
- Placement rules & validity (`buildings/BuildingManager`, `PlacementRule`): LAND (buildable land), MOUNTAIN (mines — with per-mine resource match, e.g. Coal Mine only on a coal mountain), COAST (fisherman/shipyard, adjacent to water). Footprint-aware bounds/overlap checks via an occupancy grid.
- Construction lifecycle (`buildings/Building`, `BuildingState`): non-Castle buildings start `UNDER_CONSTRUCTION` at progress 0 and advance to `FINISHED` over their build time (drawing from a stubbed unlimited resource pool this phase); the Castle is founded finished.
- Founding: `World.foundSettlement()` places the Castle on the nearest buildable spot to map centre and centres the camera there.
- Build UI (`ui/BuildMenu`): left-edge palette with category headers and one clickable row per building; click arms placement, the armed type is highlighted. Renderer draws placed buildings (footprint block + glyph + construction progress bar) and a **green/red placement ghost** that follows the cursor and reflects validity. Input routes clicks menu → minimap → world placement; right-click cancels.

**Stubbed / placeholder**
- Construction is time-only; no planks/stone are consumed yet (Phase 3 wires real costs and a warehouse inventory).
- No settlers, production, or building function beyond placement/construction.

**Deferred / notes**
- Interactive Castle-founding (click to place the HQ at game start) is deferred to Phase 7's new-game flow; for now the Castle is auto-placed at centre.
- Multi-tile footprints are axis-aligned blocks; the original's irregular building shapes are cosmetic and deferred to art polish.

**Verification:** `javac --release 8 -Xlint:all` — clean; headless self-test — 12/12 (adds placement-rules, footprint-overlap, construction-completion, and found-settlement checks).

## Phase 3 — Economy simulation ✅ (2026-07-15)

**Implemented & runnable**
- Goods & inventory (`economy/Good`, `GoodCategory`, `Inventory`): 25 goods across raw/material/food/tool/weapon/coin; `Inventory` is the stockpile and per-building buffer primitive.
- Settler roles (`economy/Profession`, `Settler`): ~20 professions plus IDLE/BUILDER/CARRIER, modelled per-settler (role + state) so Phase 4 can attach carrier movement.
- Production chains (`economy/Recipe`, `ProductionChains`): a `Recipe` per productive building — inputs → output, build time, required-tool gate, food-consuming mines, and extractor terrain needs (forest/rock/water/farmland/mountain). Realises the full dependency graph: Wood→Planks, Grain→Flour(+Water)→Bread, Pig→Meat, Fish, ores→Iron/Gold, Iron+Planks→tools, Iron+Coal→sword/shield.
- Simulation (`economy/Economy`): settler spawning from the Castle (cap raised by warehouses), tool-gated staffing (a building is only staffed once its specific tool exists — the deliberate bottleneck), and per-building production stepping. **Renewable wood**: woodcutters fell nearby forest (FOREST→GRASS); foresters replant (GRASS→FOREST) — running out of trees is a real stall state. Mines consume one food per cycle. Transport is **stubbed instant** via one central stockpile (Phase 4 swaps in the flag relay).
- Distribution priority: per-building-type priority (1–9); scarce inputs are serviced highest-priority-first each tick, so raising a consumer's priority genuinely lets it win contested goods.
- Tool priority: player-ordered list controls which tool the Tool Maker builds next among those in demand.
- **UI** (`ui/EconomyPanel`, toggle **E**): live stockpile, population, reorderable tool-priority list, and +/- supply-priority controls for the contested consumers. Buildings show a live status caption (working / needs-tool / no-input / no-trees / no-food) and construction progress.

**Stubbed / placeholder**
- Transport is instant (central stockpile) — replaced in Phase 4.
- Quarry/mines don't deplete their rock/ore yet (only forests deplete, to exercise the forester loop); finite ore is a Phase 8 tuning item.
- Settlers are staff counts + per-building worker objects; carriers get real positions in Phase 4.

**Verification:** `javac --release 8 -Xlint:all` — clean; headless self-test — 15/15 (adds a live woodcutter→sawmill→planks chain, forester replanting, and tool-gated-staffing checks).

## Phase 4 — Roads & transport — _not started_
## Phase 4 — Roads & transport — _not started_
## Phase 5 — Military & territory — _not started_
## Phase 6 — AI opponent(s) — _not started_
## Phase 7 — UI/UX polish & meta — _not started_
## Phase 8 — Hardening — _not started_
