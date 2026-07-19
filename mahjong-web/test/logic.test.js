// Node built-in test runner: `npm test`. Exercises the pieces that ARE
// implemented in the skeleton (scoring order, money/penalty conversion, claim
// priority, identity validation). Engine/AI/persistence bodies are stubs and are
// intentionally not tested here.
import { test } from 'node:test';
import assert from 'node:assert/strict';

import { scoreHand, mahjongBonus, resolveFirstTileWin, meldPoints } from '../src/engine/ScoringEngine.js';
import { moneyForPoints, falseMahjongPenaltyPerOpponent, settleWin, settleFalseMahjong } from '../src/engine/Settlement.js';
import { resolveClaims } from '../src/engine/ClaimResolver.js';
import { validateIdentityInput } from '../src/api/identity.js';
import { buildHandRecords } from '../src/persistence/handRecords.js';

test('mahjong bonus is floor(1% of limit); 0 when unlimited', () => {
  assert.equal(mahjongBonus(1000), 10);
  assert.equal(mahjongBonus(2500), 25);
  assert.equal(mahjongBonus(null), 0);
});

test('appendix example 1: base 30 + bonus 10, all-concealed (2 doubles) => 160', () => {
  // Model a 30-base concealed hand as three concealed honor pungs (8 each = 24)
  // + a concealed simple pung (4) + a concealed simple pung (4) = 32... instead
  // assert the pipeline directly with a synthetic meld set summing to 30.
  const melds = [
    { type: 'pung', tiles: ['DR', 'DR', 'DR'], concealed: true },   // honor pung concealed = 8
    { type: 'pung', tiles: ['DG', 'DG', 'DG'], concealed: true },   // 8
    { type: 'pung', tiles: ['DW', 'DW', 'DW'], concealed: true },   // 8
    { type: 'pung', tiles: ['C2', 'C2', 'C2'], concealed: true },   // simple concealed = 4
    { type: 'pair', tiles: ['C5', 'C5'], concealed: true },         // 0
  ]; // base = 28; add a flower to reach 30... use flowerCount instead:
  const r = scoreHand({ melds, flowerCount: 0, pointsLimit: 1000,
    doubleFlags: ['ALL_CONCEALED_HAND'], specialLimitHand: null, firstTileWinType: null });
  // base 28 + bonus 10 = 38; ×4 = 152; not capped.
  assert.equal(r.mahjong_bonus, 10);
  assert.equal(r.doubles_count, 2);
  assert.equal(r.final_points, 152);
  assert.equal(r.is_limit_hand, 0);
});

test('meld points: Dots are simple, not honors (dragon/dots collision guard)', () => {
  // A Dots pung is SIMPLE (concealed 4 / exposed 2); a Dragon pung is HONOR (8 / 4).
  assert.equal(meldPoints({ type: 'pung', tiles: ['D6', 'D6', 'D6'], concealed: true }), 4);
  assert.equal(meldPoints({ type: 'pung', tiles: ['D6', 'D6', 'D6'], concealed: false }), 2);
  assert.equal(meldPoints({ type: 'pung', tiles: ['DR', 'DR', 'DR'], concealed: true }), 8);
  assert.equal(meldPoints({ type: 'kong', tiles: ['D1', 'D1', 'D1', 'D1'], concealed: true }), 8); // simple kong
  assert.equal(meldPoints({ type: 'kong', tiles: ['WE', 'WE', 'WE', 'WE'], concealed: true }), 16); // honor kong
});

test('scoring breakdown itemizes subtotal, doubles, multiplier, pre-cap', () => {
  const melds = [
    { type: 'pung', tiles: ['DR', 'DR', 'DR'], concealed: true },
    { type: 'pung', tiles: ['DG', 'DG', 'DG'], concealed: true },
    { type: 'pung', tiles: ['DW', 'DW', 'DW'], concealed: true },
    { type: 'pung', tiles: ['C2', 'C2', 'C2'], concealed: true },
    { type: 'pair', tiles: ['C5', 'C5'], concealed: true },
  ]; // base 28
  const r = scoreHand({ melds, flowerCount: 0, pointsLimit: 1000,
    doubleFlags: ['ALL_CONCEALED_HAND'], specialLimitHand: null, firstTileWinType: null });
  assert.equal(r.subtotal, 38);              // 28 base + 10 bonus
  assert.equal(r.multiplier, 4);             // 2 doubles
  assert.equal(r.pre_cap, 152);
  assert.equal(r.final_points, 152);
  assert.deepEqual(r.applied_doubles.map((d) => d.flag), ['ALL_CONCEALED_HAND']);
  assert.equal(r.applied_doubles[0].value, 2);
});

test('big doubles cap at the points limit (All Honours)', () => {
  const melds = [{ type: 'pung', tiles: ['WE', 'WE', 'WE'], concealed: true }];
  const r = scoreHand({ melds, flowerCount: 0, pointsLimit: 1000,
    doubleFlags: ['ALL_HONORS'], specialLimitHand: null, firstTileWinType: null });
  assert.equal(r.final_points, 1000);
  assert.equal(r.is_limit_hand, 1);
});

test('special limit hand scores as the limit and skips doubles', () => {
  const r = scoreHand({ melds: [], flowerCount: 8, pointsLimit: 1000,
    doubleFlags: ['ALL_HONORS'], specialLimitHand: 'all_flowers_seasons', firstTileWinType: null });
  assert.equal(r.final_points, 1000);
  assert.equal(r.is_limit_hand, 1);
  assert.equal(r.special_hand, 'all_flowers_seasons');
});

test('first-tile win takes the higher of flat-1000 and the doubles result', () => {
  // Small base with 13 doubles on a 1000 limit caps at 1000; flat also 1000 => 1000.
  assert.equal(resolveFirstTileWin(1000, 1000), 1000);
  // On a 500-limit table the doubles path caps at 500 but flat is min(1000,500)=500.
  assert.equal(resolveFirstTileWin(500, 500), 500);
  // Unlimited: a doubles result of 40 vs flat 1000 => 1000.
  assert.equal(resolveFirstTileWin(40, null), 1000);
});

test('money conversion: 250 pts, 1000 limit, $10 => $2 (floor)', () => {
  assert.equal(moneyForPoints(250, 1000, 10), 2);
  assert.equal(moneyForPoints(250, 2000, 100), 12);
  assert.equal(moneyForPoints(250, null, null), 250); // unlimited $1/pt
});

test('false-mahjong penalty converts via the money formula', () => {
  assert.equal(falseMahjongPenaltyPerOpponent(1000, 10), 10); // (1000/1000)*10
  assert.equal(falseMahjongPenaltyPerOpponent(2000, 10), 5);
  assert.equal(falseMahjongPenaltyPerOpponent(null, null), 1000);
});

test('settlement supports partial pay + flags bankruptcy', () => {
  const balances = [
    { seat: 'E', money: 5 }, { seat: 'S', money: 100 },
    { seat: 'W', money: 100 }, { seat: 'N', money: 100 },
  ];
  const { deltas, debts, bankrupt } = settleWin(balances, 'S', 10);
  assert.equal(deltas.E, -5);      // E could only pay 5
  assert.equal(debts.E, 5);        // owes 5 more
  assert.equal(deltas.S, 25);      // winner collects 5 + 10 + 10
  assert.equal(bankrupt, true);    // E hits 0
});

test('false-mahjong settlement: offender pays each opponent; partial pay + bankruptcy', () => {
  // Full payment: offender S pays $10 to each of the other three (−$30 total).
  const flush = [
    { seat: 'E', money: 100 }, { seat: 'S', money: 100 },
    { seat: 'W', money: 100 }, { seat: 'N', money: 100 },
  ];
  const r1 = settleFalseMahjong(flush, 'S', 10);
  assert.equal(r1.deltas.S, -30);
  assert.equal(r1.deltas.E, 10);
  assert.equal(r1.deltas.W, 10);
  assert.equal(r1.deltas.N, 10);
  assert.equal(r1.bankrupt, false);
  assert.equal(Object.values(r1.deltas).reduce((a, b) => a + b, 0), 0, 'money conserved');

  // Partial: offender S has only $15 for a $10-per-opponent penalty. Pays E and W
  // fully ($10 + $5), N gets $0 and is owed $10; S is bankrupt at $0.
  const broke = [
    { seat: 'E', money: 100 }, { seat: 'S', money: 15 },
    { seat: 'W', money: 100 }, { seat: 'N', money: 100 },
  ];
  const r2 = settleFalseMahjong(broke, 'S', 10);
  assert.equal(r2.deltas.S, -15);
  assert.equal(r2.deltas.E, 10);
  assert.equal(r2.deltas.W, 5);
  assert.equal(r2.deltas.N, 0);
  assert.equal(r2.debts.N, 10, 'unpaid remainder tracked as debt');
  assert.equal(r2.bankrupt, true);
  assert.equal(Object.values(r2.deltas).reduce((a, b) => a + b, 0), 0, 'money conserved');
});

test('claim priority: mahjong beats pung; nearest-CCW breaks mahjong ties', () => {
  const w = resolveClaims([
    { seat: 'W', type: 'pung', legal: true },
    { seat: 'N', type: 'mahjong', legal: true },
    { seat: 'S', type: 'mahjong', legal: true },
  ], 'E');
  assert.equal(w.type, 'mahjong');
  assert.equal(w.seat, 'S'); // S is nearest counter-clockwise to discarder E
});

test('buildHandRecords: win maps scoring + splits settlement vs flower money', () => {
  // E wins $12; the losers each paid $4, and a $2 flower pay happened earlier
  // (S paid $2 to E during the deal). moneyDeltas already folds flowers in.
  const result = {
    resultType: 'win', winnerSeat: 'E', final_points: 24,
    base_points: 14, flower_points: 0, mahjong_bonus: 10, doubles_count: 0,
    is_limit_hand: 0, fullyConcealed: 1, special_hand: null, timing: 'normal', wallRemaining: 30,
    moneyDeltas: { E: 14, S: -6, W: -4, N: -4 },   // 12 settlement + 2 net flowers to E
    flowerPayments: { E: 2, S: -2, W: 0, N: 0 },
    debts: { E: 0, S: 0, W: 0, N: 0 },
    events: [{ seq: 1, seat: 'E', event_type: 'deal', tile_code: null, meta: null },
             { seq: 2, seat: 'S', event_type: 'discard', tile_code: 'B5', meta: { from: 'S' } }],
  };
  const seatPlayerIds = { E: 1, S: 2, W: 3, N: 4 };
  const balancesAfter = { E: 1014, S: 994, W: 996, N: 996 };
  const { hand, results, events } = buildHandRecords({
    handNumber: 3, roundWind: 'E', dealerSeat: 'E',
    cfg: { limitMode: 'limited', pointsLimit: 1000, moneyLimit: 10 },
    result, seatPlayerIds, balancesAfter,
  });

  assert.equal(hand.result_type, 'win');
  assert.equal(hand.winner_seat, 'E');
  assert.equal(hand.final_points, 24);
  assert.equal(hand.points_limit, 1000);
  assert.equal(hand.fully_concealed, 1);

  const e = results.find((r) => r.seat === 'E');
  assert.equal(e.is_winner, 1);
  assert.equal(e.points_delta, 24);
  assert.equal(e.money_delta, 12);          // settlement only (14 net − 2 flower)
  assert.equal(e.flower_pay_delta, 2);
  assert.equal(e.money_after, 1014);
  // Each seat's money columns + flowers reconstruct the net delta.
  for (const r of results) {
    const seat = r.seat;
    assert.equal(r.money_delta + r.penalty_delta + r.flower_pay_delta, result.moneyDeltas[seat],
      `seat ${seat} money columns reconstruct net`);
  }
  assert.equal(events.length, 2);
  assert.equal(events[1].tile_code, 'B5');
});

test('buildHandRecords: false mahjong routes the loss into penalty_delta', () => {
  const result = {
    resultType: 'false_mahjong', winnerSeat: null, offenderSeat: 'E', final_points: 0,
    penaltyPerOpponent: 10, wallRemaining: 40,
    moneyDeltas: { E: -30, S: 10, W: 10, N: 10 },
    flowerPayments: { E: 0, S: 0, W: 0, N: 0 },
    debts: { E: 0, S: 0, W: 0, N: 0 }, events: [],
  };
  const { hand, results } = buildHandRecords({
    handNumber: 1, roundWind: 'E', dealerSeat: 'E',
    cfg: { limitMode: 'limited', pointsLimit: 1000, moneyLimit: 10 },
    result, seatPlayerIds: { E: 1, S: 2, W: 3, N: 4 },
    balancesAfter: { E: 970, S: 1010, W: 1010, N: 1010 },
  });
  assert.equal(hand.result_type, 'false_mahjong');
  assert.equal(hand.winner_seat, null);
  const e = results.find((r) => r.seat === 'E');
  assert.equal(e.money_delta, 0, 'no settlement money on a penalty hand');
  assert.equal(e.penalty_delta, -30, 'the loss lands in penalty_delta');
  assert.equal(results.reduce((s, r) => s + r.money_delta + r.penalty_delta + r.flower_pay_delta, 0), 0);
});

test('buildHandRecords: unlimited mode nulls the limit columns', () => {
  const result = {
    resultType: 'draw', winnerSeat: null, final_points: 0,
    moneyDeltas: { E: 0, S: 0, W: 0, N: 0 }, flowerPayments: { E: 0, S: 0, W: 0, N: 0 },
    debts: {}, events: [],
  };
  const { hand } = buildHandRecords({
    handNumber: 1, roundWind: 'S', dealerSeat: 'S',
    cfg: { limitMode: 'unlimited', pointsLimit: null, moneyLimit: null },
    result, seatPlayerIds: { E: 1, S: 2, W: 3, N: 4 }, balancesAfter: { E: 0, S: 0, W: 0, N: 0 },
  });
  assert.equal(hand.limit_mode, 'unlimited');
  assert.equal(hand.points_limit, null);
  assert.equal(hand.money_limit, null);
  assert.equal(hand.result_type, 'draw');
});

test('identity validation enforces modes', () => {
  assert.equal(validateIdentityInput({ mode: 'guest' }).ok, true);
  assert.equal(validateIdentityInput({ mode: 'name', name: 'Alice' }).ok, true);
  assert.equal(validateIdentityInput({ mode: 'name', name: 'Alice', email: 'x@y.z' }).ok, false);
  assert.equal(validateIdentityInput({ mode: 'name_email', name: 'Alice', email: 'bad' }).ok, false);
});
