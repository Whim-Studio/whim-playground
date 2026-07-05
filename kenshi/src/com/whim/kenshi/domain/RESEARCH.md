# Kenshi Demake — Domain Research & Chosen Constants (Task 1)

This document records the mechanics the `com.whim.kenshi.domain` layer models and
the concrete numbers behind them. Where real Kenshi's exact values are unknown or
un-observable, we pick coherent, self-consistent values and note the reasoning.
The engine (Task 2) applies these; the domain layer only stores state and exposes
the derivations the `Views` need.

All tuning constants that cross task boundaries live in `api.Config`; values named
here that are *not* in `Config` are domain-local (documented as such).

## 1. Anatomy — the seven-part limb system

Every character owns independent HP for the seven `Enums.BodyPart`s in canonical
order: **Head, Chest, Stomach, Left Arm, Right Arm, Left Leg, Right Leg**.

- **Maxima:** torso parts (`HEAD/CHEST/STOMACH`) = `Config.TORSO_PART_MAX` (100);
  limbs = `Config.LIMB_PART_MAX` (100). Modelled per-part so future gear/racial
  bonuses can raise a single part's max without touching the others.
- **Range:** a part starts at max, heals up to max, and can fall to `-max` (the
  *floor*, `Config.PART_MIN_FRACTION = -1.0` × max). Kenshi lets limbs go into the
  negative "crippled/destroyed" band before they lock at the floor; we mirror that.
- **Disabled:** `hp <= Config.PART_DISABLED_AT` (0.0). A disabled part is
  unusable but not necessarily destroyed.

### Damage consequences (derivations, `Character.moveState()` / `effectiveWeapon()`)

Priority order (highest first): **DEAD > DOWNED > CRAWLING > MOVING > IDLE**.

| Condition | Result |
|---|---|
| A **vital** part at its `-max` floor | **DEAD** |
| Stomach *disabled* (hp ≤ 0, above floor) **or** blood `< BLOOD_UNCONSCIOUS_AT` | **DOWNED** (unconscious, alive) |
| Both legs disabled | **CRAWLING** (`Config.CRAWL_SPEED_MULT` = 0.28× speed) |
| A live MOVE order | **MOVING** |
| otherwise | **IDLE** |
| An arm disabled | `effectiveWeapon()` downgrades `TWO_HANDED` → `UNARMED` |

**Vital-part / death rule — resolved ambiguity.** The contract prose says "a
vital part (Head *or* Chest) hitting the floor → DEAD", while `Enums.BodyPart`
flags **Head, Chest, and Stomach** all as `vital()`. We honour the API's
`vital()` flag: a character dies when **any** part with `vital()==true` reaches
its `-max` floor. This stays coherent with the DOWNED rule because the two
stomach states are distinct:

- Stomach **disabled** (hp in `(-max, 0]`) → **DOWNED** (gut wound, unconscious).
- Stomach **destroyed** (hp == `-max`) → **DEAD**.

This gives a sensible escalation (a gut wound knocks you out; a destroyed torso
kills you) and means the engine can rely on `Anatomy.isDead()` alone for lethality
without special-casing which torso part landed the killing blow.

`isDowned(bloodLow)` takes the blood test as a parameter because blood lives on
`Character`, keeping `Anatomy` a pure part-HP store. `Character.isDowned()` wires
in `blood < Config.BLOOD_UNCONSCIOUS_AT`.

## 2. Survival — hunger & blood

- **Hunger:** `Config.HUNGER_MAX` = 1000, decays `HUNGER_DECAY_PER_SEC` = 0.30 per
  *world* second. At 0 hunger the engine applies slow starvation damage. The
  domain layer only stores/clamps hunger in `[0, HUNGER_MAX]`.
- **Blood:** `Config.BLOOD_MAX` = 100, regenerates `BLOOD_REGEN_PER_SEC` = 0.05
  when not bleeding, pass-out threshold `BLOOD_UNCONSCIOUS_AT` = 25, death at 0.
- **Bleed:** `Character.bleedRate` (HP-equiv lost per world second), `>= 0`,
  additive via `addBleedRate`. A landed hit adds `Config.BLEED_FROM_DAMAGE` (0.06)
  × damage to the victim's bleed rate (engine-applied). Domain just holds it.

Hungry Bandits are seeded at 15–40% hunger so they read as desperate raiders.

## 3. Skills — 1..100 with an XP curve

`Skills` stores an integer level in `[Config.SKILL_MIN=1, Config.SKILL_MAX=100]`
plus fractional banked XP per `Enums.SkillType`
(`TOUGHNESS, ATHLETICS, STRENGTH, DEXTERITY, MELEE_ATTACK, MELEE_DEFENCE`).

- **XP to advance from level L → L+1:** `XP_BASE + XP_PER_LEVEL × L`
  (domain-local: `XP_BASE = 10`, `XP_PER_LEVEL = 2.5`). Linear-in-level cost →
  cumulative cost is quadratic, so early levels are quick and high levels grind
  (e.g. 1→10 ≈ 213 XP, 1→50 ≈ 3.5k XP, 1→100 ≈ 13.5k XP).
- `addXp` promotes across multiple levels in one call and hard-caps at 100
  (banked XP zeroed at cap). `setLevel` is used at world-build time to seed a
  character straight to a level.

Who trains what (engine decides *when*; recorded here for the seam): acting trains
the action's skill (`MELEE_ATTACK`/`MELEE_DEFENCE`/`DEXTERITY`/`STRENGTH`), taking
damage trains `TOUGHNESS`, moving trains `ATHLETICS`.

Seeded starting bands: player heroes 8–22, town guards 18–40, bandits 10–30,
drifters 3–15.

## 4. Combat (formula lives in the engine; inputs are domain state)

`hitChance = clamp(BASE_HIT_CHANCE + HIT_CHANCE_PER_SKILL ×
(attacker.MELEE_ATTACK − defender.MELEE_DEFENCE), MIN_HIT_CHANCE, MAX_HIT_CHANCE)`
= `clamp(0.55 + 0.03·Δskill, 0.05, 0.95)`. Melee reach `MELEE_RANGE` = 22 world
units. On a hit the engine picks **one** body part by weighted random (torso
weighted above limbs) and applies damage + bleed. The domain layer supplies the
skills, weapon (`effectiveWeapon()`), positions, and the `Anatomy.damage` sink.

## 5. Factions

`FactionMatrix` holds a symmetric pairwise `Enums.Relation`
(`ALLY/NEUTRAL/HOSTILE`) and a player reputation per faction in `[-100, 100]`.
Seeded defaults:

- **Holy Nation ↔ Shek Kingdom:** HOSTILE (the signature feud).
- **Dust Bandits & Hungry Bandits:** HOSTILE to *everyone*, including each other.
- **Trade Guild & Drifters:** NEUTRAL to the settled factions.
- **Player:** NEUTRAL with the settled world, HOSTILE with both bandit factions.
- **Starting reputations:** Trade Guild +10, Holy Nation +5, Shek/Drifters 0,
  Hungry Bandits −30, Dust Bandits −40.

`isHostile(a,b)` is the engine's aggro gate; hostiles attack on sight within the
engine's aggro range.

## 6. Map generation (`MapGrid`)

A `Config.MAP_TILES² = 96×96` grid of `Enums.Terrain`, generated deterministically
from the seed:

- **Layered value noise:** two independent fields, *elevation* and *moisture*,
  each 4 octaves of bilinearly-smoothed (`smoothstep`) value noise starting at a
  4×4 lattice and doubling per octave, amplitude halving per octave, normalised to
  `[0,1]`.
- **Classification** (`elevation e`, `moisture m`): `e < 0.30` → WATER (lakes);
  `e > 0.80` → ROCK highlands; `0.68 < e ≤ 0.80` → ASH if dry else ROCK; mid
  elevations by moisture: `m > 0.66` GREEN belts, `m > 0.42` SCRUB, else SAND
  desert. This yields desert basins with green river-belts, rocky/ash ridges, and
  scattered lakes.
- **Towns** are stamped as 5×5 `TOWN` patches after noise. **WATER is the only
  impassable terrain** (`blocked`), which the engine's pathfinder honours.

## 7. World population (`WorldBuilder.build(long seed)`)

Deterministic placement (same seed → identical world):

- **4 towns** (The Hub / Trade Guild start, Bad Teeth / Holy Nation, Squin / Shek,
  World's End / Trade Guild), each with a co-located BAR node; plus a RUIN and a
  Dust-Bandit CAMP node → 10 nodes total.
- **Player squad "Wanderers":** 3 heroes (Beep one-handed, Ruka two-handed, Shryke
  unarmed) spawned around The Hub, `AiState.IDLE`.
- **Town guards:** 2–3 per settled town, `AiState.PATROL`, one/two-handed.
- **Bandit squads:** 3 squads of 3–5 (Dust/Hungry alternating), `AiState.WANDER`,
  placed on random non-water tiles.
- **Drifters:** 4–6 unarmed wanderers, `AiState.WANDER`.

Total ≈ 30+ characters, 9 squads (seed 42 → 32 characters, verified by
`DomainSelfCheck`).

## 8. What the domain layer deliberately does NOT do

No threading, no Swing, no AI/combat/survival *stepping*, no pathfinding. Those
are Task 2 (engine). Task 1 is pure mutable state + the `moveState()` /
`effectiveWeapon()` / `isDead()` / `isDowned()` derivations that
`Views.CharacterView` exposes, so the engine can build snapshots without
re-deriving physical state.
