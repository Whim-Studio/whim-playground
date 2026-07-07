# Babylon 5 Wars — Rules & Design Reference

**Game:** *Babylon 5 Wars* — tactical starship-combat miniatures wargame
**Publisher:** Agents of Gaming (AOG). 1st edition **1997**, 2nd edition **1999**.
**Designers:** Bruce Graw, Robert Glass.

> **Scope of this document.** This is the Phase 1 research reference that will drive the
> Java 8 / Swing implementation. It describes the mechanics we intend to model.

---

## ⚠️ Source-availability disclaimer (read first)

The original rulebook and the community rules archives could **not** be retrieved in this
environment: the demo proxy blocks web search, BoardGameGeek returns HTTP 403, and the
official AOG rules PDFs are not accessible here. The only source that resolved was the
Wikipedia article on the game.

Consequently this document separates two confidence tiers, and **every mechanic is tagged**:

- **[CONFIRMED]** — corroborated by the retrievable source (Wikipedia) or unambiguous.
- **[APPROXIMATED]** — reconstructed from general knowledge of the AOG system and genre
  convention. Treat all specific numbers, die types, tables, and per-ship stats in this
  tier as **unverified against the original rulebook** and subject to correction.

The implementation is deliberately **data-driven** (see `ARCHITECTURE.md` in Phase 2) so
that every [APPROXIMATED] number lives in an external JSON file and can be corrected
against the real rulebook without touching engine code. Code comments will carry the same
`APPROXIMATED, unverified` marker at each such value.

### Design decisions (resolved with user)
1. **Map = HEX GRID.** ✅ Decided. Ships occupy hexes with facing along hexsides; the
   "vector" feel comes from the impulse-movement / thrust system, not free rulers. Matches
   the real AOG game and simplifies faithful movement, facing, and firing arcs. The play
   area may be *rendered* to look like open space, but the model is hex-based.
2. **Defense = ARMOR by default, SHIELD by flag.** ✅ Decided. A per-facing defense layer
   defaults to **non-regenerating armor** (faithful to B5 Wars); a data flag
   (`defenseType = SHIELD`) makes it a regenerating layer for the rare hulls that warrant it.
3. **Die type for to-hit = d20.** [APPROXIMATED] AOG games of this era typically use a
   **d20**; used as the default and trivially swappable in data.

---

## 1. Turn sequence & impulse / initiative structure

### 1.1 Turn skeleton [CONFIRMED at the high level, APPROXIMATED in detail]
Wikipedia confirms: play is **turn-based with simultaneous interaction**; a **die roll with
bonuses determines movement order (initiative)**; and each turn resolves **power usage,
electronic warfare, thrust, drift, fighter operations, and fire**.

Reconstructed step order for one full **Turn** ([APPROXIMATED]):

1. **Initiative** — each side rolls (d6 + initiative bonus). Winner chooses to move
   first or second (moving *second* is usually the reactive advantage).
2. **Power allocation** — each ship distributes reactor power to engines/thrust,
   weapons, EW, reload, and defensive systems for the turn (see §4).
3. **Electronic Warfare allocation** — assign offensive/defensive EW points (see §3.3).
4. **Impulse-movement phase** — the turn is subdivided into **impulses**; ships move and
   fire interleaved across impulses (see §1.2).
5. **Fire & damage resolution** — declared/opportunity fire resolved in the impulse it
   occurs (see §3).
6. **End of turn** — heat/reload bookkeeping, crit checks, remove destroyed units, drift
   for powerless ships.

### 1.2 Impulse movement [APPROXIMATED — core concept believed correct, exact chart unverified]
The signature AOG mechanic: a **Turn is divided into a fixed number of impulses** and a
ship of Speed *S* moves one hex on a **subset of impulses** dictated by a **movement chart**,
so that fast and slow ships advance in a realistic interleaved cadence rather than all at
once. A Speed-8 ship moves on more impulses than a Speed-2 ship; over a full turn each ship
covers exactly *S* hexes.

- We will implement a **configurable impulse count** (default **8**; unverified) and a
  data-defined distribution table mapping `speed → list of impulses on which a hex is
  entered`. Firing is resolved per impulse, so relative positions during movement matter.
- **Uncertain:** the true number of impulses per turn and the exact per-speed distribution.
  Marked `APPROXIMATED` in data and code.

---

## 2. Movement system

All values below are the *model* we implement; per-ship numbers are [APPROXIMATED] and live
in ship JSON.

- **Speed** — hexes entered per turn (integer). Changed only by thrust.
- **Facing** — one of 6 hexside directions on a hex grid.
- **Thrust** — points produced by engines each turn (reduced by power shortfall / engine
  damage). Spent on:
  - **Acceleration / deceleration** — change Speed by 1 per *N* thrust (N scales with ship
    mass; [APPROXIMATED]).
  - **Turning** — change facing by one hexside, subject to **turn mode**.
  - **Pivot** — rotate facing without translating, at a thrust premium (used for pointing
    weapons; a pivoted ship still drifts along its prior vector). [APPROXIMATED]
  - **Sideslip** — translate one hex sideways without changing facing, at a thrust cost.
    [APPROXIMATED]
- **Turn mode / turn cost** — minimum hexes a ship must travel straight between successive
  facing changes; heavier/less agile hulls have a higher turn mode. [APPROXIMATED]
- **Drift** — a ship with no available thrust (powerless/crippled) continues along its
  current vector at current Speed and cannot turn. [CONFIRMED that "drift" is a turn step.]

---

## 3. Combat resolution

### 3.1 Weapons [APPROXIMATED for specifics]
Each weapon has: **type**, **firing arc(s)**, **range brackets**, **base to-hit**,
**damage profile (dice)**, **rate of fire / reload**, and **special traits**
(e.g. armor-piercing, raking, interceptor/point-defense, ballistic/guided).
Genre-representative types by race (names paraphrased, stats [APPROXIMATED]):

- **Earth Alliance:** particle beams/cannons, plasma cannons, rail guns, interceptors
  (point defense), missiles.
- **Minbari:** neutron lasers, fusion cannons, molecular pulsars — plus strong stealth/jamming.
- **Narn:** heavy laser cannons, ion torpedoes, energy mines.
- **Centauri:** battle lasers, matter cannons, twin arrays.

### 3.2 Firing arcs [APPROXIMATED]
Arcs are defined relative to ship facing on the hex grid — e.g. **Forward**, **Aft**,
**Port**, **Starboard**, plus wide **Turret** arcs and boresight-only mounts. A target
must lie within a weapon's arc (and range) to be fired on.

### 3.3 To-hit resolution [APPROXIMATED — d20 default]
Roll **d20**; hit if the roll meets the weapon's modified to-hit target. Modifiers:

- **Range** — accuracy degrades per range bracket beyond point-blank.
- **Target speed** — faster targets are harder to hit.
- **Target size** — larger hulls easier, fighters harder.
- **Electronic Warfare** — *offensive EW* (attacker) improves the shot / strips defense;
  *defensive EW* (target) worsens it; net = offensive − defensive after ECCM. 
- **Arc / off-boresight, fire-control quality, crew quality** — secondary modifiers.

### 3.4 Damage: armor → structure → systems [APPROXIMATED]
1. Determine the **facing hit** (from firing geometry) → selects the target's defense layer
   and the systems column exposed.
2. Roll weapon **damage**. Subtract the facing's **armor** from each hit; remainder
   penetrates. (For rare shield-bearing hulls, the layer regenerates between turns; default
   armor does not.)
3. Penetrating damage marks off **structure boxes** and destroys **systems** in the exposed
   column of the Ship Control Sheet damage diagram.
4. **Special traits** modify this: armor-piercing reduces armor effect; raking spreads
   damage; interceptors/point-defense reduce incoming ballistic/fighter hits before armor.

### 3.5 Critical hits & system damage [APPROXIMATED]
When structure is depleted (or a weapon's crit trait triggers), roll on a **critical/system
table**. Representative results: **reactor hit** (lose power → less thrust/fewer weapons),
**engine/thruster hit** (lose thrust / maneuver), **weapon destroyed**, **sensor/fire-control
hit** (worse to-hit), **crew casualties**, **jump-engine hit**. A destroyed reactor or hull
depletion destroys the ship.

---

## 4. Power allocation & system-damage effects [APPROXIMATED]
Reactors produce **power** each turn, allocated in the Power step to: **engines** (sets
available thrust), **weapons** (some need power/charging to fire or reload), **EW**,
**defensive systems**, and **reload**. Reactor damage lowers the pool, forcing trade-offs
(move vs. shoot vs. defend). Insufficient power → reduced thrust (risk of drift) and/or
weapons offline. This is a *core* tension of the game and will be modeled, even if exact
per-system costs are tuned later.

---

## 5. Ship data structure (fields the model needs)
Per-ship record (all numeric values [APPROXIMATED], sourced from JSON):

| Field | Meaning |
|---|---|
| `id`, `name`, `class`, `race` | identity |
| `points` | fleet-build cost |
| `maxSpeed`, `turnMode` | movement limits |
| `thrust` | engine thrust per turn |
| `power` | reactor output |
| `initiativeBonus` | added to initiative roll |
| `crewQuality` | to-hit / control modifier |
| `sensorRating`, `ewRating` | fire control & EW capacity |
| `armor` per facing (fore/aft/port/stbd[/primary]) | defense layer |
| `structure` per section | hull boxes |
| `defenseType` (`ARMOR`\|`SHIELD`) | regenerating or not |
| `weapons[]` | each: type, arcs, range brackets, toHit, damage, reload, traits |
| `specials[]` | interceptors, jump engine, hangar/fighters, jammers |

Ship Control Sheet (SCS) presentation mirrors the tabletop: stat header, a **damage-box
diagram** by section/facing, a **weapons table** with arcs/ranges, and **power/EW logs**.

---

## 6. First scenario to implement — "Border Skirmish: 1-on-1 duel"
A single-ship duel is the cleanest MVP and exercises movement, EW, power, firing, arcs,
armor, structure, and criticals end-to-end.

- **Proposed matchup [APPROXIMATED stats]:** **EA Hyperion-class heavy cruiser** vs.
  **Narn G'Quan-class heavy cruiser** — two iconic, roughly comparable heavy cruisers.
- **Setup:** opposite map edges, facing inward, Speed ~6.
- **Victory:** destroy or cripple (reactor + all weapons disabled) the enemy ship; or after
  N turns the ship with more surviving structure wins.
- Hot-seat 2-player is the MVP interaction model; a simple AI is a stretch goal.

*(Rationale: a duel avoids fighter-flight and multi-ship initiative complexity while still
demonstrating every core subsystem. Exact stats will be tuned/corrected against the
rulebook when available.)*

---

## 7. Terminology & UI conventions to mirror
- **Ship Control Sheet (SCS)** — per-ship record: stat header, damage-box diagram, weapons
  table, power & EW logs.
- **Impulse chart** — the per-speed movement cadence within a turn.
- **Thrust log / power allocation** — turn bookkeeping the player fills in.
- **Arcs** — named/lettered firing arcs relative to facing.
- **Fire control / EW** — to-hit and defensive electronic warfare.
- The play area shows ships as **facing-oriented markers on a hex field** with movement
  vectors and arc overlays.

---

## 8. Sources
- **[CONFIRMED]** [Babylon 5 Wars — Wikipedia](https://en.wikipedia.org/wiki/Babylon_5_Wars)
  — publisher, year, designers, factions, and the high-level turn structure (initiative
  roll, power/EW/thrust/drift/fighter steps, simultaneous interaction, points-based
  single-race fleet building).
- **Not retrievable in this environment** (blocked/403): BoardGameGeek entry, official AOG
  rulebook PDFs, and community rules archives. All mechanics tagged **[APPROXIMATED]** above
  await verification against those sources.
