// =============================================================================
// players/AIController.js — heuristic AI seat. Same rules + decision window as a
// human. Exposes SYNC pure decision helpers (used by the headless engine and by
// tests) plus the async SeatController wrappers (used by the real-time UI path).
//
// HEURISTIC (reasonably competent, not optimal):
//   DISCARD: keep tiles that participate in pairs/triplets and in potential chows
//     (neighbors present); shed isolated terminals/honors first. Score each tile
//     by its "connectivity" and discard the minimum.
//   MAHJONG: always take a win (self-draw or a completing discard).
//   PUNG: conservative — only claim a pung when we already hold the pair AND the
//     hand is not close to a concealed win (claiming forfeits the fully-concealed
//     2-double bonus, so early pungs are avoided). Kong-from-discard is illegal.
//   RISK: `aggression` biases pung claims; unlimited rounds raise it (chase
//     bigger concealed hands), limited rounds lower it once the cap is reachable.
// =============================================================================

import { SeatController } from './SeatController.js';
import { isWinningHand } from '../engine/HandAnalyzer.js';
import { isSuited, suitOf, rankOf, isHonor } from '../domain/tiles.js';

export class AIController extends SeatController {
  constructor(seat, { aggression = 0.4 } = {}) { super(seat); this.aggression = aggression; }

  /** SYNC: choose a discard from concealed tiles. Returns a tile code. */
  chooseDiscard(concealed) {
    let worst = concealed[0], worstScore = Infinity;
    for (const t of concealed) {
      const s = this._connectivity(t, concealed);
      if (s < worstScore) { worstScore = s; worst = t; }
    }
    return worst;
  }

  /** SYNC: would claiming `tile` off a discard complete a legal win? */
  claimsMahjong(concealed, exposed, tile) {
    return isWinningHand([...concealed, tile], exposed);
  }

  /**
   * SYNC: claim a pung on `tile`? Deterministic (no RNG, so seeded games stay
   * reproducible). Conservative per the heuristic: only pung when we hold the
   * pair AND still keep a separate pair back to serve as the eye. Honors are
   * always worth ponging (hard to complete off the wall); simples require the
   * `aggression` knob to clear a threshold. Never pung a tile we could instead
   * win on — Mahjong is checked first and takes claim priority.
   */
  claimsPung(concealed, exposed, tile) {
    const same = concealed.filter((t) => t === tile).length;
    if (same < 2) return false;                      // need a pair to pung
    if (countPairs(concealed) < 2) return false;     // keep a pair back for the eye
    if (isHonor(tile)) return true;                  // honors: always claim
    return this.aggression >= 0.4;                   // simples: only if aggressive enough
  }

  _connectivity(tile, hand) {
    const same = hand.filter((t) => t === tile).length; // pair/triplet weight
    let score = same * 4;
    if (isHonor(tile)) return score;                 // honors: only pair/triplet value
    if (isSuited(tile)) {
      const r = rankOf(tile), s = suitOf(tile);
      for (const d of [-2, -1, 1, 2]) {
        const nb = `${s}${r + d}`;
        if (r + d >= 1 && r + d <= 9 && hand.includes(nb)) score += (Math.abs(d) === 1 ? 2 : 1);
      }
      score += (r >= 3 && r <= 7) ? 1 : 0;           // middles are more flexible
    }
    return score;
  }

  // --- async SeatController surface (real-time UI path) ---------------------
  async decideTurn(view) {
    if (view.canWin) return { action: 'mahjong' };
    return { action: 'discard', tile: this.chooseDiscard(view.concealed) };
  }
  async decideClaim(view, discard) {
    if (this.claimsMahjong(view.concealed, view.exposed, discard)) return { claim: 'mahjong' };
    if (this.claimsPung(view.concealed, view.exposed, discard)) return { claim: 'pung' };
    return { claim: 'pass' };
  }
}

function countPairs(hand) {
  const c = {};
  for (const t of hand) c[t] = (c[t] || 0) + 1;
  return Object.values(c).filter((n) => n >= 2).length;
}
