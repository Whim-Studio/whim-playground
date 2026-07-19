// =============================================================================
// engine/ClaimResolver.js — §3 claim legality + priority. This IS implemented
// (small, rule-critical). Note: per §4 a Kong may NOT be claimed from a discard,
// so the "Kong" priority tier is effectively vestigial for discards; it remains
// in the ordering for completeness and future rule variants. See open questions.
// =============================================================================

import { claimDistance } from '../domain/melds.js';

export const ClaimType = Object.freeze({ MAHJONG: 'mahjong', KONG: 'kong', PUNG: 'pung' });

const PRIORITY = { mahjong: 3, kong: 2, pung: 1 };

/**
 * Resolve competing claims on a single discard.
 * @param {{seat:string, type:string, legal:boolean}[]} claims  candidate claims
 * @param {string} discarderSeat
 * @returns {?{seat:string, type:string}} winning claim, or null if none
 *
 * Rules:
 *  - Mahjong > Kong > Pung (Kong-from-discard is illegal and should never be
 *    passed as legal:true; guarded anyway).
 *  - Ties within the top type are broken by nearest counter-clockwise to the
 *    discarder (smallest claimDistance) — this matters for simultaneous Mahjong.
 */
export function resolveClaims(claims, discarderSeat) {
  const legal = claims.filter((c) => c.legal && c.type !== ClaimType.KONG);
  if (legal.length === 0) return null;
  const topPriority = Math.max(...legal.map((c) => PRIORITY[c.type]));
  const contenders = legal.filter((c) => PRIORITY[c.type] === topPriority);
  contenders.sort((a, b) => claimDistance(discarderSeat, a.seat) - claimDistance(discarderSeat, b.seat));
  const w = contenders[0];
  return { seat: w.seat, type: w.type };
}
