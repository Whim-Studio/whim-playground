// =============================================================================
// engine/GameEngine.js — authoritative engine for ONE hand. Seat-agnostic: it
// never assumes which seats are AI vs human. `playHand(brains)` runs a full legal
// hand headlessly (draw → flowers/replacement → self-draw win / concealed kong →
// discard → claims → win/draw → score → settle). `brains` maps seat -> an object
// with sync helpers { chooseDiscard, claimsMahjong, claimsPung } (AIController
// implements these). The real-time UI path uses the same engine but feeds
// decisions through the async SeatController layer.
// =============================================================================

import { CLAIM_TIMEOUT_MS } from '../domain/constants.js';
import { fullTileSet, shuffle, isFlowerOrSeason, isHonor } from '../domain/tiles.js';
import { SEATS, nextSeat, makeMeld, claimDistance } from '../domain/melds.js';
import { resolveClaims } from './ClaimResolver.js';
import { scoreHand, sumBasePoints, meldPoints } from './ScoringEngine.js';
import { decompose, detectStructuralDoubles, isThirteenOrphans, isAllFlowersSeasons, isWinningHand } from './HandAnalyzer.js';
import { moneyForPoints, settleWin, settleFalseMahjong, falseMahjongPenaltyPerOpponent } from './Settlement.js';

export const Phase = Object.freeze({
  DEAL: 'deal', PLAY: 'play', SCORE: 'score', DRAWN: 'drawn', DONE: 'done',
});

export class GameEngine {
  /** @param {import('./GameEngine.js').HandConfig} config */
  constructor(config) {
    this.config = { rng: Math.random, ...config };
    this.claimTimeoutMs = CLAIM_TIMEOUT_MS;
    this.seq = 0;
    this.events = [];
    this.discards = [];
    this.wall = [];
    this.phase = Phase.DEAL;
    this.seats = {};
    for (const s of SEATS) this.seats[s] = { concealed: [], exposed: [], flowers: [], everClaimed: false };
    this.flowerPayments = { E: 0, S: 0, W: 0, N: 0 }; // net $ from immediate flower pays
    this.firstDiscardHappened = false;
  }

  _log(seat, type, tile = null, meta = null) {
    this.events.push({ seq: ++this.seq, seat, event_type: type, tile_code: tile, meta });
  }

  /**
   * Draw the next tile, auto-revealing flowers/seasons (each pulls a further
   * replacement + pays $2). `drawLabel` tags the event ('draw' for a normal turn
   * draw, 'replacement_draw' for a kong/flower replacement) for the §8 log.
   */
  _drawWithFlowers(seat, drawLabel = 'draw') {
    while (this.wall.length) {
      const tile = this.wall.shift();
      if (isFlowerOrSeason(tile.code)) {
        this.seats[seat].flowers.push(tile.code);
        this._log(seat, 'flower', tile.code);
        this._payFlower(seat);                 // immediate, cannot be deferred (§4)
        drawLabel = 'replacement_draw';        // the follow-on tile is a replacement
        continue;
      }
      this.seats[seat].concealed.push(tile.code);
      this._log(seat, drawLabel, tile.code);
      return tile.code;
    }
    return null;                               // wall exhausted
  }

  _payFlower(revealer) {
    for (const s of SEATS) {
      if (s === revealer) this.flowerPayments[s] += 6;   // receives $2 from each of 3
      else this.flowerPayments[s] -= 2;
    }
  }

  /** Deal 13 to each seat (dealer takes the first turn's draw), resolve flowers. */
  deal() {
    this.wall = shuffle(fullTileSet(), this.config.rng);
    for (let i = 0; i < 13; i++) for (const s of SEATS) {
      // draw a normal tile, resolving flowers inline
      let t;
      do { t = this.wall.shift(); if (t && isFlowerOrSeason(t.code)) { this.seats[s].flowers.push(t.code); this._payFlower(s); } }
      while (t && isFlowerOrSeason(t.code));
      if (t) this.seats[s].concealed.push(t.code);
    }
    for (const s of SEATS) this.seats[s].concealed.sort();
    this.phase = Phase.PLAY;
  }

  /**
   * Run the hand to completion.
   * @param {Record<string,{chooseDiscard:Function,claimsMahjong:Function,claimsPung:Function}>} brains
   * @param {{E:number,S:number,W:number,N:number}} balances current money per seat
   * @returns {HandResult}
   */
  playHand(brains, balances) {
    if (this.phase === Phase.DEAL) this.deal();
    let active = this.config.dealerSeat;
    let firstGoAround = true;
    let skipDraw = false;   // true when a seat took the turn via a pung claim

    while (true) {
      const seat = active;
      const st = this.seats[seat];

      // 1) Draw — UNLESS this seat took the turn by claiming a pung (it used the
      //    discard as its draw and only needs to discard).
      if (!skipDraw) {
        const drawn = this._drawWithFlowers(seat);
        if (drawn === null) return this._drawnGame(balances);
        st.concealed.sort();

        // 2) Concealed kong (self-draw four of a kind) — declare + replacement.
        this._maybeConcealedKong(seat);
        // 2b) Late kong: promote an exposed pung whose 4th tile was just drawn.
        this._maybeExposedKongUpgrade(seat);

        // 3) Self-draw win?
        if (this._isWin(seat)) {
          const timing = this.wall.length === 0 ? 'last_wall_tile'
            : (firstGoAround && seat === this.config.dealerSeat ? 'first_tile' : 'normal');
          return this._win(seat, { selfDraw: true, timing, firstGoAround, balances });
        }
      }
      skipDraw = false;

      // 4) Discard.
      const tile = brains[seat].chooseDiscard(st.concealed);
      st.concealed.splice(st.concealed.indexOf(tile), 1);
      this.discards.push({ seat, tile });
      this._log(seat, 'discard', tile);
      this.firstDiscardHappened = true;

      // 5) Claims on the discard (Mahjong > Pung; kong-from-discard illegal).
      const outcome = this._resolveDiscardClaims(seat, tile, brains, balances, firstGoAround);
      if (outcome.win) return outcome.win;
      if (outcome.pungSeat) {
        // The claimer exposed the pung and now becomes active WITHOUT drawing;
        // next loop iteration they discard. Play then continues from after them.
        // Any claim disrupts the opening lap (first-tile hands are already gone).
        active = outcome.pungSeat;
        skipDraw = true;
        firstGoAround = false;
        continue;
      }

      // 6) Next seat; a full lap past the dealer ends the "first go-around".
      active = nextSeat(active);
      if (active === this.config.dealerSeat) firstGoAround = false;
    }
  }

  /**
   * A redacted snapshot for ONE seat: its own tiles, everyone's exposed melds,
   * the discard pile, opponents' concealed COUNTS only (never their tiles), and
   * whose turn it is. Safe to send to that seat's client — the anti-peek boundary.
   */
  viewFor(seat) {
    const st = this.seats[seat];
    const opponents = SEATS.filter((s) => s !== seat).map((s) => ({
      seat: s, name: null, concealedCount: this.seats[s].concealed.length,
      melds: this.seats[s].exposed.map(serializeMeld), flowers: [...this.seats[s].flowers],
    }));
    return {
      seat,
      concealed: [...st.concealed].sort(),
      exposed: st.exposed.map(serializeMeld),
      flowers: [...st.flowers],
      canWin: this._isWin(seat),
      discardPile: this.discards.map((d) => ({ seat: d.seat, tile: d.tile })),
      opponents, wallRemaining: this.wall.length, turn: this.activeSeat ?? null,
    };
  }

  /**
   * Interactive async variant of playHand. `controllers[seat]` implements the
   * async SeatController contract (decideTurn/decideClaim). `emit(type, payload)`
   * streams state to a transport (e.g. WebSocket). Reuses every rules helper so it
   * stays in lockstep with the tested sync path. AI controllers resolve instantly;
   * a human controller resolves when its move arrives (or its deadline passes).
   * @returns {Promise<HandResult>}
   */
  async run(controllers, balances, emit = () => {}) {
    if (this.phase === Phase.DEAL) this.deal();
    let active = this.config.dealerSeat;
    let firstGoAround = true;
    let skipDraw = false;

    while (true) {
      this.activeSeat = active;
      const seat = active;
      const st = this.seats[seat];

      if (!skipDraw) {
        const drawn = this._drawWithFlowers(seat);
        if (drawn === null) return this._emitReturn(emit, this._drawnGame(balances));
        st.concealed.sort();
        this._maybeConcealedKong(seat);            // v1: auto-declares for all seats
        this._maybeExposedKongUpgrade(seat);       // late kong: exposed pung + drawn 4th
        if (this._isWin(seat)) {
          const timing = this.wall.length === 0 ? 'last_wall_tile'
            : (firstGoAround && seat === this.config.dealerSeat ? 'first_tile' : 'normal');
          return this._emitReturn(emit, this._win(seat, { selfDraw: true, timing, firstGoAround, balances }));
        }
      }
      skipDraw = false;

      // Active seat's own decision (discard, or declare mahjong on a legal hand).
      emit('turn', { seat, view: this.viewFor(seat) });
      const action = await controllers[seat].decideTurn(this.viewFor(seat));
      if (action.action === 'mahjong') {
        if (this._isWin(seat)) {
          const timing = this.wall.length === 0 ? 'last_wall_tile' : 'normal';
          return this._emitReturn(emit, this._win(seat, { selfDraw: true, timing, firstGoAround, balances }));
        }
        // §8: declaring Mahjong on a non-winning hand ends the round with a penalty.
        return this._emitReturn(emit, this._falseMahjong(seat, balances));
      }
      let tile = action.tile;
      if (!tile || !st.concealed.includes(tile)) tile = st.concealed[st.concealed.length - 1]; // safe fallback
      st.concealed.splice(st.concealed.indexOf(tile), 1);
      this.discards.push({ seat, tile });
      this._log(seat, 'discard', tile);
      this.firstDiscardHappened = true;
      emit('discard', { seat, tile });

      // Concurrent claim window: ask every eligible non-active seat at once.
      const others = SEATS.filter((s) => s !== seat);
      const decisions = await Promise.all(others.map(async (s) => {
        const st2 = this.seats[s];
        const canMj = isWinningHand([...st2.concealed, tile], st2.exposed);
        const canPung = st2.concealed.filter((t) => t === tile).length >= 2;
        if (!canMj && !canPung) return { seat: s, claim: 'pass' };
        emit('claim_request', { seat: s, tile, canPung, canMahjong: canMj });
        const d = await controllers[s].decideClaim(this.viewFor(s), tile, { canPung, canMahjong: canMj });
        return { seat: s, claim: d.claim };
      }));

      const claims = [];
      let falseClaimer = null;   // nearest-CCW seat that claimed Mahjong illegally
      for (const d of decisions) {
        const st2 = this.seats[d.seat];
        if (d.claim === 'mahjong') {
          if (isWinningHand([...st2.concealed, tile], st2.exposed)) {
            claims.push({ seat: d.seat, type: 'mahjong', legal: true });
          } else if (falseClaimer == null || claimDistance(seat, d.seat) < claimDistance(seat, falseClaimer)) {
            falseClaimer = d.seat;   // §8 false Mahjong on a discard
          }
        } else if (d.claim === 'pung' && st2.concealed.filter((t) => t === tile).length >= 2) {
          claims.push({ seat: d.seat, type: 'pung', legal: true });
        }
      }
      // A genuine winner takes precedence; otherwise an illegal Mahjong claim is a
      // false declaration and ends the round with a penalty (client never trusted).
      if (falseClaimer != null && !claims.some((c) => c.type === 'mahjong')) {
        return this._emitReturn(emit, this._falseMahjong(falseClaimer, balances));
      }
      const won = resolveClaims(claims, seat);
      if (won) {
        const st2 = this.seats[won.seat];
        st2.everClaimed = true;
        if (won.type === 'mahjong') {
          st2.concealed.push(tile);
          this._log(won.seat, 'mahjong', tile, { from: seat });
          const timing = this.wall.length === 0 ? 'final_discard' : 'normal';
          return this._emitReturn(emit, this._win(won.seat, { selfDraw: false, timing, firstGoAround, balances, winningTile: tile }));
        }
        this.discards.pop();
        for (let i = 0; i < 2; i++) st2.concealed.splice(st2.concealed.indexOf(tile), 1);
        st2.exposed.push(makeMeld('pung', [tile, tile, tile], { concealed: false, fromSeat: seat }));
        this._log(won.seat, 'claim_pung', tile, { from: seat });
        emit('claim', { seat: won.seat, type: 'pung', tile, from: seat });
        active = won.seat; skipDraw = true; firstGoAround = false;
        continue;
      }

      active = nextSeat(active);
      if (active === this.config.dealerSeat) firstGoAround = false;
    }
  }

  _emitReturn(emit, result) { this.activeSeat = null; emit('settlement', result); return result; }

  /**
   * Auto-declare any concealed kong (four of a kind in hand). Each kong MUST pull
   * a replacement tile for the 4th tile consumed (§4); that replacement can itself
   * complete another kong, so we loop until stable. If the wall is empty there is
   * no replacement — per §4 the kong on the last tile can't be completed, so we
   * stop declaring (the hand plays out to a drawn game). Returns true if the
   * replacement draw exhausted the wall (caller should end the hand as a draw).
   */
  _maybeConcealedKong(seat) {
    const st = this.seats[seat];
    let declared = true;
    while (declared) {
      declared = false;
      const counts = {};
      for (const t of st.concealed) counts[t] = (counts[t] || 0) + 1;
      const code = Object.keys(counts).find((c) => counts[c] === 4);
      if (!code) break;
      if (this.wall.length === 0) break;       // kong on last tile: no replacement (§4)
      for (let i = 0; i < 4; i++) st.concealed.splice(st.concealed.indexOf(code), 1);
      st.exposed.push(makeMeld('kong', [code, code, code, code], { concealed: true }));
      this._log(seat, 'conceal_kong', code);
      this._drawWithFlowers(seat, 'replacement_draw'); // MUST replace the 4th tile
      st.concealed.sort();
      declared = true;                         // re-scan: the replacement may kong again
    }
  }

  /**
   * Late kong / "exposed kong upgrade" (§4): on the player's OWN turn, an already
   * exposed pung whose 4th tile is now in hand may be promoted to an exposed kong,
   * followed by the mandatory replacement draw. Auto-declared for all seats (same
   * v1 simplification as concealed kongs). Chains, since a replacement draw can
   * complete another. Skipped when the wall is empty (no replacement exists, §4).
   */
  _maybeExposedKongUpgrade(seat) {
    const st = this.seats[seat];
    let upgraded = true;
    while (upgraded) {
      upgraded = false;
      for (let i = 0; i < st.exposed.length; i++) {
        const m = st.exposed[i];
        if (m.type !== 'pung' || m.concealed) continue;
        const code = m.tiles[0];
        if (!st.concealed.includes(code)) continue;
        if (this.wall.length === 0) return;    // no replacement tile available (§4)
        st.concealed.splice(st.concealed.indexOf(code), 1);   // consume the 4th tile
        st.exposed[i] = makeMeld('kong', [code, code, code, code],
          { concealed: false, fromSeat: m.fromSeat });
        this._log(seat, 'kong_upgrade', code);
        this._drawWithFlowers(seat, 'replacement_draw');      // MUST replace it
        st.concealed.sort();
        upgraded = true;                       // re-scan: the replacement may enable another
        break;
      }
    }
  }

  /**
   * Resolve claims on the current discard.
   * @returns {{win?:Object, pungSeat?:string}} `win` if a Mahjong claim wins the
   *   hand; `pungSeat` if a Pung was claimed (meld already exposed, discard taken);
   *   empty object if nobody claimed.
   */
  _resolveDiscardClaims(discarder, tile, brains, balances, firstGoAround) {
    const claims = [];
    for (const s of SEATS) {
      if (s === discarder) continue;
      const st = this.seats[s];
      if (brains[s].claimsMahjong(st.concealed, st.exposed, tile)) claims.push({ seat: s, type: 'mahjong', legal: true });
      else if (brains[s].claimsPung(st.concealed, st.exposed, tile)) claims.push({ seat: s, type: 'pung', legal: true });
    }
    const won = resolveClaims(claims, discarder);   // Mahjong > Pung; nearest-CCW tiebreak
    if (!won) return {};

    const st = this.seats[won.seat];
    st.everClaimed = true;                          // claiming a discard forfeits fully-concealed

    if (won.type === 'mahjong') {
      st.concealed.push(tile);
      this._log(won.seat, 'mahjong', tile, { from: discarder });
      const timing = this.wall.length === 0 ? 'final_discard'
        : (firstGoAround && won.seat !== this.config.dealerSeat && this.discards.length === 1 ? 'first_tile' : 'normal');
      return { win: this._win(won.seat, { selfDraw: false, timing, firstGoAround, balances, winningTile: tile }) };
    }

    // Pung: take the discard off the pile, expose the meld (discard + two held).
    this.discards.pop();
    for (let i = 0; i < 2; i++) st.concealed.splice(st.concealed.indexOf(tile), 1);
    st.exposed.push(makeMeld('pung', [tile, tile, tile], { concealed: false, fromSeat: discarder }));
    this._log(won.seat, 'claim_pung', tile, { from: discarder });
    return { pungSeat: won.seat };
  }

  _isWin(seat) {
    const st = this.seats[seat];
    return decompose(st.concealed, st.exposed).length > 0 || (st.exposed.length === 0 && isThirteenOrphans(st.concealed));
  }

  // --- scoring / settlement -------------------------------------------------
  _win(seat, { selfDraw, timing, firstGoAround, balances, winningTile = null }) {
    const st = this.seats[seat];
    const limit = this.config.pointsLimit ?? null;

    // Special limit hands.
    let specialLimitHand = null;
    if (st.exposed.length === 0 && isThirteenOrphans(st.concealed)) specialLimitHand = '13_orphans';
    else if (isAllFlowersSeasons(st.flowers)) specialLimitHand = 'all_flowers_seasons';

    // Pick the highest-scoring legal decomposition. On a WIN BY DISCARD the meld
    // the claimed tile completes is EXPOSED, not concealed (§ definitions), which
    // lowers its base points; applyDiscardExposure attributes the winning tile to
    // a chow/pair when possible (0 penalty) and only exposes a pung when forced.
    const decomps = decompose(st.concealed, st.exposed);
    let bestMelds = decomps[0] || st.exposed;
    let bestFlags = [];
    let bestBase = -1;
    for (const d0 of decomps) {
      const d = (!selfDraw && winningTile) ? applyDiscardExposure(d0, winningTile) : d0;
      const flags = detectStructuralDoubles(d);
      const base = sumBasePoints(d);
      if (base > bestBase) { bestBase = base; bestMelds = d; bestFlags = flags; }
    }

    const doubleFlags = [...bestFlags];
    if (selfDraw && !st.everClaimed) doubleFlags.push('FULLY_CONCEALED'); // == ALL_CONCEALED (single 2-double)
    if (timing === 'last_wall_tile') doubleFlags.push('LAST_WALL_TILE');
    if (timing === 'final_discard') doubleFlags.push('FINAL_DISCARD');

    // First-tile hands (13-double variants); resolution vs flat-1000 in scorer.
    let firstTileWinType = null;
    if (timing === 'first_tile') {
      if (seat === this.config.dealerSeat && selfDraw) { firstTileWinType = 'heavenly'; doubleFlags.push('HEAVENLY_HAND'); }
      else if (!selfDraw) { firstTileWinType = 'earthly'; doubleFlags.push('EARTHLY_HAND'); }
      else { firstTileWinType = 'human'; doubleFlags.push('HUMAN_HAND'); }
    }

    const breakdown = scoreHand({
      melds: bestMelds, flowerCount: st.flowers.length, pointsLimit: limit,
      doubleFlags: dedupe(doubleFlags), specialLimitHand, firstTileWinType,
    });

    const payout = moneyForPoints(breakdown.final_points, limit, this.config.moneyLimit ?? null);
    const bal = SEATS.map((s) => ({ seat: s, money: balances[s] }));
    const { deltas, debts, bankrupt } = settleWin(bal, seat, payout);

    // Per-meld point breakdown for the settlement panel (§6 base table).
    const scoredMelds = bestMelds.map((m) => ({
      type: m.type, tiles: m.tiles, concealed: m.concealed, points: meldPoints(m),
    }));
    const scoring = {
      melds: scoredMelds,
      basePoints: breakdown.base_points,
      flowerCount: st.flowers.length, flowerPoints: breakdown.flower_points,
      mahjongBonus: breakdown.mahjong_bonus,
      subtotal: breakdown.subtotal,
      appliedDoubles: breakdown.applied_doubles,
      doublesCount: breakdown.doubles_count, multiplier: breakdown.multiplier,
      preCap: breakdown.pre_cap, finalPoints: breakdown.final_points,
      isLimitHand: breakdown.is_limit_hand, specialHand: breakdown.special_hand,
      pointsLimit: limit, moneyLimit: this.config.moneyLimit ?? null,
      moneyPerOpponent: payout, selfDraw, timing,
    };

    this.phase = Phase.DONE;
    return {
      resultType: 'win', winnerSeat: seat, selfDraw, timing, ...breakdown,
      wallRemaining: this.wall.length, fullyConcealed: selfDraw && !st.everClaimed ? 1 : 0,
      payout, moneyDeltas: applyFlowers(deltas, this.flowerPayments), debts,
      bankrupt, flowerPayments: this.flowerPayments, events: this.events,
      hands: this._revealHands(), winningMelds: bestMelds.map(serializeMeld), scoring,
    };
  }

  _drawnGame(balances) {
    this.phase = Phase.DRAWN;
    const deltas = { E: 0, S: 0, W: 0, N: 0 };
    return {
      resultType: 'draw', winnerSeat: null, final_points: 0,
      wallRemaining: 0, moneyDeltas: applyFlowers(deltas, this.flowerPayments),
      debts: { E: 0, S: 0, W: 0, N: 0 }, bankrupt: SEATS.some((s) => balances[s] + this.flowerPayments[s] <= 0),
      flowerPayments: this.flowerPayments, events: this.events,
      hands: this._revealHands(),
    };
  }

  /**
   * §8 False Mahjong: a seat declared/claimed Mahjong on a hand that is NOT a
   * legal win. The round ends immediately and the offender pays the limit penalty
   * to EACH other player (Unlimited: $1000 each). Partial-payment + bankruptcy
   * rules apply. The client is never trusted — this is enforced server-side.
   */
  _falseMahjong(offenderSeat, balances) {
    this.phase = Phase.DONE;
    this._log(offenderSeat, 'penalty', null, { reason: 'false_mahjong' });
    const penalty = falseMahjongPenaltyPerOpponent(
      this.config.pointsLimit ?? null, this.config.moneyLimit ?? null);
    const bal = SEATS.map((s) => ({ seat: s, money: balances[s] }));
    const { deltas, debts, bankrupt } = settleFalseMahjong(bal, offenderSeat, penalty);
    return {
      resultType: 'false_mahjong', winnerSeat: null, offenderSeat, final_points: 0,
      penaltyPerOpponent: penalty, wallRemaining: this.wall.length,
      moneyDeltas: applyFlowers(deltas, this.flowerPayments), debts, bankrupt,
      flowerPayments: this.flowerPayments, events: this.events,
      hands: this._revealHands(),
    };
  }

  /** Snapshot all four hands for the settlement reveal (§8 shows everyone). */
  _revealHands() {
    const out = {};
    for (const s of SEATS) {
      const st = this.seats[s];
      out[s] = {
        concealed: [...st.concealed].sort(),
        melds: st.exposed.map(serializeMeld),
        flowers: [...st.flowers],
      };
    }
    return out;
  }
}

function serializeMeld(m) { return { type: m.type, tiles: m.tiles, concealed: m.concealed }; }

/**
 * On a win by discard, the meld the claimed tile completes is exposed. Chows and
 * pairs score 0 whether exposed or concealed, so if the winning tile can be
 * attributed to a chow or the pair we expose nothing (the winner's best case).
 * Otherwise the single pung it completes is marked exposed (lower base points).
 */
function applyDiscardExposure(decomp, winningTile) {
  const attributableToZeroScorer = decomp.some(
    (m) => (m.type === 'chow' || m.type === 'pair') && m.tiles.includes(winningTile));
  if (attributableToZeroScorer) return decomp;
  let exposedOne = false;
  return decomp.map((m) => {
    if (!exposedOne && m.type === 'pung' && m.concealed && m.tiles.includes(winningTile)) {
      exposedOne = true;
      return makeMeld('pung', m.tiles, { concealed: false, fromSeat: 'discard' });
    }
    return m;
  });
}

function dedupe(a) { return [...new Set(a)]; }

/** Fold the immediate flower $2 payments into the settlement deltas. */
function applyFlowers(deltas, flowers) {
  const out = {};
  for (const s of SEATS) out[s] = (deltas[s] || 0) + (flowers[s] || 0);
  return out;
}
