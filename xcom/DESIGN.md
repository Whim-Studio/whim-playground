# UFO: Enemy Unknown — Design Document (clean-room)

A design reference for a clean-room Java 8 / Swing recreation of **UFO: Enemy
Unknown** (a.k.a. *X-COM: UFO Defense*, MicroProse / Mythos Games, 1994).

> **Clean-room.** Every rule and number below is reconstructed from **public
> documentation** — primarily the community wiki **UFOpaedia**
> (`https://www.ufopaedia.org`) and the open-source re-implementation
> **OpenXcom** (which encodes the original tables). **No original game code,
> assets or data files are used.** All in-game art is drawn procedurally with
> Java2D. Where a value is uncertain it is **flagged** with the assumption taken.

This document is the spec Phases 1+ implement against. Phase 0 (this deliverable)
builds only the architecture that will host these systems — see the "Phase 0 vs
deferred" section of `README.md`. Formulas marked **[IMPLEMENTED]** already return
real values in the Phase 0 ruleset with passing unit tests.

Sources are cited inline as `[UFOpaedia:<page>]` / `[OpenXcom]`. Two anchor
references: UFOpaedia home, and the OpenXcom vanilla ruleset.

---

## 1. Geoscape (strategic layer)

### 1.1 Time controls
Six speeds, matching the original toolbar: **5 sec, 1 min, 5 min, 30 min, 1 hour,
1 day**. The simulation advances in discrete ticks; the smallest UFO-detection and
craft-movement resolution is the **30-minute** step (the game internally steps in
5-second units but batches to 30 min for many checks). Time auto-drops to a slower
speed on any event (UFO detected, craft arrives, research done). `[UFOpaedia:Time]`

### 1.2 Funding nations & monthly score
- **16 funding countries**, each with a monthly funding value and a hidden
  "satisfaction" affected by X-COM's activity in their region vs alien activity.
- Each month the **Council of Funding Nations** reviews performance. A country's
  funding change is driven by the **monthly score** (X-COM score − alien score) in
  its region; happy countries **increase** funding (up to roughly +double or capped
  per-country), unhappy ones **cut** funding or **sign a secret pact** with the
  aliens and stop funding entirely. `[UFOpaedia:Funding]`
- **Score** accrues from: shooting down/recovering UFOs, winning battlescape
  missions, researching, and killing/capturing aliens (each alien has a
  `scoreValue`); negative score from civilian deaths, soldier deaths, lost craft,
  and letting UFOs complete missions. `[UFOpaedia:Score]`
- **Loss condition:** if net monthly performance is bad enough for two straight
  months (funding collapses / the Council terminates the project), the game ends.

### 1.3 UFO detection & behaviour
- Radar facilities roll a **detection chance per 30-min tick** while a UFO is in
  range: Small Radar ≈ **10%**, Large Radar ≈ **20%**, Hyperwave Decoder = **100%**
  (and reveals mission/race). Ranges are ~**450** nmi for the ground radars.
  `[UFOpaedia:Radar Systems]`
- Once detected, a UFO may be tracked until it lands or leaves radar. UFOs fly
  **alien missions** (Research, Harvest, Abduction, Infiltration, Base, Terror,
  Retaliation, Supply) as scripted waves; each mission is a sequence of UFO
  appearances of escalating size. `[UFOpaedia:Alien Missions]`
- UFO types (Small Scout → Battleship) differ in hull, speed, weapon power and
  crew — see `UfoDef` and `data/rules1994.json`.

### 1.4 Interception / air combat
- An interceptor closes to a weapon's range band (**cautious / standard /
  aggressive / disengage** stances set closing distance). Each craft weapon has a
  **hit chance**, **damage per hit**, **ammo** and **reload time**; hits subtract
  from UFO hull points. `[UFOpaedia:Interception]`
- Example weapons: Cannon (low dmg, short range), Avalanche missiles (high dmg,
  long range), Laser/Plasma/Fusion cannons post-research. A downed UFO becomes a
  **crash site** (reduced crew, salvage) or, if landed and assaulted intact, a
  **landing site** (full crew, more salvage).

### 1.5 Base facilities
6×6 grid, facilities connected to the **Access Lift**. Key facilities and their
1994 build cost / build days / monthly upkeep (see `data/rules1994.json`):

| Facility | Cost | Days | Upkeep | Provides |
|---|---:|---:|---:|---|
| Access Lift | $300k | 1 | $4k | base entry |
| Living Quarters | $400k | 16 | $10k | 50 personnel |
| Laboratory | $750k | 26 | $30k | 50 scientist slots |
| Workshop | $800k | 32 | $35k | 50 engineer slots |
| Small Radar | $500k | 12 | $10k | 10%/tick detect |
| Large Radar | $800k | 25 | $15k | 20%/tick detect |
| Hangar (2×2) | $200k | 25 | $25k | 1 craft |
| General Stores | $150k | 10 | $5k | 50 storage |

### 1.6 Research & manufacturing
- **Research**: assign scientists; progress = scientist-days against a project's
  cost. Projects gate on prerequisites (captured live aliens, recovered items,
  prior research) and **unlock** items, manufacturing and further research.
  `[UFOpaedia:Research]`
- **Manufacturing**: assign engineers in a Workshop; each project needs a required
  research, **engineer-hours**, an up-front **$ cost**, optional **input items**
  (e.g. Alien Alloys, Elerium) and consumes **workshop space** while queued. Output
  is stored/sold. `[UFOpaedia:Manufacturing]`
- Modelled by `ResearchNode` / `ManufactureNode`; a small tech tree ships in the
  data pack.

### 1.7 Council report
Delivered monthly: per-country funding changes, total funding, the graph of X-COM
vs alien activity, and a rating (Terrible … Excellent). Drives the loss condition.

---

## 2. Battlescape (tactical layer)

### 2.1 Time Units (TU) & action costs **[IMPLEMENTED: `TimeUnitModel`]**
Each unit has a TU pool (soldiers ~50–80). Costs `[UFOpaedia:Time Units]`,
`[OpenXcom]`:
- **Walk, orthogonal:** the tile's terrain move cost — **4** on standard ground.
- **Walk, diagonal:** ×1.5 → **6**.
- **Up a level / rough terrain:** higher per-tile cost (terrain-dependent).
- **Turn:** **1 TU per 45°** facing step. *(Assumption: vanilla turning is cheap;
  we use 1/step. Flagged.)*
- **Kneel down: 4 TU; stand up: 8 TU.** *(Flagged — sources vary between 3–4 to
  kneel and 8–10 to stand; we chose 4 / 8.)*
- **Fire:** `round(maxTU × weapon.tuPercent(mode) / 100)`. Percentages are
  per-weapon per-mode. Rifle: Snap **25%**, Auto **35%**, Aimed **80%**.

### 2.2 Fire modes & the accuracy formula **[IMPLEMENTED: `AccuracyModel`]**
Modes: **Snap / Aimed / Auto** (auto fires a 3-shot burst). Each weapon lists an
accuracy multiplier per mode. The core to-hit formula `[UFOpaedia:Firing
Accuracy]`:

```
finalAccuracy% = firingAccuracy(soldier) × weaponAccuracy(mode) / 100
               × (kneeling     ? 1.15 : 1.0)     // kneel bonus
               × (oneHanded2H  ? 0.80 : 1.0)     // two-handed weapon in one hand
               × (1 − 0.10 × fatalWoundsToFiringArm)
               × (targetInSmoke ? 0.5 : 1.0)      // flagged approximation
```

Worked example: Firing Accuracy 60, Rifle **Aimed** (110%) → 60 × 110/100 = **66%**.
Kneeling → 66 × 1.15 = 75.9 → **75%**. The rolled trajectory then determines the
actual impact tile; a "miss" can still hit something along the line of fire.

Representative 1994 mode table (in `data/rules1994.json`):

| Weapon | Snap acc/TU | Aimed acc/TU | Auto acc/TU×shots | Power/Type |
|---|---|---|---|---|
| Pistol | 60 / 18% | 78 / 30% | — | 26 AP |
| Rifle | 60 / 25% | 110 / 80% | 35 / 35% ×3 | 30 AP |
| Heavy Cannon | 60 / 33% | 90 / 80% | — | 56 AP |
| Laser Rifle | 65 / 25% | 100 / 50% | 46 / 34% ×3 | 60 Laser |
| Heavy Plasma | 75 / 30% | 110 / 60% | 55 / 35% ×3 | 115 Plasma |

### 2.3 Reaction fire **[IMPLEMENTED: `ReactionModel`]**
A watching unit may interrupt a moving enemy. `[UFOpaedia:Reactions]`:

```
reactionScore = reactions × currentTU / maxTU
```

The reactor interrupts when its score **>** the mover's score at the instant the
mover spends TU. The reactor then takes a **snap shot** if it has the TU. Because
score scales with remaining TU, a unit that has spent most of its TU is unlikely to
react.

### 2.4 Damage, armour, stun & capture **[IMPLEMENTED: `DamageModel`]**
`[UFOpaedia:Damage]`:

```
rolled     = power × random(0..200)% / 100      // uniform, mean = power
afterArmor = rolled − armor(struckFacing)
damage     = max(0, afterArmor × armorResistance(damageType))
```

- Armour has four facings (**front/side/rear/under**); the struck facing is chosen
  by the shot geometry. Armour is also **degraded** by fire/acid over time
  (deferred).
- **Stun** damage accrues to a stun pool; when **stun ≥ health** the unit falls
  **unconscious** (alive). **Capturing a live alien** (via Stun Rod or Small
  Launcher, then ending the mission with it unconscious and X-COM in control) is
  required to research many techs. `[UFOpaedia:Stun]`
- **Explosives/grenades:** blast power falls off with distance from the epicentre
  (roughly linear, ~−(power/…) per tile); High Explosive, grenades, proximity
  grenades, and the Blaster Launcher (guided). Explosions also destroy terrain.

### 2.5 Morale & panic `[UFOpaedia:Morale]`
- Units start at **100 morale**. Losses (friendly deaths, taking damage, low squad
  strength) reduce it. At the end of a unit's side's turn, if **morale < 50** there
  is a **panic check**: roll vs `(morale × 2)`% to stay in control; on failure the
  unit **panics** (freezes / drops items / fires wildly) or goes **berserk**.
  *(Flagged: exact panic roll uses bravery; we adopt the documented
  `morale-based` check and refine in implementation.)*

### 2.6 Day/night, LOS & fog `[UFOpaedia:Day and Night]`
- Missions are **day or night** (affects visual range: ~**20 tiles day**, ~**9
  tiles night** unless lit by flares/incendiaries). LOS is tile-based; unseen tiles
  are **fogged** and remember last-seen state.
- **Line of fire** is a separate voxel/line trace from LOS; smoke reduces both
  sight and accuracy.

### 2.7 Terrain destruction
Tiles/objects have HP and are destroyed by explosives and heavy fire, opening walls
and floors (units fall through destroyed floors). Fire and smoke spread and persist
across turns. (Deferred to the Battlescape phase; `DamageType` already enumerates
`HIGH_EXPLOSIVE`, `INCENDIARY`, `SMOKE`.)

### 2.8 Inventory & weight `[UFOpaedia:Inventory]`
Slots: **two hands, belt, shoulders (2), backpack, ground**. Every item has a
**weight**; total carried weight above the soldier's **Strength** reduces available
TU (encumbrance). Reloading, priming grenades and drawing from the backpack all
cost TU.

---

## 3. Meta loop (campaign)

### 3.1 Soldier stats & progression `[UFOpaedia:Experience]`
Stats: **Time Units, Stamina, Health, Bravery, Reactions, Firing Accuracy,
Throwing Accuracy, Strength**, plus **Psi Strength/Skill** (hidden until tested).
- **Per-stat improvement** after a mission is driven by **actions taken** with that
  stat: e.g. firing (and hitting) improves Firing Accuracy, taking hits/effort
  improves Health/Stamina, panicking-and-recovering builds Bravery. Each stat has a
  documented **hidden experience counter → random increase** within a per-stat cap.
  *(Flagged: exact increase brackets vary by stat; the ruleset will encode the
  UFOpaedia experience tables. Phase 0 leaves these as data.)*
- Enough experience promotes soldiers through **ranks** (Rookie → Commander), which
  grant squad-wide morale bonuses.

### 3.2 Item recovery
After a won mission, surviving items on the map, UFO components (**Alien Alloys,
Elerium-115, UFO Power Source/Navigation**), corpses and **live captives** are
recovered to base stores — feeding research and manufacturing.

### 3.3 Research → Manufacture → Deploy cycle
Recover/capture → **research** unlocks tech → **manufacture** builds equipment/craft
→ **deploy** on interception and battlescape → recover better tech. This loop is the
spine of the campaign and is why the ruleset exposes research/manufacture as data.

### 3.4 Difficulty tiers
Five: **Beginner, Experienced, Veteran, Genius, Superhuman** (`Difficulty` enum).
Higher tiers scale alien **stats, counts and aggression** and reduce the score
needed for the aliens; the original also had the well-known "difficulty bug" where
some values reset — we **do not** reproduce the bug (flagged deviation).

### 3.5 Win / lose
- **Win:** research the path to **Cydonia or Bust!**, then win the two-stage
  **Cydonia** assault (Martian surface + alien base, killing the **Alien Brain**).
- **Lose:** the **Council terminates** X-COM after sustained poor monthly
  performance / **funding collapse**, or all bases are lost.

---

## 4. Flagged assumptions (summary)

| Rule | Documented range | Choice taken |
|---|---|---|
| Kneel / stand TU | kneel 3–4, stand 8–10 | **4 / 8** |
| Turning TU | small; "free-ish" in some notes | **1 TU / 45°** |
| Firing-arm fatal wound penalty | ~−10% each (reconstructed) | **−10% per wound** |
| Smoke accuracy penalty | reduces LOF accuracy | **×0.5 approximation** |
| Panic check | bravery/morale based | **morale×2% to hold** (refine later) |
| Difficulty bug | present in original | **not reproduced** |
| Terror-ignore penalty | large negative score + funding fallout | **−150 score** on expiry (Phase 8) |
| Terror frequency | scripted alien-mission waves | **~14→6 game-days by difficulty**, 24–48h to respond |
| Terror battlescape | civilians on a distinct third side | **no civilian side** — a tougher night crew + a real geo-level ignore penalty (civilians deferred) |
| UFOpaedia unlocking | per-item research/autopsy gates | **forgiving gates** (basic gear always; tech on its research; aliens on autopsy/interrogation or *Alien Origins*) |

These live behind the ruleset interfaces, so tuning any of them is a data/strategy
change, never an engine change.

---

## 5. Sources
- **UFOpaedia** — community reference wiki: `https://www.ufopaedia.org/`
  (pages: Firing Accuracy, Time Units, Reactions, Damage, Stun, Morale, Radar
  Systems, Funding, Score, Alien Missions, Research, Manufacturing, Inventory,
  Experience, Day and Night).
- **OpenXcom** — open-source clean re-implementation whose rulesets encode the
  original tables: `https://openxcom.org/` / `https://github.com/OpenXcom/OpenXcom`.

No original MicroProse/Mythos assets, code, or data are used or redistributed.
