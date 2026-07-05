# Expedition outcome model

`ExpeditionEngine.resolve(fleet, expeditionBonus, seededRandom, catalog)` picks one outcome
from a weighted table, then scales the magnitude by the fleet and the owner's
`ClassDef.expeditionBonus` (Explorer = 2.0, others = 1.0). All randomness comes from the
caller-supplied seeded `Random` (derived from the game's master seed), so a save replays
identically.

## Size tier

`tier` is derived from the number of ships escorting the expedition:

| ships      | tier |
|------------|------|
| 1–14       | 0    |
| 15–59      | 1    |
| 60–199     | 2    |
| 200+       | 3    |

Bigger fleets shift the odds toward good outcomes and away from pirates/black-hole, and
raise the caps on how much can be found.

## Weighted table

Weights below are `base (+ per-tier adjustment)`. A single `nextInt(totalWeight)` selects
the bucket by cumulative weight, in this order:

| Outcome       | Weight (tier-adjusted)            | Effect |
|---------------|-----------------------------------|--------|
| **Resources** | `30 + 4·tier`                     | Find metal/crystal/deut split 50/30/20, sized `min(cargoCap, cargoCap · (0.15..0.60) · expeditionBonus)`, min 500. |
| **Dark Matter** | `12 + tier`                     | `(50..500) · expeditionBonus` dark matter added to the empire. |
| **Fleet found** | `8 + 2·tier`                    | Gain `1 + min(20, tier·3 + 0..3)` Light Fighters or Small Cargo. |
| **Nothing**   | `25`                              | No effect; fleet returns normally. |
| **Delay**     | `10`                              | Fleet return delayed by `1..2` extra ticks (hours). |
| **Pirates**   | `max(3, 10 − 2·tier)`             | Small ambush; lose `3%..15%` of the escort (cheapest ships first). Sometimes repelled with no loss. |
| **Black hole**| `max(1, 2 − [tier≥2])`            | Rare total loss — the entire fleet is destroyed. |

## Notes

- Find magnitudes are capped by the fleet's total cargo capacity so a tiny scout cannot
  return a fortune; a large hauler expedition can.
- Pirate losses target the cheapest ships first (fighters/cargo) so capital escorts are
  more survivable, matching the intent that a strong escort protects the expedition.
- The black-hole weight is deliberately tiny and shrinks further for large fleets; it is
  the only "catastrophic" branch and exists to keep expeditions risky.
- These weights are an OG-Galaxy-flavoured approximation (OGame's exact expedition table is
  server-configurable and not publicly fixed); they are the single source of truth for this
  build and are easy to retune here without touching the engine.
