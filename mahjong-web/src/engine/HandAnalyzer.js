// =============================================================================
// engine/HandAnalyzer.js — win detection, decomposition, special hands, and
// structural-double detection. Pure functions over canonical tile codes.
//
// A concealed hand at win time contains exactly 3*needSets + 2 tiles, where
// needSets = 4 - exposedMelds.length (declared pungs/kongs are in exposedMelds).
// Chows/pungs/pair are found here; kongs only ever arrive via exposedMelds
// (concealed kongs are declared during play, so they are not loose in the hand).
// =============================================================================

import { makeMeld } from '../domain/melds.js';
import { isSuited, suitOf, rankOf, isHonor } from '../domain/tiles.js';

// Canonical ordering so recursion always consumes the lowest tile first.
const ORDER = (() => {
  const o = [];
  for (const s of ['D', 'B', 'C']) for (let r = 1; r <= 9; r++) o.push(`${s}${r}`);
  return o.concat(['WE', 'WS', 'WW', 'WN', 'DR', 'DG', 'DW']);
})();

const TERMINALS_HONORS = ['D1', 'D9', 'B1', 'B9', 'C1', 'C9', 'WE', 'WS', 'WW', 'WN', 'DR', 'DG', 'DW'];

function toCounts(tiles) {
  const c = {};
  for (const t of tiles) c[t] = (c[t] || 0) + 1;
  return c;
}
function firstTile(counts) {
  for (const code of ORDER) if (counts[code] > 0) return code;
  return null;
}

/** Enumerate every way to split `counts` into exactly `need` sets (pung/chow). */
function enumerateSets(counts, need) {
  if (need === 0) return ORDER.every((code) => !counts[code]) ? [[]] : [];
  const code = firstTile(counts);
  if (!code) return [];
  const out = [];
  if (counts[code] >= 3) {                                  // pung branch
    const c2 = { ...counts }; c2[code] -= 3;
    for (const rest of enumerateSets(c2, need - 1)) out.push([makeMeld('pung', [code, code, code]), ...rest]);
  }
  if (isSuited(code)) {                                     // chow branch
    const r = rankOf(code), s = suitOf(code);
    const b = `${s}${r + 1}`, d = `${s}${r + 2}`;
    if (r <= 7 && counts[b] > 0 && counts[d] > 0) {
      const c2 = { ...counts }; c2[code]--; c2[b]--; c2[d]--;
      for (const rest of enumerateSets(c2, need - 1)) out.push([makeMeld('chow', [code, b, d]), ...rest]);
    }
  }
  return out;
}

/**
 * All full decompositions (exposedMelds + concealed sets + pair). Empty if the
 * concealed tiles are not a legal completion. De-dup is unnecessary because the
 * lowest-first recursion yields each ordered decomposition once.
 * @returns {Meld[][]}
 */
export function decompose(concealedTiles, exposedMelds = []) {
  const need = 4 - exposedMelds.length;
  if (need < 0) return [];
  const counts = toCounts(concealedTiles);
  const results = [];
  for (const pairCode of Object.keys(counts)) {
    if (counts[pairCode] >= 2) {
      const c2 = { ...counts }; c2[pairCode] -= 2;
      for (const sets of enumerateSets(c2, need)) {
        results.push([...exposedMelds, ...sets, makeMeld('pair', [pairCode, pairCode])]);
      }
    }
  }
  return results;
}

export function isWinningHand(concealedTiles, exposedMelds = []) {
  if (exposedMelds.length === 0 && isThirteenOrphans(concealedTiles)) return true;
  return decompose(concealedTiles, exposedMelds).length > 0;
}

/** §5 Thirteen Orphans: all 13 terminal/honor types present, exactly one paired. */
export function isThirteenOrphans(concealedTiles) {
  if (concealedTiles.length !== 14) return false;
  const counts = toCounts(concealedTiles);
  if (Object.keys(counts).some((c) => !TERMINALS_HONORS.includes(c))) return false;
  return TERMINALS_HONORS.every((c) => counts[c] >= 1) && TERMINALS_HONORS.some((c) => counts[c] === 2);
}

/** §5 All Flowers & Seasons limit hand: all 8 flowers+seasons collected. */
export function isAllFlowersSeasons(flowerCodes) {
  return new Set(flowerCodes).size === 8;
}

/**
 * Structural doubles for ONE decomposition (keys into DOUBLES). Timing/first-tile
 * and fully-concealed doubles are injected by GameEngine, not detected here.
 *
 * Documented resolutions (open questions #5–#7):
 *  - NO_CHOWS and ALL_CHOWS are mutually exclusive by construction.
 *  - DOUBLE_PUNG counts pung/kong triplets of the same rank in different suits;
 *    awarded once if any such pair exists.
 *  - ALL_ONE_SUIT_HONORS requires a single suited suit AND at least one honor
 *    (a pure single-suit-no-honor "full flush" gets no listed double).
 */
export function detectStructuralDoubles(melds) {
  const flags = [];
  const sets = melds.filter((m) => m.type !== 'pair');
  const chows = sets.filter((m) => m.type === 'chow');
  const triplets = sets.filter((m) => m.type === 'pung' || m.type === 'kong');
  const allTiles = melds.flatMap((m) => m.tiles);

  if (chows.length === 0) flags.push('NO_CHOWS');
  if (chows.length === 4) flags.push('ALL_CHOWS');
  if (sets.length === 4 && sets.every((m) => m.type === 'kong')) {
    flags.push('ALL_KONGS');
    if (sets.every((m) => m.concealed)) flags.push('ALL_CONCEALED_KONGS');
  }

  // Double Pung: same rank, two different suits (suited triplets only).
  const tripByRank = {};
  for (const t of triplets) {
    if (!isSuited(t.tiles[0])) continue;
    const r = rankOf(t.tiles[0]);
    (tripByRank[r] ||= new Set()).add(suitOf(t.tiles[0]));
  }
  if (Object.values(tripByRank).some((suits) => suits.size >= 2)) flags.push('DOUBLE_PUNG');

  // Mixed Double Chow: identical chow (same ranks) in two different suits.
  const chowByRank = {};
  for (const c of chows) {
    const r = rankOf(c.tiles[0]);
    (chowByRank[r] ||= new Set()).add(suitOf(c.tiles[0]));
  }
  if (Object.values(chowByRank).some((suits) => suits.size >= 2)) flags.push('MIXED_DOUBLE_CHOW');

  // Short Straight: 123+456 or 456+789 in the same suit.
  for (const s of ['D', 'B', 'C']) {
    const starts = new Set(chows.filter((c) => suitOf(c.tiles[0]) === s).map((c) => rankOf(c.tiles[0])));
    if ((starts.has(1) && starts.has(4)) || (starts.has(4) && starts.has(7))) { flags.push('SHORT_STRAIGHT'); break; }
  }

  // Two Terminal Chows: a 1-2-3 and a 7-8-9 (any suits).
  const chowStarts = chows.map((c) => rankOf(c.tiles[0]));
  if (chowStarts.includes(1) && chowStarts.includes(7)) flags.push('TWO_TERMINAL_CHOWS');

  // Suit composition.
  const suitedSuits = new Set(allTiles.filter(isSuited).map(suitOf));
  const hasHonor = allTiles.some(isHonor);
  if (suitedSuits.size === 0 && hasHonor) flags.push('ALL_HONORS');
  else if (suitedSuits.size === 1 && hasHonor) flags.push('ALL_ONE_SUIT_HONORS');

  return flags;
}
