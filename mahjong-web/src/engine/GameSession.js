// =============================================================================
// engine/GameSession.js — an in-memory table (4 seats + running balances) that
// plays successive hands via GameEngine. This is the seat-agnostic session state
// a future multiplayer store would persist/replicate. v1 keeps it in-process.
//
// NOTE: in v1 all four seats are driven by the AI brain for a self-running demo
// hand (the human's East seat included). Interactive human turns over REST/WS —
// suspending the engine mid-turn for a human decision — is the next step and is
// what the SeatController async layer exists for. Stats persistence is separate
// and MySQL-gated (see PlayerRepository); it is not required to play a hand.
// =============================================================================

import { GameEngine } from './GameEngine.js';
import { AIController } from '../players/AIController.js';
import { SEATS } from '../domain/melds.js';
import { STARTING_MONEY } from '../domain/constants.js';

export class GameSession {
  constructor({ humanName = 'You', rng = Math.random } = {}) {
    this.rng = rng;
    this.roundWind = 'E';
    this.dealerSeat = 'E';
    this.handNumber = 0;
    this.ended = false;
    this.endedReason = null;
    this.names = { E: humanName, S: 'AI South', W: 'AI West', N: 'AI North' };
    this.balances = { E: STARTING_MONEY, S: STARTING_MONEY, W: STARTING_MONEY, N: STARTING_MONEY };
    this.brains = {};
    for (const s of SEATS) this.brains[s] = new AIController(s);
  }

  /** Play one hand with the given round config; update balances + dealer/wind. */
  playHand({ limitMode, pointsLimit, moneyLimit }) {
    if (this.ended) throw new Error('game already ended');
    this.handNumber++;
    const engine = new GameEngine({
      dealerSeat: this.dealerSeat, roundWind: this.roundWind,
      limitMode, pointsLimit, moneyLimit, rng: this.rng,
    });
    const result = engine.playHand(this.brains, this.balances);

    for (const s of SEATS) this.balances[s] += (result.moneyDeltas[s] || 0);

    // Dealer keeps the seat only by winning (§2); otherwise rotate.
    if (!(result.resultType === 'win' && result.winnerSeat === this.dealerSeat)) {
      const order = SEATS;
      this.dealerSeat = order[(order.indexOf(this.dealerSeat) + 1) % 4];
      if (this.dealerSeat === 'E') this.roundWind = order[(order.indexOf(this.roundWind) + 1) % 4];
    }

    if (result.bankrupt || SEATS.some((s) => this.balances[s] <= 0)) {
      this.ended = true; this.endedReason = 'bankruptcy';
    }

    return {
      handNumber: this.handNumber,
      resultType: result.resultType,
      winnerSeat: result.winnerSeat ?? null,
      winnerName: result.winnerSeat ? this.names[result.winnerSeat] : null,
      finalPoints: result.final_points ?? 0,
      isLimitHand: result.is_limit_hand ?? 0,
      specialHand: result.special_hand ?? null,
      fullyConcealed: result.fullyConcealed ?? 0,
      doublesCount: result.doubles_count ?? 0,
      timing: result.timing ?? null,
      moneyDeltas: result.moneyDeltas,
      balances: { ...this.balances },
      names: this.names,
      ended: this.ended, endedReason: this.endedReason,
      discards: result.events?.filter((e) => e.event_type === 'discard').map((e) => ({ seat: e.seat, tile: e.tile_code })) ?? [],
      hands: result.hands,            // all four hands revealed at settlement
      winningMelds: result.winningMelds ?? null,
      scoring: result.scoring ?? null, // itemized winning-hand math
    };
  }
}
