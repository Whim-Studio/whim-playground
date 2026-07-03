# Populous (1989) — Domain Mechanics (Task 1)

This note records the researched mechanics of Bullfrog's *Populous* (1989) and the
concrete numbers the `com.whim.populous.domain` package encodes. Everything here is
pure state + rules — no AI, loop, or rendering.

## Landscape & terraforming

Populous land is a heightfield of discrete levels between a fixed water table and
bare peaks. You reshape it by raising/lowering a column of land; the engine smooths
neighbours so you can never make a vertical cliff. Land is only useful once it is
**flat and above the water table** — flat plateaus are where your people build.

`TerrainRules` is the single source of truth for elevation → `TerrainType`, keyed to
the live `seaLevel` so the FLOOD power (which raises sea level) instantly re-derives
every tile:

| height above sea | terrain | notes |
|---|---|---|
| `< -1` | WATER | deep sea, drowns walkers |
| `-1` | SHALLOW | surf, still drowns |
| `0` | SAND | coast/beach, buildable but poor |
| `+1..+2` | GRASS | prime flat building land |
| `+3..+4` | HILL | |
| `+5..+6` | MOUNTAIN | |
| `+7+` | ROCK | bare peaks |

SWAMP and LAVA are **transient overrides** stamped onto a `Tile` by the Swamp and
Volcano powers (with a tick TTL), not derived from elevation.

`MapGrid` terraforming (`raise`/`lower`/`raiseBrush`/`lowerBrush`) nudges a tile by
one step then enforces "no adjacent tile differs by more than one step", which is the
mechanism that turns repeated edits into flat, buildable plateaus. `flatAreaAt(col,row)`
flood-fills the contiguous run of equal-elevation, dry, non-transient tiles — this is
the number the settlement tiers read.

## Settlements (flat-area → tier)

In Populous a dwelling's size is set by how much flat land surrounds it: a lone flat
square yields a tent; a broad flattened field yields a walled castle that breeds many
people and radiates far more manna. `SettlementRules` encodes the CONTRACT table:

| flat tiles | tier | rel. pop | rel. mana |
|---|---|---|---|
| 1 | TENT | 1 | 1 |
| 2–3 | HUT | 2 | 2 |
| 4–8 | HOUSE | 4 | 4 |
| 9–15 | TOWER | 7 | 7 |
| 16+ | CASTLE | 12 | 12 |

Mana weight equals population weight 1:1 — more worshippers, more belief.

## Mana & population

- **Mana per tick** = sum of a side's settlement mana weights (`GameStateManager.accrueMana`).
  Bigger empires charge the mana bar faster. Clamped to `DEFAULT_MAX_MANA = 6000`,
  sized above the costliest power (ARMAGEDDON, 5000).
- **Population** (HUD + victory) = live count of a side's walkers (`recomputePopulations`).
  Per-side soft cap `DEFAULT_POP_CAP = 250`.

Power costs come from the `api` `GodPower` enum: PAPAL_MAGNET 200, SWAMP 400,
EARTHQUAKE 800, VOLCANO 1500, FLOOD 3000, ARMAGEDDON 5000; RAISE/LOWER are free
terraform trickles.

## Divine powers — intended domain behaviour

The engine (Task 2) drives these; the domain exposes the state each one touches:

- **Papal Magnet** — `PapalMagnet.placeAt` sets a per-side rally point followers converge on.
- **Earthquake** — lower a jagged fault (`MapGrid.lower` along a line); `Tile.clearSettlement`
  topples dwellings that fall below buildable level.
- **Swamp** — `Tile.setTransient(SWAMP, ttl)` on target tiles; walkers ending a tick there drown.
- **Volcano** — raise a tall cone (`raiseBrush` up) and stamp LAVA overrides around it.
- **Flood** — global: `MapGrid.raiseSeaLevel(n)` for a while; low walkers drown, then restore.
- **Armageddon** — global: engine summons all walkers to a central battlefield for a final fight.

## Starting island

`GameStateManager.newGame(seed)` builds a seeded 64×64 island: a radial dome that
drops below sea level at the edges (guaranteeing a surrounded landmass) plus random
Gaussian hills/hollows. Each side gets a flattened 5×5 grass plateau at mid-latitude
(GOOD west, EVIL east) with a starter HOUSE and `START_FOLLOWERS = 6` walkers.

`DomainSelfCheck` asserts all of the above (dims, terrain derivation, tiering, flat-area
measurement, mana math, flood re-derivation) with no GUI.
