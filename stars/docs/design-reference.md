# Stars! — Game Design Reference Document

Clean-room design reference for the recreation in this directory. It records the
mechanics the implementation targets and, crucially, **flags every value that is a
reconstruction rather than a confirmed source figure**. Confirmed values are marked
`[SOURCE]`; best-effort reconstructions are marked `[RECON]` and are isolated in code in
`model/formulas/Formulas.java`, `sim/Economy.java`, and `model/race/Race.java` so a
corrected number is always a one-line change.

**Source types referenced (community-preservation material):** the original Stars! manual
& help file; the Stars!/StarsAutoHost community wikis and strategy guides; the
`rec.games.computer.stars` newsgroup archives; and community "supplement"/formula
compendiums. Exact per-component tables are voluminous and are treated as later-phase
data; where this document needs a number that those tables would supply, it is `[RECON]`.

---

## 1. Core game loop

- The game advances in **yearly turns** starting in **2400 `[SOURCE]`**. A single-player
  "generate turn" resolves one year.
- The original is a **simultaneous-turn** game: all players submit orders, then the host
  resolves them in a fixed order. This recreation resolves one deterministic order per
  year (`TurnEngine.generateTurn()`): **movement → production → mining → growth → research
  → combat → year++**. The step ordering is a documented simplification `[RECON]`; the
  original interleaves scrapping, packet/minefield decay, and detection that are not yet
  modelled.
- Determinism is a hard design rule: **no RNG in the engine**. Identical input state
  always yields identical output (asserted by `SimSelfTest`).

## 2. Galaxy generation

- Square universes by size (width in light-years) `[SOURCE]` order-of-magnitude, exact
  planet counts `[RECON]`:

  | Size | Width (ly) | Typical planets |
  |---|---|---|
  | Tiny | 400 | ~24 |
  | Small | 800 | ~60 |
  | Medium | 1200 | ~128 |
  | Large | 1600 | ~240 |
  | Huge | 2000 | ~400 |

- Planets have a fixed position, three environment axes and mineral concentrations.
  Universe "shapes"/clumping and wrap options from the original are not modelled; the
  demo scatters worlds uniformly with a fixed seed (`DemoGalaxy`).

## 3. Races

- **Primary Racial Trait (PRT)** — one per race. The ten PRTs (`model/race/PRT.java`):
  Hyper-Expansion (HE), Super Stealth (SS), War Monger (WM), Claim Adjuster (CA),
  Inner Strength (IS), Space Demolition (SD), Packet Physics (PP), Interstellar Traveler
  (IT), Alternate Reality (AR), Jack of All Trades (JOAT) `[SOURCE]` (names/roster). Their
  full mechanical effects are enumerated but only partially wired into the engine `[RECON]`.
- **Lesser Racial Traits (LRTs)** — a set of optional modifiers (`model/race/LRT.java`),
  e.g. Improved Fuel Efficiency, Total Terraforming, Advanced Remote Mining, No Ramscoop
  Engines, Cheap Engines, etc. `[SOURCE]` (roster); effects are data-only for now.
- **Habitability** uses three bands — **Gravity, Temperature, Radiation** — each a
  centre ± half-width on a 0..100 scale (`model/race/HabBand.java`). A world inside all
  bands is green (positive hab up to +1 at centre); outside any band it is red (negative).
  The sign/ordering (ideal > band-edge > hostile) is faithful; the precise red-value curve
  is `[RECON]` (`Formulas.habitability`). Immune bands (a race indifferent to an axis) are
  supported.

## 4. Research

- Six independent fields `[SOURCE]`: **Energy, Weapons, Propulsion, Construction,
  Electronics, Biotechnology**, each level 0–26.
- Cost to reach the next level rises steeply with that field's own level **and** with the
  sum of the other fields' levels (the well-known Stars! cross-field interplay). The exact
  polynomial is `[RECON]` (`Formulas.researchCost`), structured to reproduce the qualitative
  curve; the true table can be dropped in unchanged.
- Each field gates hull types, components, and planetary installations. Field-unlock tables
  are later-phase data.

## 5. Planets & production

- **Colonization** requires a colony ship delivering colonists; the colonizer is consumed
  becoming the colony `[SOURCE]`.
- **Population**: logistic growth throttled by crowding as pop nears the world's max, with
  die-off on red worlds. Max pop scales with habitability against a base of 1,000,000 on a
  green world `[RECON]` (`Formulas.maxPopulation`, `populationGrowth`).
- **Mining** of Ironium/Boranium/Germanium scales with operable mines and concentration;
  concentration slowly depletes `[RECON]` (`Economy.miningOutput`).
- **Resources** come from population + operable factories; factories/mines are staffed per
  10k colonists (race-wizard knobs on `Race`) `[RECON]` mapping.
- **Production queue** (`ProductionItem`): factories, mines, defenses, planetary scanner,
  ships, and open-ended **auto-build** entries (factories/mines to max). The engine spends
  resources + surface minerals down the queue, banking partial progress and stopping at the
  first item it cannot finish `[SOURCE]` (queue semantics), costs `[RECON]`.
- **Terraforming** is modelled in the data (environment is mutable) but no terraform action
  is wired yet — TODO.

## 6. Ship design & components

- A **design** is one **hull** with typed **component slots** filled with components
  (`ShipDesign`, `HullType`, `Component`, `ComponentCategory`). Slots accept only their
  category; a design needs ≥1 engine to be valid (starbases excepted). Derived stats
  (mass, armor, shield, fuel/cargo capacity, warp, firepower, cost) are computed on demand.
- A representative starter **catalogue** (`Catalogue`) provides scout/colony/frigate/
  destroyer/freighter hulls and sample engines, armor, shields, weapons, scanner, fuel/
  cargo/colonization modules. Values are clearly-simplified samples `[RECON]`; the full
  component tables are later-phase data.

## 7. Fleets & movement

- Fleets stack identical ships per design (`Fleet`), carry fuel + a cargo hold, and follow
  an ordered list of **waypoints** each with a target and warp.
- **Warp**: distance per year = **warp² ly** `[SOURCE]` (warp 5 = 25 ly, warp 9 = 81 ly).
- **Fuel** scales with mass and warp; an empty tank forces a free **warp-1 crawl** `[RECON]`
  (`Formulas.fuelUsage`) — the real per-engine fuel-vs-warp tables override this later.
- Fleet merge/split and full cargo transfer UI are TODO; the model supports the operations.

## 8. Combat & related systems

- **Combat** (`TurnEngine.resolveCombat`) is `[RECON]` **simplified**: at each location
  where mutually-hostile fleets meet, each side deals its total firepower in one
  simultaneous exchange and ships die as damage exceeds armor+shield. This is **not** the
  original's 10×10 tactical board with initiative, range, accuracy, jammers and battle
  plans — that is a dedicated later phase.
- **Minefields, mass-driver packets, stargates, bombing, and ground invasion** are
  documented here but not yet implemented — TODO.

## 9. Diplomacy & victory conditions

- Player relations are Friend/Neutral/Enemy (`Player.Relation`); only Enemy currently
  drives combat. Treaties/negotiation are not modelled.
- Victory conditions (planets %, tech levels, score threshold, last-race-standing, etc.)
  are not yet implemented — TODO. Scoring is a later phase.

## 10. UI / screen layout

The original's major screens and how this recreation maps them:

| Original screen | This build |
|---|---|
| Galaxy/Starmap view | `GalaxyMapPanel` — `Graphics2D`, pannable (drag), zoomable (wheel), owner-coloured planets + fleet markers, click-to-select. |
| Planet report | `CommandPanel` "Planet Report" — owner, environment, pop, factories/mines/defenses, surface & concentration minerals, habitability. |
| Research screen | `ResearchDialog` — all six fields, level, banked points, cost-to-next; set current field, budget %, next-field policy. |
| Status/summary bar | `MainWindow` status bar — year, player, tech string, planet/fleet counts. |
| Production screen | `ProductionDialog` — edit the selected planet's queue (add/reorder/remove factories, mines, defenses, scanner, ships, auto-build). |
| Ship design screen | `ShipDesignDialog` — pick a hull, fill typed slots from compatible components, live mass/warp/firepower/cost. |
| Fleet composition + cargo transfer | `FleetDialog` — composition/fuel/cargo, add waypoints, load/unload colonists & minerals at an orbited colony. |
| Battle plans / relations | `RelationsDialog` — set Friend/Neutral/Enemy per rival. Score/History screens still TODO. |
| New game wizard | `NewGameDialog` — universe size, race name + PRT, AI opponent count. |

## Known gaps summary (next phases)

Done since the first draft: ship-design & production editors, research/fleet/planet/
relations screens, cargo transfer, the new-game wizard, and a basic expansion AI (Phases
5–7). Remaining:

1. Full component/hull tech tables and field-unlock data.
2. Real tactical combat (10×10 battle board + battle plans) — combat is a deterministic
   attrition exchange today.
3. Minefields, packets, stargates, bombing, ground invasion.
4. Terraforming action and fleet merge/split UI.
5. A stronger, military-aware AI (current `SimpleAi` is expansion-only).
6. Victory conditions & scoring; Score/History screens.
