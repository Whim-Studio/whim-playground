import { test } from 'node:test';
import assert from 'node:assert/strict';

import { isWinningHand, decompose, isThirteenOrphans, detectStructuralDoubles }
  from '../src/engine/HandAnalyzer.js';
import { GameEngine } from '../src/engine/GameEngine.js';
import { AIController } from '../src/players/AIController.js';
import { SEATS, makeMeld } from '../src/domain/melds.js';

// Deterministic RNG so headless games are reproducible.
function mulberry32(seed) {
  return function () {
    seed |= 0; seed = (seed + 0x6D2B79F5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

test('decompose recognizes a standard 4-sets+pair win', () => {
  const hand = ['C1', 'C1', 'C1', 'B2', 'B3', 'B4', 'D5', 'D6', 'D7', 'WE', 'WE', 'WE', 'D9', 'D9'];
  assert.ok(isWinningHand(hand, []));
  assert.ok(decompose(hand, []).length >= 1);
});

test('a non-winning hand decomposes to nothing', () => {
  const hand = ['C1', 'C1', 'C1', 'B2', 'B3', 'B5', 'D5', 'D6', 'D7', 'WE', 'WE', 'WE', 'D9', 'D9'];
  assert.equal(isWinningHand(hand, []), false);
});

test('thirteen orphans detected', () => {
  const hand = ['D1', 'D9', 'B1', 'B9', 'C1', 'C9', 'WE', 'WS', 'WW', 'WN', 'DR', 'DG', 'DW', 'D1'];
  assert.ok(isThirteenOrphans(hand));
  assert.ok(isWinningHand(hand, []));
});

test('structural doubles: NO_CHOWS + DOUBLE_PUNG', () => {
  const melds = decompose(['D5', 'D5', 'D5', 'C5', 'C5', 'C5', 'B1', 'B1', 'B1', 'WE', 'WE', 'WE', 'D9', 'D9'], [])[0];
  const flags = detectStructuralDoubles(melds);
  assert.ok(flags.includes('NO_CHOWS'));
  assert.ok(flags.includes('DOUBLE_PUNG'));
});

test('structural doubles: ALL_ONE_SUIT_HONORS', () => {
  const melds = decompose(['D1', 'D2', 'D3', 'D4', 'D5', 'D6', 'D7', 'D8', 'D9', 'DR', 'DR', 'DR', 'DG', 'DG'], [])[0];
  const flags = detectStructuralDoubles(melds);
  assert.ok(flags.includes('ALL_ONE_SUIT_HONORS'));
});

test('structural doubles: ALL_HONORS', () => {
  const melds = decompose(['WE', 'WE', 'WE', 'WS', 'WS', 'WS', 'WW', 'WW', 'WW', 'DR', 'DR', 'DR', 'DG', 'DG'], [])[0];
  const flags = detectStructuralDoubles(melds);
  assert.ok(flags.includes('ALL_HONORS'));
  assert.ok(flags.includes('NO_CHOWS'));
});

test('headless: 300 full hands complete, stay legal + money-conserving', () => {
  let wins = 0, draws = 0, pungs = 0;
  for (let seed = 1; seed <= 300; seed++) {
    const rng = mulberry32(seed);
    const engine = new GameEngine({
      dealerSeat: 'E', roundWind: 'E', limitMode: 'limited',
      pointsLimit: 1000, moneyLimit: 10, rng,
    });
    const brains = {};
    for (const s of SEATS) brains[s] = new AIController(s, { aggression: 0.5 });
    const balances = { E: 1000, S: 1000, W: 1000, N: 1000 };

    const r = engine.playHand(brains, balances);
    assert.ok(r.resultType === 'win' || r.resultType === 'draw');

    // Money is zero-sum across the table (settlement + immediate flower pays).
    const total = SEATS.reduce((sum, s) => sum + (r.moneyDeltas[s] || 0), 0);
    assert.equal(total, 0, `seed ${seed}: money not conserved (${total})`);

    // Tile-count legality: every seat holds 3*need + (2 unless it won) concealed
    // tiles, where need = 4 - exposedMelds; exposed pungs from claims must be
    // sound. This catches any pung/transfer bug that corrupts hand size.
    pungs += engine.events.filter((e) => e.event_type === 'claim_pung').length;
    for (const s of SEATS) {
      const st = engine.seats[s];
      for (const m of st.exposed) {
        if (m.type === 'pung') assert.ok(m.fromSeat && m.tiles.length === 3, `seed ${seed}: bad pung meld`);
      }
      // At rest a seat holds 13 - 3*melds concealed tiles (a kong's replacement
      // draw keeps the count whole, so each meld counts as 3 here); the winner
      // holds one more (the winning tile).
      const expected = (s === r.winnerSeat ? 14 : 13) - 3 * st.exposed.length;
      assert.equal(st.concealed.length, expected, `seed ${seed} seat ${s}: illegal tile count`);
    }

    if (r.resultType === 'win') {
      wins++;
      assert.ok(r.final_points >= 0);
      assert.ok(r.winnerSeat && SEATS.includes(r.winnerSeat));
      assert.ok(r.final_points <= 1000, `seed ${seed}: exceeded points limit`);
    } else { draws++; }
  }
  assert.equal(wins + draws, 300);
  assert.ok(wins > 0, 'expected at least some wins across 300 hands');
  assert.ok(pungs > 0, 'expected pung claims (turn-transfer path) to execute');
});

test('win by discard exposes the completed pung; self-draw keeps it concealed', () => {
  // Hand = D6 pung + B5 pung + C7 pung + C4C5C6 chow + C2 pair. Won on a D6.
  const hand = ['D6', 'D6', 'D6', 'B5', 'B5', 'B5', 'C7', 'C7', 'C7', 'C4', 'C5', 'C6', 'C2', 'C2'];
  const mk = () => {
    const e = new GameEngine({ dealerSeat: 'E', roundWind: 'E', limitMode: 'unlimited', rng: mulberry32(1) });
    e.seats.E.concealed = [...hand];
    e.wall = [{ code: 'B1' }, { code: 'B2' }]; // non-empty → 'normal' timing, no cap
    return e;
  };

  // Win by discard on D6: that pung is EXPOSED (simple pung 4 → 2). Base = 2+4+4 = 10.
  const eDisc = mk(); eDisc.seats.E.everClaimed = true;
  const disc = eDisc._win('E', { selfDraw: false, timing: 'normal', firstGoAround: false,
    balances: { E: 1000, S: 1000, W: 1000, N: 1000 }, winningTile: 'D6' });
  assert.equal(disc.scoring.basePoints, 10, 'discard win base = 10 (one pung exposed)');
  const d6 = disc.scoring.melds.find((m) => m.tiles[0] === 'D6');
  assert.equal(d6.concealed, false, 'the completed D6 pung is exposed');
  assert.equal(d6.points, 2, 'exposed simple pung scores 2');
  assert.equal(disc.final_points, 10, 'unlimited: base 10, no bonus, no doubles');

  // Self-draw of the same tiles: every pung stays concealed. Base = 4+4+4 = 12.
  const self = (() => { const e = mk(); e.seats.E.everClaimed = true; // isolate base from FC bonus
    return e._win('E', { selfDraw: true, timing: 'normal', firstGoAround: false,
      balances: { E: 1000, S: 1000, W: 1000, N: 1000 } }); })();
  assert.equal(self.scoring.basePoints, 12, 'self-draw base = 12 (all pungs concealed)');
  assert.equal(self.scoring.melds.find((m) => m.tiles[0] === 'D6').concealed, true);
});

test('win by discard completing a CHOW exposes nothing (chows score 0)', () => {
  // D5D6D7 chow + D1D2D3 chow + B5 pung + C7 pung + C2 pair. Won on D6 (in a chow).
  const e = new GameEngine({ dealerSeat: 'E', roundWind: 'E', limitMode: 'unlimited', rng: mulberry32(1) });
  e.seats.E.concealed = ['D5', 'D6', 'D7', 'D1', 'D2', 'D3', 'B5', 'B5', 'B5', 'C7', 'C7', 'C7', 'C2', 'C2'];
  e.seats.E.everClaimed = true;
  e.wall = [{ code: 'B1' }];
  const r = e._win('E', { selfDraw: false, timing: 'normal', firstGoAround: false,
    balances: { E: 1000, S: 1000, W: 1000, N: 1000 }, winningTile: 'D6' });
  // Both pungs remain concealed (winning tile attributed to the chow): 4 + 4 = 8.
  assert.equal(r.scoring.basePoints, 8);
  assert.ok(r.scoring.melds.filter((m) => m.type === 'pung').every((m) => m.concealed));
});

test('concealed kong auto-declares, draws a replacement tile, logs it', () => {
  const engine = new GameEngine({ dealerSeat: 'E', roundWind: 'E', limitMode: 'unlimited', rng: mulberry32(1) });
  engine.seats.E.concealed = ['C5', 'C5', 'C5', 'C5', 'D1', 'D2'];
  engine.wall = [{ code: 'B7' }, { code: 'B8' }];
  engine._maybeConcealedKong('E');

  const kongs = engine.seats.E.exposed.filter((m) => m.type === 'kong' && m.concealed);
  assert.equal(kongs.length, 1);
  assert.deepEqual(kongs[0].tiles, ['C5', 'C5', 'C5', 'C5']);
  // 6 concealed − 4 (kong) + 1 (replacement) = 3; wall dropped from 2 to 1.
  assert.equal(engine.seats.E.concealed.length, 3);
  assert.equal(engine.wall.length, 1);
  assert.ok(engine.events.some((e) => e.event_type === 'replacement_draw'), 'replacement draw logged');
});

test('kong on the last wall tile is not forced (no replacement exists, §4)', () => {
  const engine = new GameEngine({ dealerSeat: 'E', roundWind: 'E', limitMode: 'unlimited', rng: mulberry32(1) });
  engine.seats.E.concealed = ['C5', 'C5', 'C5', 'C5'];
  engine.wall = []; // empty
  engine._maybeConcealedKong('E');
  assert.equal(engine.seats.E.exposed.length, 0, 'no kong declared without a replacement tile');
});

test('exposed pung + drawn 4th tile upgrades to an exposed kong with a replacement draw', () => {
  const engine = new GameEngine({ dealerSeat: 'E', roundWind: 'E', limitMode: 'unlimited', rng: mulberry32(1) });
  // Player already exposed a C5 pung (claimed from South); now holds the 4th C5.
  engine.seats.E.exposed = [makeMeld('pung', ['C5', 'C5', 'C5'], { concealed: false, fromSeat: 'S' })];
  engine.seats.E.concealed = ['C5', 'D1', 'D2'];
  engine.wall = [{ code: 'B7' }, { code: 'B8' }];
  engine._maybeExposedKongUpgrade('E');

  assert.equal(engine.seats.E.exposed.length, 1);
  const meld = engine.seats.E.exposed[0];
  assert.equal(meld.type, 'kong');
  assert.equal(meld.concealed, false, 'upgraded kong is exposed');
  assert.equal(meld.fromSeat, 'S', 'origin seat preserved from the original pung');
  assert.deepEqual(meld.tiles, ['C5', 'C5', 'C5', 'C5']);
  // 3 concealed − 1 (the 4th C5) + 1 (replacement) = 3; wall dropped from 2 to 1.
  assert.equal(engine.seats.E.concealed.length, 3);
  assert.equal(engine.wall.length, 1);
  assert.ok(engine.events.some((e) => e.event_type === 'kong_upgrade'), 'kong upgrade logged');
});

test('exposed kong upgrade is skipped when the wall is empty (§4, no replacement)', () => {
  const engine = new GameEngine({ dealerSeat: 'E', roundWind: 'E', limitMode: 'unlimited', rng: mulberry32(1) });
  engine.seats.E.exposed = [makeMeld('pung', ['C5', 'C5', 'C5'], { concealed: false, fromSeat: 'S' })];
  engine.seats.E.concealed = ['C5'];
  engine.wall = [];
  engine._maybeExposedKongUpgrade('E');
  assert.equal(engine.seats.E.exposed[0].type, 'pung', 'no upgrade without a replacement tile');
  assert.deepEqual(engine.seats.E.concealed, ['C5'], 'the 4th tile is retained');
});

test('async run(): declaring Mahjong on a non-winning hand is a false Mahjong (server-enforced)', async () => {
  const engine = new GameEngine({
    dealerSeat: 'E', roundWind: 'E', limitMode: 'limited', pointsLimit: 1000, moneyLimit: 10,
    rng: mulberry32(1),
  });
  // East is a rogue client that declares Mahjong on its very first turn regardless
  // of hand legality; S/W/N pass all claims. The engine must NOT trust East.
  const rogue = {
    async decideTurn() { return { action: 'mahjong' }; },
    async decideClaim() { return { claim: 'pass' }; },
  };
  const pass = {
    async decideTurn(view) { return { action: 'discard', tile: view.concealed[view.concealed.length - 1] }; },
    async decideClaim() { return { claim: 'pass' }; },
  };
  const controllers = { E: rogue, S: pass, W: pass, N: pass };
  const balances = { E: 1000, S: 1000, W: 1000, N: 1000 };

  const r = await engine.run(controllers, balances, () => {});
  assert.equal(r.resultType, 'false_mahjong');
  assert.equal(r.offenderSeat, 'E');
  assert.equal(r.penaltyPerOpponent, 10);          // (1000/1000)*10
  assert.ok(r.moneyDeltas.E < 0, 'offender loses money');
  // Deltas fold in immediate flower payments from the deal, so assert conservation
  // (both the penalty and the flower pays are zero-sum) rather than an exact figure.
  assert.equal(SEATS.reduce((sum, s) => sum + (r.moneyDeltas[s] || 0), 0), 0, 'money conserved');
  assert.ok(r.hands && Object.keys(r.hands).length === 4, 'false mahjong still reveals hands');
});

test('_falseMahjong applies the exact limit penalty to each opponent (no flowers)', () => {
  const engine = new GameEngine({
    dealerSeat: 'E', roundWind: 'E', limitMode: 'limited', pointsLimit: 1000, moneyLimit: 10,
    rng: mulberry32(1),
  });
  // No deal → flowerPayments are all 0, isolating the penalty deltas.
  const r = engine._falseMahjong('E', { E: 1000, S: 1000, W: 1000, N: 1000 });
  assert.equal(r.resultType, 'false_mahjong');
  assert.equal(r.penaltyPerOpponent, 10);
  assert.equal(r.moneyDeltas.E, -30);              // pays $10 to each of 3 opponents
  assert.equal(r.moneyDeltas.S, 10);
  assert.equal(r.moneyDeltas.W, 10);
  assert.equal(r.moneyDeltas.N, 10);
});

test('async run() matches the sync path invariants (all-AI, 120 hands)', async () => {
  let wins = 0, draws = 0, events = 0;
  for (let seed = 1; seed <= 120; seed++) {
    const rng = mulberry32(seed * 7 + 3);
    const engine = new GameEngine({
      dealerSeat: 'E', roundWind: 'E', limitMode: 'limited', pointsLimit: 1000, moneyLimit: 10, rng,
    });
    // Async AI controllers implement decideTurn/decideClaim; resolve instantly.
    const controllers = {};
    for (const s of SEATS) controllers[s] = new AIController(s, { aggression: 0.5 });
    const balances = { E: 1000, S: 1000, W: 1000, N: 1000 };

    const r = await engine.run(controllers, balances, () => { events++; });
    assert.ok(r.resultType === 'win' || r.resultType === 'draw');
    const total = SEATS.reduce((sum, s) => sum + (r.moneyDeltas[s] || 0), 0);
    assert.equal(total, 0, `seed ${seed}: async money not conserved`);
    assert.ok(r.hands && Object.keys(r.hands).length === 4, 'settlement reveals all hands');
    for (const s of SEATS) {
      const st = engine.seats[s];
      const expected = (s === r.winnerSeat ? 14 : 13) - 3 * st.exposed.length;
      assert.equal(st.concealed.length, expected, `seed ${seed} seat ${s}: async illegal count`);
    }
    if (r.resultType === 'win') { wins++; assert.ok(r.final_points <= 1000); } else draws++;
  }
  assert.equal(wins + draws, 120);
  assert.ok(wins > 0 && events > 0, 'async run should produce wins and stream events');
});
