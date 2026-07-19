// =============================================================================
// persistence/handRecords.js — PURE mapping from an engine settlement result to
// the row shapes for `hands`, `hand_results`, and `hand_events`. Kept free of any
// DB access so the (subtle) money/flower/penalty split is unit-testable without a
// database. HandRepository.saveHand just writes what this returns, in one tx.
//
// Money split (§7): result.moneyDeltas already folds the immediate $2 flower
// payments into each seat's net. hand_results stores them separately, so:
//   settlementDelta = moneyDeltas[seat] - flowerPayments[seat]
//   flower_pay_delta = flowerPayments[seat]
// On a false Mahjong the settlement portion IS the penalty, so it lands in
// penalty_delta instead of money_delta. Either way the 4 money columns sum with
// the flower column to moneyDeltas[seat], and Σ moneyDeltas over the table is 0.
// =============================================================================

import { SEATS } from '../domain/melds.js';

/**
 * @param {Object}  p
 * @param {number}  p.handNumber        1-based within the game
 * @param {string}  p.roundWind         'E'|'S'|'W'|'N'
 * @param {string}  p.dealerSeat        'E'|'S'|'W'|'N'
 * @param {Object}  p.cfg               { limitMode, pointsLimit, moneyLimit }
 * @param {Object}  p.result            GameEngine settlement result
 * @param {Object}  p.seatPlayerIds     { E,S,W,N -> player id }
 * @param {Object}  p.balancesAfter     { seat -> money after this hand }
 * @param {Object} [p.pointsAfter]      { seat -> profile points after } (optional)
 * @returns {{hand:Object, results:Object[], events:Object[]}}
 */
export function buildHandRecords({ handNumber, roundWind, dealerSeat, cfg, result,
  seatPlayerIds, balancesAfter, pointsAfter = {} }) {
  const unlimited = cfg.limitMode === 'unlimited';
  const isFalse = result.resultType === 'false_mahjong';

  const hand = {
    hand_number: handNumber,
    round_wind: roundWind,
    dealer_seat: dealerSeat,
    limit_mode: cfg.limitMode,
    points_limit: unlimited ? null : cfg.pointsLimit,
    money_limit: unlimited ? null : cfg.moneyLimit,
    result_type: result.resultType,
    winner_seat: result.winnerSeat ?? null,
    base_points: result.base_points ?? null,
    flower_points: result.flower_points ?? null,
    mahjong_bonus: result.mahjong_bonus ?? null,
    doubles_count: result.doubles_count ?? null,
    final_points: result.final_points ?? null,
    is_limit_hand: result.is_limit_hand ? 1 : 0,
    fully_concealed: result.fullyConcealed ? 1 : 0,
    special_hand: result.special_hand ?? null,
    win_timing: result.timing ?? null,
    wall_remaining: result.wallRemaining ?? null,
  };

  const flowerPayments = result.flowerPayments || {};
  const debts = result.debts || {};
  const results = SEATS.map((seat) => {
    const net = result.moneyDeltas[seat] || 0;      // settlement + flowers combined
    const flower = flowerPayments[seat] || 0;
    const settlement = net - flower;                 // pure settlement portion
    const isWinner = seat === result.winnerSeat ? 1 : 0;
    return {
      player_id: seatPlayerIds[seat],
      seat,
      is_winner: isWinner,
      points_delta: isWinner ? (result.final_points || 0) : 0,
      money_delta: isFalse ? 0 : settlement,
      flower_pay_delta: flower,
      penalty_delta: isFalse ? settlement : 0,
      debt_incurred: debts[seat] || 0,
      money_after: balancesAfter[seat] ?? 0,
      points_after: pointsAfter[seat] ?? 0,
    };
  });

  const events = (result.events || []).map((e) => ({
    seq: e.seq,
    seat: e.seat,
    event_type: e.event_type,
    tile_code: e.tile_code ?? null,
    meta: e.meta ?? null,
  }));

  return { hand, results, events };
}
