// =============================================================================
// domain/tiles.js — the 144-tile set, canonical tile codes, and wall building.
// Pure + deterministic. A tile is a plain immutable object {code, suit, rank}.
//
// Canonical codes (also used by hand_events.tile_code):
//   Dots       D1..D9      Bamboo B1..B9      Characters C1..C9
//   Winds      WE WS WW WN Dragons DR (red) DG (green) DW (white)
//   Flowers    F1..F4      Seasons S1..S4
// =============================================================================

export const SUIT = Object.freeze({
  DOTS: 'D', BAMBOO: 'B', CHAR: 'C',
  WIND: 'W', DRAGON: 'DR', FLOWER: 'F', SEASON: 'S',
});

export const HONORS = new Set(['WE', 'WS', 'WW', 'WN', 'DR', 'DG', 'DW']);
export const WINDS = ['WE', 'WS', 'WW', 'WN'];
export const DRAGONS = ['DR', 'DG', 'DW'];

export function isHonor(code) { return HONORS.has(code); }
export function isFlowerOrSeason(code) { return code[0] === 'F' || code[0] === 'S'; }
// Suited = Dots/Bamboo/Characters with a numeric rank. Note dragons are DR/DG/DW
// (also start with 'D') — the digit check excludes them.
export function isSuited(code) { return 'DBC'.includes(code[0]) && code.length === 2 && code[1] >= '1' && code[1] <= '9'; }
export function suitOf(code) { return code[0]; }           // 'D' | 'B' | 'C' | ...
export function rankOf(code) { return isSuited(code) ? Number(code[1]) : null; }

/** Build the full ordered 144-tile set (before shuffle). */
export function fullTileSet() {
  const tiles = [];
  const push = (code) => tiles.push(Object.freeze({ code, suit: suitOf(code), rank: rankOf(code) }));
  for (const s of ['D', 'B', 'C']) for (let r = 1; r <= 9; r++) for (let i = 0; i < 4; i++) push(`${s}${r}`);
  for (const w of WINDS) for (let i = 0; i < 4; i++) push(w);
  for (const d of DRAGONS) for (let i = 0; i < 4; i++) push(d);
  for (let n = 1; n <= 4; n++) push(`F${n}`);   // 4 distinct flowers
  for (let n = 1; n <= 4; n++) push(`S${n}`);   // 4 distinct seasons
  return tiles; // length 144
}

/**
 * Fisher–Yates shuffle using an injected RNG (default Math.random). Passing a
 * seeded RNG makes whole hands reproducible for tests and for server-side replay
 * verification (§8). NOTE: the RNG/shuffle is the ONE place hidden state is set;
 * it must live server-side only — the client never receives wall order.
 */
export function shuffle(tiles, rng = Math.random) {
  const a = tiles.slice();
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(rng() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}
