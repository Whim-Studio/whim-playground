// =============================================================================
// domain/melds.js — meld value object + classification helpers (data + tiny
// pure predicates only; all *decisions* live in the engine).
// =============================================================================

export const MeldType = Object.freeze({
  PUNG: 'pung', KONG: 'kong', CHOW: 'chow', PAIR: 'pair',
});

/**
 * A meld. `concealed` matters for scoring (§6). Chows are ALWAYS concealed by
 * rule; a KONG may be exposed-upgrade or concealed. `fromSeat` records the
 * discarder when the meld was claimed (null if self-drawn) — needed for the
 * fully-concealed check and §8 verification.
 * @typedef {{type:string, tiles:string[], concealed:boolean, fromSeat:?string}} Meld
 */
export function makeMeld(type, tiles, { concealed = true, fromSeat = null } = {}) {
  return Object.freeze({ type, tiles: Object.freeze(tiles.slice()), concealed, fromSeat });
}

export const SEATS = ['E', 'S', 'W', 'N'];
export const WINDS_ORDER = ['E', 'S', 'W', 'N'];

/** Next seat in counter-clockwise play order (turn passes E→S→W→N→E). */
export function nextSeat(seat) {
  return SEATS[(SEATS.indexOf(seat) + 1) % 4];
}

/**
 * Distance from discarder to a claimant in play order (1..3). Lower = "nearest
 * counter-clockwise", the §3 tie-break for simultaneous Mahjong claims.
 */
export function claimDistance(fromSeat, claimantSeat) {
  return (SEATS.indexOf(claimantSeat) - SEATS.indexOf(fromSeat) + 4) % 4;
}
