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
## Phase 4 — Roads & transport ✅ (2026-07-15)

**Implemented & runnable**
- Flag/road graph (`transport/Flag`, `Road`, `RoadNetwork`): flags are relay hubs with FIFO good queues; roads join two flags along a fixed tile path. BFS routing (`nextHop`, `connected`) over the flag graph.
- Road routing (`transport/Pathfinder`): 4-directional A* lays a road between two flags over walkable tiles (not water/mountain/buildings).
- **Flag-relay carriers** (`transport/Road`): one carrier per segment picks up a good at a flag, walks the length to the far flag (time ∝ length), sets it down, then walks back empty before carrying again — a genuine per-segment throughput limit. A good relays flag→flag until it reaches its destination; congestion emerges when carriers are busy (waiting counts shown at flags). **Not teleport.**
- Transport orchestration (`transport/TransportSystem`): auto-places a flag beside every building, builds roads via A*, and each tick advances carriers then dispatches queued goods onto free segments.
- **Economy now flows over roads** (`economy/Economy` reworked): producers ship output to the Castle stockpile and consumers request inputs from it, both as physical relayed shipments into per-building input buffers. A building with no road to the Castle can neither be staffed nor produce ("no road"). The Phase-3 recipe/staffing logic is otherwise unchanged.
- UX: **F** arms the flag tool (click land), **R** arms the road tool (click two flags), **Esc**/right-click cancels. Renderer draws roads, flags (with waiting-count congestion indicator), and carriers as dots moving along their road; HUD shows the active tool.

**Stubbed / placeholder / notes**
- **Topology simplification (documented):** goods route producer→Castle→consumer (Castle is the hub) rather than arbitrary producer→consumer. The physical relay — flags, per-segment carriers, multi-hop, congestion, road-connectivity requirement — is genuine; only the routing target is simplified. Recorded in `docs/GDD.md`.
- One carrier per road (no auto-added second carrier / donkeys yet); waterway/ship crossings remain a stretch feature.
- Staffing consumes a tool from the stockpile without animating its delivery; carrier bodies are visualised per-segment rather than individually pathed between jobs.

**Verification:** `javac --release 8 -Xlint:all` — clean; headless self-test — 16/16, incl. a **road-gated relay** check (no wood reaches the stockpile without a road; it does once connected) and the full woodcutter→Castle→sawmill→Castle plank chain over roads.

## Phase 5 — Military & territory ✅ (2026-07-15)

**Implemented & runnable**
- Players (`military/Players`): human (0) and a static enemy (1) with colours; `World.spawnEnemy()` places an enemy Castle + Guard Hut with seeded garrisons.
- Knights & ranks (`military/Knight`): five ranks, experience-based promotion (faster in the Castle), strength scales with rank.
- Military system (`military/MilitarySystem`):
  - **Territory** = union of radii around every finished, garrisoned fort (Castle/Guard Hut/Tower/Garrison, ascending radii); each tile owned by the nearest claiming fort's player. Recomputed every 0.5 s.
  - **Knight production** — settlers + a sword + a shield become knights (human), filling fort garrisons up to per-tier capacity; player sets the knight target and default rank.
  - **Morale** — gold coins delivered to a fort raise its morale (a combat multiplier).
  - **Attack/defend** — send N knights from your nearest forts at an enemy fort; strength (Σ rank × morale) resolves after a march delay. Win ⇒ capture (owner flips, survivors garrison it), territory recomputes, and the loser's buildings left outside their remaining borders go neutral. Defence is automatic.
- **Territory-gated building** (`World.canPlayerPlace`): ordinary buildings must sit inside your borders; military buildings may sit on/just beyond them to expand — the only way to grow territory. The Castle is exempt (it founds the first territory).
- Economy is now correctly scoped to the human player (enemy buildings are skipped).
- UI: territory tint + borders per player, owner-tinted fort outlines with `knights/cap` labels, and a bottom **Military bar** (knight target, default rank, and — when you click an enemy fort — an attack panel with a send-count selector and Attack button).

**Stubbed / placeholder / notes**
- The enemy is static this phase (no economy or AI); **Phase 6** gives it a brain.
- Combat resolves by aggregate strength with a fixed march time `// approximate`; no per-knight battle animation. Captured non-fort buildings become inert-neutral rather than being physically removed (no building-removal path yet).

**Verification:** `javac --release 8 -Xlint:all` — clean; headless self-test — 19/19 (adds territory-claim, attack-captures-weaker-fort, and attack-fails-vs-stronger-fort checks).

## Phase 6 — AI opponent(s) ✅ (2026-07-15)

**Implemented & runnable**
- **Per-player economy**: `Economy` is now scoped to a player id and only touches that player's buildings, over a shared road network with per-player Castle stockpile hubs (`TransportSystem` tracks a Castle flag per player). The human and AI run the *identical* production/staffing/transport rules.
- **Multi-player military** (`MilitarySystem`): knight production, morale, and knobs are per-player; `launchAttack(attacker, target, count)` works for any player. Territory already supported multiple owners.
- **AI opponent** (`ai/AiController`): a computer player that, using only the same public systems the UI drives (no shortcuts): places economy buildings inside its territory in dependency order with greedy resource-aware site search, **connects each to its Castle with a road** (goods relay exactly like the human's), expands its border with Guard Huts toward the enemy, and launches threshold-based attacks once it has a striking force. The enemy settlement spawned in Phase 5 is now driven by this AI.

**Stubbed / placeholder / notes**
- One AI personality (fixed build order + greedy siting + threshold attacks) — competent, not an optimiser; the peaceful↔aggressive personality slider and multiple AIs are Phase 7/meta polish.
- AI roads are one-per-building stars to its Castle (functional, not optimised); it doesn't yet re-plan when a site is lost.

**Verification:** `javac --release 8 -Xlint:all` — clean; headless self-test — 20/20 (adds a full two-player run asserting the AI raises ≥3 buildings and lays roads to connect them). Existing single-player checks still pass under the per-player refactor.

## Phase 7 — UI/UX polish & meta — _not started_
## Phase 8 — Hardening — _not started_
