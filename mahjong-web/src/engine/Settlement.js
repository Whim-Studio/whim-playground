// =============================================================================
// engine/Settlement.js — Rulebook §7 money conversion + §8 penalty conversion.
// Pure functions over integers. Rounding is ALWAYS floor, and — per §7 — money
// is summed per player then rounded ONCE at settlement, never per intermediate.
// =============================================================================

/**
 * §7 currency payout for a winning hand.
 *   Limited:   floor( (handPoints / pointsLimit) * moneyLimit )
 *   Unlimited: $1 per point  => floor(handPoints)
 * @param {number} handPoints  final points (already floored by ScoringEngine)
 * @param {?number} pointsLimit null => unlimited
 * @param {?number} moneyLimit  null => unlimited ($1/pt)
 * @returns {number} whole currency units
 */
export function moneyForPoints(handPoints, pointsLimit, moneyLimit) {
  if (pointsLimit == null || moneyLimit == null) return Math.floor(handPoints); // $1/pt
  return Math.floor((handPoints / pointsLimit) * moneyLimit);
}

/**
 * §8 false-Mahjong / false-concealed penalty owed to EACH other player.
 * Confirmed policy: convert the 1000-point penalty via the SAME money formula.
 *   Limited:   floor( (1000 / pointsLimit) * moneyLimit )  per opponent
 *   Unlimited: $1000 per opponent
 */
export function falseMahjongPenaltyPerOpponent(pointsLimit, moneyLimit) {
  const PENALTY_POINTS = 1000;
  if (pointsLimit == null || moneyLimit == null) return 1000;
  return Math.floor((PENALTY_POINTS / pointsLimit) * moneyLimit);
}

/**
 * Apply a settlement to four seat balances, honoring §7 partial-payment +
 * "go until broke" bankruptcy. Returns per-seat deltas AND whether the game must
 * end (any balance <= 0 after settlement).
 *
 * NOTE: this is the reviewable settlement contract. The winner is paid by the 3
 * others; a payer who cannot cover pays what they can and the remainder becomes
 * `debt` (tracked, still owed). Flower $2 payments are settled SEPARATELY and
 * IMMEDIATELY during the hand (see engine flow) and are not re-applied here.
 *
 * @param {{seat:string, money:number}[]} balances length 4, current money
 * @param {string} winnerSeat
 * @param {number} payout amount each loser owes the winner (whole units)
 * @returns {{deltas:Object, debts:Object, bankrupt:boolean}}
 */
export function settleWin(balances, winnerSeat, payout) {
  const deltas = {}; const debts = {};
  let collected = 0;
  for (const b of balances) {
    if (b.seat === winnerSeat) continue;
    const canPay = Math.max(0, b.money);
    const paid = Math.min(canPay, payout);
    deltas[b.seat] = -paid;
    debts[b.seat] = payout - paid;      // unpaid remainder -> owed_debt
    collected += paid;
  }
  deltas[winnerSeat] = collected;       // winner receives only what was collectible
  const after = balances.map((b) => b.money + (deltas[b.seat] || 0));
  const bankrupt = after.some((m) => m <= 0);
  return { deltas, debts, bankrupt };
}

/**
 * §8 false-Mahjong settlement: the offender pays `penalty` to EACH other player,
 * honoring the same §7 partial-payment + bankruptcy rules as a win (the offender
 * pays what they can; any shortfall is tracked as debt still owed). Opponents are
 * paid in canonical seat order until the offender's money is exhausted.
 *
 * @param {{seat:string, money:number}[]} balances length 4, current money
 * @param {string} offenderSeat
 * @param {number} penalty per-opponent penalty (whole units)
 * @returns {{deltas:Object, debts:Object, bankrupt:boolean}}
 */
export function settleFalseMahjong(balances, offenderSeat, penalty) {
  const deltas = {}; const debts = {};
  let avail = Math.max(0, (balances.find((b) => b.seat === offenderSeat) || {}).money || 0);
  let paidTotal = 0;
  for (const b of balances) {
    if (b.seat === offenderSeat) continue;
    const paid = Math.min(avail, penalty);
    avail -= paid;
    deltas[b.seat] = paid;              // opponent receives what the offender could pay
    debts[b.seat] = penalty - paid;     // shortfall still owed to this opponent
    paidTotal += paid;
  }
  deltas[offenderSeat] = -paidTotal;    // offender loses only what was collectible
  const after = balances.map((b) => b.money + (deltas[b.seat] || 0));
  const bankrupt = after.some((m) => m <= 0);
  return { deltas, debts, bankrupt };
}
