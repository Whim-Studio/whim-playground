// =============================================================================
// engine/ScoringEngine.js — Rulebook §6 scoring, implemented in the EXACT order.
//
// Order of operations (§6 / appendix), do not reorder:
//   1. base = Σ meld points + Σ flower/season points
//   2. add Mahjong bonus (floor(1% of pointsLimit); 0 if unlimited)
//   3. apply doubles multiplicatively: subtotal *= 2 ** totalDoubles
//   4. if result > pointsLimit -> cap at pointsLimit (treat as Limit Hand)
//   5. SPECIAL LIMIT HANDS (13 Orphans, All Flowers & Seasons): skip step 3,
//      score directly as the Limit Hand (still receives the Mahjong bonus per §5).
//   6. round down to nearest whole point (values are integers throughout).
//
// This module is PURE: it takes a fully-decided WinContext and returns a
// breakdown. It does not know about tiles-in-hand mechanics or money.
// =============================================================================

import { BASE_POINTS, DOUBLES, FLOWER_SEASON_POINTS, FIRST_TILE_FLAT_POINTS }
  from '../domain/constants.js';
import { isHonor } from '../domain/tiles.js';

/** floor(1% of limit); unlimited (limit == null) => 0. */
export function mahjongBonus(pointsLimit) {
  return pointsLimit == null ? 0 : Math.floor(pointsLimit * 0.01);
}

/**
 * @typedef {Object} WinContext
 * @property {Meld[]} melds            decomposed 4 sets + pair (pair as MeldType.PAIR)
 * @property {number} flowerCount      flowers + seasons revealed
 * @property {?number} pointsLimit     null => unlimited
 * @property {string[]} doubleFlags    keys into DOUBLES that apply (decided upstream)
 * @property {?string} specialLimitHand '13_orphans' | 'all_flowers_seasons' | null
 * @property {?string} firstTileWinType 'heavenly'|'earthly'|'human' | null
 */

/** Sum base meld points honoring exposed/concealed and simple/honor (§6 table). */
export function sumBasePoints(melds) {
  let total = 0;
  for (const m of melds) total += meldPoints(m);
  return total;
}

// Exposed here for unit tests; classification of honor vs simple is by tiles.
export function meldPoints(meld) {
  const isHonorMeld = isHonorTiles(meld.tiles);
  const key = {
    pung: isHonorMeld ? 'PUNG_HONOR' : 'PUNG_SIMPLE',
    kong: isHonorMeld ? 'KONG_HONOR' : 'KONG_SIMPLE',
    chow: 'CHOW',
    pair: null, // pair scores 0 base (kept for completeness)
  }[meld.type];
  if (!key) return 0;
  return BASE_POINTS[key][meld.concealed ? 'concealed' : 'exposed'];
}

// Honor = wind (WE/WS/WW/WN) or dragon (DR/DG/DW). NOTE: Dots also start with
// 'D' (D1..D9), so classification MUST use the honor set, not the first letter.
function isHonorTiles(tiles) {
  return tiles.length > 0 && isHonor(tiles[0]);
}

/**
 * Core scorer. Returns a full audit breakdown that maps 1:1 onto the `hands`
 * table columns so results are reviewable.
 * @param {WinContext} ctx
 */
export function scoreHand(ctx) {
  const limit = ctx.pointsLimit;               // null => unlimited
  const bonus = mahjongBonus(limit);

  // Step 5 short-circuit: special limit hands score as the limit (or their base
  // when unlimited) and skip the doubles multiplier entirely.
  if (ctx.specialLimitHand) {
    const flowerPts = ctx.flowerCount * FLOWER_SEASON_POINTS;
    const meldBase = sumBasePoints(ctx.melds);
    const raw = meldBase + flowerPts + bonus;
    const finalPts = limit == null ? raw : limit;
    return breakdown({ base: meldBase + flowerPts, flowerPts, bonus, doubles: 0,
      appliedDoubles: [], subtotal: raw, multiplier: 1, preCap: finalPts,
      finalPts, isLimit: true, special: ctx.specialLimitHand });
  }

  const flowerPts = ctx.flowerCount * FLOWER_SEASON_POINTS;   // step 1
  const base = sumBasePoints(ctx.melds) + flowerPts;
  const subtotal = base + bonus;                              // step 2 (pre-double)

  const appliedDoubles = (ctx.doubleFlags || [])
    .filter((k) => (DOUBLES[k] || 0) > 0)
    .map((k) => ({ flag: k, label: DOUBLE_LABELS[k] || k, value: DOUBLES[k] }));
  const totalDoubles = appliedDoubles.reduce((n, d) => n + d.value, 0);
  const multiplier = 2 ** totalDoubles;
  const preCap = subtotal * multiplier;                       // step 3 (multiplicative)

  let finalPts = preCap;
  let isLimit = false;
  if (limit != null && finalPts > limit) { finalPts = limit; isLimit = true; }  // step 4
  finalPts = Math.floor(finalPts);                            // step 6

  // First-tile win resolution (confirmed policy: HIGHER of the two).
  if (ctx.firstTileWinType) {
    finalPts = resolveFirstTileWin(finalPts, limit);
    isLimit = limit != null && finalPts >= limit;
  }

  return breakdown({ base, flowerPts, bonus, doubles: totalDoubles,
    appliedDoubles, subtotal, multiplier, preCap, finalPts, isLimit, special: null });
}

// Human-readable labels for the settlement scoring panel.
export const DOUBLE_LABELS = {
  ALL_CONCEALED_HAND: 'All concealed hand', FULLY_CONCEALED: 'Fully concealed',
  NO_CHOWS: 'No chows', ALL_CHOWS: 'All chows', ALL_KONGS: 'All kongs',
  ALL_CONCEALED_KONGS: 'All concealed kongs', DOUBLE_PUNG: 'Double pung',
  MIXED_DOUBLE_CHOW: 'Mixed double chow', SHORT_STRAIGHT: 'Short straight',
  TWO_TERMINAL_CHOWS: 'Two terminal chows', ALL_ONE_SUIT_HONORS: 'All one suit + honours',
  ALL_HONORS: 'All honours', HEAVENLY_HAND: 'Heavenly hand', EARTHLY_HAND: 'Earthly hand',
  HUMAN_HAND: 'Human hand', LAST_WALL_TILE: 'Won on last wall tile', FINAL_DISCARD: 'Won on final discard',
};

/**
 * "Take the higher of the two" (confirmed). The doubles path (HEAVENLY/EARTHLY/
 * HUMAN = 13 doubles, already capped) vs. the flat §5 "first tile = 1000".
 * At the default 1000-limit both converge to 1000; at other limits this returns
 * whichever is larger (flat 1000 can beat a small-limit cap, or the cap can beat
 * 1000 on a high-limit game).
 */
export function resolveFirstTileWin(doublesResult, pointsLimit) {
  const flat = FIRST_TILE_FLAT_POINTS;
  // In a limited game the flat award is itself treated as a limit hand: it cannot
  // exceed the table's Points Limit (a 500-limit table can't pay a 1000 flat).
  const flatEffective = pointsLimit == null ? flat : Math.min(flat, pointsLimit);
  return Math.max(doublesResult, flatEffective);
}

function breakdown({ base, flowerPts, bonus, doubles, appliedDoubles = [], subtotal = 0,
  multiplier = 1, preCap = 0, finalPts, isLimit, special }) {
  return {
    base_points: base - flowerPts,      // meld-only, for the audit column
    flower_points: flowerPts,
    mahjong_bonus: bonus,
    doubles_count: doubles,
    applied_doubles: appliedDoubles,    // [{flag,label,value}] for the settlement panel
    subtotal,                           // base + flowers + bonus, pre-doubles
    multiplier,                         // 2 ** doubles_count
    pre_cap: Math.floor(preCap),        // subtotal * multiplier, before the limit cap
    final_points: Math.floor(finalPts),
    is_limit_hand: isLimit ? 1 : 0,
    special_hand: special,
  };
}
