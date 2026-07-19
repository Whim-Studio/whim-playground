// =============================================================================
// realtime/LiveGame.js — one interactive table bound to one WebSocket. The human
// fills seat East; S/W/N are AI. Holds session state (balances, dealer/wind,
// bankruptcy) across hands and drives each hand via GameEngine.run, streaming
// state to the client and routing the human's moves into HumanController.
//
// This is the seat-agnostic session a future multiplayer store would persist and
// replicate: filling S/W/N with remote HumanControllers instead of AIControllers
// is the only change needed to seat more humans.
// =============================================================================

import { GameEngine } from '../engine/GameEngine.js';
import { AIController } from '../players/AIController.js';
import { HumanController } from '../players/HumanController.js';
import { SEATS } from '../domain/melds.js';
import { STARTING_MONEY } from '../domain/constants.js';
import { validateRoundConfig } from '../api/validation.js';
import { StatsRecorder } from '../persistence/StatsRecorder.js';

/**
 * Compute the table's opening balances. A returning player (persisted profile)
 * carries their lifetime bankroll into seat East; the three AI opponents always
 * start from a fresh buy-in so they can't go permanently bankrupt across sessions.
 * `startingMoney == null` (offline guest / no DB) => everyone gets a fresh buy-in,
 * so the guest path is byte-for-byte unchanged.
 * @param {?number} startingMoney East's persisted balance, or null for a guest.
 * @returns {{E:number,S:number,W:number,N:number}}
 */
export function startingBalances(startingMoney) {
  const east = (startingMoney != null && Number.isFinite(startingMoney))
    ? startingMoney : STARTING_MONEY;
  return { E: east, S: STARTING_MONEY, W: STARTING_MONEY, N: STARTING_MONEY };
}

export class LiveGame {
  constructor(send, { humanName = 'You' } = {}) {
    this.send = send;                      // (type, payload) => void
    this.names = { E: humanName, S: 'AI South', W: 'AI West', N: 'AI North' };
    this.balances = { E: STARTING_MONEY, S: STARTING_MONEY, W: STARTING_MONEY, N: STARTING_MONEY };
    this.dealerSeat = 'E'; this.roundWind = 'E'; this.handNumber = 0; this.ended = false;
    this.human = new HumanController('E');
    this.controllers = {
      E: this.human,
      S: new AIController('S', { aggression: 0.5 }),
      W: new AIController('W', { aggression: 0.5 }),
      N: new AIController('N', { aggression: 0.5 }),
    };
    this.engine = null;
    this.inHand = false;
    this.recorder = new StatsRecorder();   // best-effort; disables itself with no DB
    this.humanPlayerId = null;             // set from hello; null => offline guest
    this._handCtx = null;                  // pre-rotation dealer/wind/cfg for recording
  }

  /** Route an inbound client message. */
  handle(msg) {
    switch (msg?.type) {
      case 'hello':
        if (msg.playerName) this.names.E = msg.playerName;
        if (msg.playerId) this.humanPlayerId = msg.playerId;
        this._pushLobby();
        break;
      case 'deal': this._deal(msg.config); break;
      case 'move': this.human.submit(msg); break;      // resolve the parked decision
      default: this.send('error', { message: `unknown message ${msg?.type}` });
    }
  }

  _pushLobby() {
    this.send('lobby', { names: this.names, balances: this.balances, ended: this.ended,
      dealerSeat: this.dealerSeat, roundWind: this.roundWind });
  }

  async _deal(config) {
    if (this.ended) return this.send('error', { message: 'game over — a player is broke' });
    if (this.inHand) return this.send('error', { message: 'hand already in progress' });
    const cfg = validateRoundConfig(config || {});
    if (!cfg.ok) return this.send('error', { message: cfg.errors.join(', ') });

    this.inHand = true;
    // Open the game row + seat profiles on the first hand (best-effort, no-op with
    // no DB). Capture this hand's dealer/wind BEFORE settlement rotates them.
    const initInfo = await this.recorder.init(this.humanPlayerId);
    // Bankroll carry-over: on the very first hand of a persisted profile, load the
    // player's lifetime balance as East's starting stack (AI stay a fresh buy-in).
    // Guest / no-DB leaves the fresh $1000 buy-in untouched.
    if (this.handNumber === 0 && initInfo && initInfo.startingMoney != null) {
      this.balances = startingBalances(initInfo.startingMoney);
      if (this.balances.E <= 0) {
        this.ended = true; this.inHand = false;
        return this.send('error', { message: 'your profile is out of funds — cannot start a hand' });
      }
    }
    this.handNumber++;
    this._handCtx = { handNumber: this.handNumber, dealerSeat: this.dealerSeat,
      roundWind: this.roundWind, cfg: cfg.value };
    this.engine = new GameEngine({
      dealerSeat: this.dealerSeat, roundWind: this.roundWind, rng: Math.random, ...cfg.value,
    });

    const emit = (type, payload) => this._onEngineEvent(type, payload, cfg.value);
    try {
      const result = await this.engine.run(this.controllers, this.balances, emit);
      this._settle(result);
    } catch (err) {
      this.send('error', { message: 'engine error: ' + err.message });
    } finally {
      this.inHand = false;
    }
  }

  _onEngineEvent(type, payload) {
    // Always refresh the human's redacted view so the client re-renders live.
    if (this.engine) this.send('state', {
      view: this.engine.viewFor('E'), names: this.names, balances: this.balances,
      handNumber: this.handNumber, dealerSeat: this.dealerSeat, roundWind: this.roundWind,
    });
    if (type === 'turn') this.send('turn', { seat: payload.seat, yourTurn: payload.seat === 'E' });
    else if (type === 'discard') this.send('discard', { seat: payload.seat, tile: payload.tile });
    else if (type === 'claim') this.send('claim', payload);
    else if (type === 'claim_request' && payload.seat === 'E') {
      this.send('claim_request', { tile: payload.tile, canPung: payload.canPung, canMahjong: payload.canMahjong });
    }
    // 'settlement' is handled by _settle after run() resolves.
  }

  _settle(result) {
    for (const s of SEATS) this.balances[s] += (result.moneyDeltas[s] || 0);

    // Dealer keeps the seat only by winning (§2); otherwise rotate; wind rolls on E.
    if (!(result.resultType === 'win' && result.winnerSeat === this.dealerSeat)) {
      this.dealerSeat = SEATS[(SEATS.indexOf(this.dealerSeat) + 1) % 4];
      if (this.dealerSeat === 'E') this.roundWind = SEATS[(SEATS.indexOf(this.roundWind) + 1) % 4];
    }
    if (result.bankrupt || SEATS.some((s) => this.balances[s] <= 0)) this.ended = true;

    this.send('settlement', {
      handNumber: this.handNumber,
      resultType: result.resultType,
      winnerSeat: result.winnerSeat ?? null,
      winnerName: result.winnerSeat ? this.names[result.winnerSeat] : null,
      offenderSeat: result.offenderSeat ?? null,
      offenderName: result.offenderSeat ? this.names[result.offenderSeat] : null,
      penaltyPerOpponent: result.penaltyPerOpponent ?? 0,
      finalPoints: result.final_points ?? 0,
      isLimitHand: result.is_limit_hand ?? 0,
      specialHand: result.special_hand ?? null,
      fullyConcealed: result.fullyConcealed ?? 0,
      doublesCount: result.doubles_count ?? 0,
      timing: result.timing ?? null,
      moneyDeltas: result.moneyDeltas,
      balances: { ...this.balances },
      names: this.names,
      hands: result.hands,
      scoring: result.scoring ?? null,   // itemized winning-hand math
      ended: this.ended,
    });

    // Persist the hand + profile deltas (best-effort; swallows all DB errors and
    // never blocks the client). Uses the pre-rotation dealer/wind captured at deal.
    const ctx = this._handCtx;
    if (ctx) {
      void this.recorder
        .recordHand({ ...ctx, result, balancesAfter: { ...this.balances } })
        .then(() => { if (this.ended) return this.recorder.endGame('bankruptcy'); });
    }
  }
}
