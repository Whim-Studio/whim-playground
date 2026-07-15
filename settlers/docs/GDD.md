# The Settlers — Game Design Document

Design reference for the recreation. Seeded from the verified project brief and
kept current as the build proceeds. Numbers marked `// approximate` are not yet
confirmed against a primary source and are for balance-tuning during playtest.

**Primary references** (to consult where live web access is available):
Wikipedia "The Settlers (1993 video game)"; the *Serf City: Life is Feudal* NA
instruction manual (retro-preservation mirrors); Freeserf
(github.com/freeserf/freeserf) as a mechanically faithful engine reference —
note Freeserf ships no art and needs the original data files; we do the opposite
(placeholder/original art only, never depend on the 1993 release).

## 1. Vision & scope

Single-player-vs-AI recreation of the build → produce → transport → expand →
fight → win loop. Depth of the core loop over content count: one well-balanced,
fully-playable map is the bar. Local split-screen and waterway/ship crossings are
stretch goals; networked play is out of scope (it was serial/modem-only in 1993).

## 2. Art & legal policy

Rules and mechanics are not copyrightable and are faithfully reproduced. Blue
Byte's specific expression (sprites, audio, text) is not used. Art is: (a)
placeholder `Graphics2D` primitives — the default; (b) CC0/public-domain assets,
credited in `CREDITS.md`; or (c) original pixel art. Never scrape, decompile, or
embed original assets; never depend on the original's data files.

## 3. Core loop & setup

- Fully mouse-driven, point-and-click. Optional keyboard shortcuts are additive.
- Modes: campaign (missions vs AI of rising difficulty) and free-play (2–4
  players, any human/AI mix, per-AI personality from peaceful→aggressive and
  configurable starting conditions).
- Every game begins by placing a **Castle** (HQ) on a shown patch of buildable
  land to found the settlement.
- **Win/lose:** control the entire map — every rival eliminated or absorbed.

## 4. Settlers

- The player never directly controls a settler. Orders are indirect (place a
  building, lay a road); game logic assigns idle settlers to jobs and paths them.
- Settlers emerge from the Castle as buildings need staff; once its pool runs
  low, a **Warehouse** is needed to keep producing them.
- ~two dozen professions, each doing exactly one job. Model as a role/state enum
  per settler (not a generic worker) — job-specific behavior and, later, job-
  specific appearance are core to the game's identity.

## 5. Roads, flags & transport — the mechanical heart

Implement this correctly before layering the economy on top.

- Goods move **only along roads**. Build: place a flag, then lay road segments
  toward another flag (directional build prompts are a UX nicety worth keeping).
- **Flags are relay hubs, not couriers.** One carrier moves a good from its
  source to the nearest flag and sets it down; a *different* carrier picks it up
  and moves it to the next flag, and so on to the destination. This relay is the
  foundation of the transport layer.
- More flags on a route ⇒ more carriers in parallel ⇒ less congestion. Model real
  road **throughput**, not just point-to-point connectivity.
- When goods pile up at a flag, an adjustable **goods-priority** setting decides
  what moves first.
- **Waterways** bridge small water gaps; a Ship Maker's building and boats handle
  the crossing. Stretch feature, not core-loop critical.

## 6. Economy — production chains (a real dependency graph)

A building idle for lack of a valid input is **correct** behavior — that
supply/demand bottlenecking is the point, not a bug.

- **Wood:** Woodcutter's Hut fells trees → Sawmill: Wood → Planks. Forest
  Ranger's Hut replants trees (renewable supply — do not skip; running out of
  trees is a real failure state).
- **Stone:** Quarryman's Hut gathers Granite from rocky terrain.
- **Food** (three independent lines feeding miners & soldiers):
  1. Farm → Grain → Windmill → Flour; Well → Water; Bakery: Flour + Water → Bread.
  2. Fisherman's Hut → Fish.
  3. Pig Farm (raises pigs on Grain) → Butcher's Shop → Meat.
- **Mining:** four mine types, each built on its matching mountain terrain — Coal,
  Iron, Gold, Granite (stone) mines. Miners must be fed or output stops.
- **Metal:** Iron Foundry: Iron Ore + Coal → Iron. Gold Foundry: Gold Ore (+ Coal)
  → Gold Bars/Coins.
- **Tools & weapons:** Tool Maker's Shop: Iron + Planks → tools (pliers, saw, axe,
  …). A building is only staffed once the specific tool its job needs exists —
  deliberate bottleneck. Blacksmith's/Armory: Iron + Coal → swords and shields.
- **Required UI (real, not backend-only):** goods-distribution priority (what
  share of food/wood/iron/coal/wheat goes to which consuming building) and tool
  priority (which tool the Tool Maker makes next when several are needed).

## 7. Military & territory

- Territory expands only by building a military structure — **Guard Hut → Guard
  Tower → Garrison** (ascending tiers) — near the current border, and only once
  it has ≥1 knight garrisoned.
- **Knights** come from the settler pool (each needs a **sword + shield**), have
  **five ranks**, promote faster if trained in the Castle than on-station, and
  fight harder when **Gold Coins** are delivered to their building (morale).
- Player knobs (match the original): how many settlers become knights; the rank
  of new front-line defenders; how many knights per building are held for offense
  vs defense; and how garrisons are staffed differently across buildings that are
  hidden, visible-but-safe, under threat, or about to be attacked.
- **Attacking:** click an enemy military building, choose how many units to send.
  Beat every defender ⇒ occupy it and grow territory to that building's radius;
  the loser's buildings left outside all remaining friendly radii are lost.
  Defense is automatic (garrisoned knights respond without micromanagement).

## 8. Open questions / to verify

- **Prospecting:** does *this* game (not its sequel) require a prospecting step to
  locate mineral deposits, or is a mountain tile's yield simply visible from its
  terrain? **To confirm from a primary source before building the mining UX.**
  Working assumption until confirmed: mountain terrain type indicates the mineral
  (see `map/TerrainType` MOUNTAIN_* variants) `// approximate — verify`.
- Exact settler professions count (~24) and the precise building roster names —
  cross-check against the manual in Phase 2.
- The original's theoretical 65,536-settler cap is impractical in Swing; a lower
  practical cap will be chosen and documented when the economy scales up.

## 9. Architecture notes

- Packages: `engine` (loop, camera, input, world, render), `map`, `buildings`,
  `economy`, `transport`, `military`, `ai`, `ui`, `io`.
- Fixed-timestep simulation (60 Hz) decoupled from active `BufferStrategy`
  rendering, so behavior is frame-rate independent.
- Rendering is currently top-down square tiles; isometric projection (the
  original's look) is a `Camera`/`Renderer`-local change deferred to a later
  phase — it does not touch the simulation model.
