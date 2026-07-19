// =============================================================================
// persistence/StatsRecorder.js — BEST-EFFORT persistence for one live table. Every
// DB call is wrapped so a missing/broken database NEVER disrupts gameplay: on any
// failure the recorder disables itself and the table keeps playing in-memory
// (identical to the no-DB guest path). When a DB IS present it records the full
// audit trail (games/game_seats/hands/hand_results/hand_events) and applies the
// per-hand settlement delta to each seated player's profile (§7).
//
// AI seats map to stable is_ai=1 profiles so head-to-head stats accumulate too.
// =============================================================================

import { HandRepository } from './HandRepository.js';
import { PlayerRepository } from './PlayerRepository.js';
import { buildHandRecords } from './handRecords.js';
import { SEATS } from '../domain/melds.js';

const AI_PROFILE_NAMES = { S: 'AI South', W: 'AI West', N: 'AI North' };

export class StatsRecorder {
  constructor() {
    this.hands = new HandRepository();
    this.players = new PlayerRepository();
    this.enabled = true;      // flipped off on first DB failure
    this.gameId = null;
    this.seatPlayerIds = null;
    this._initPromise = null;
  }

  _disable(err) {
    if (this.enabled) console.warn('[StatsRecorder] disabled (no persistence):', err?.message || err);
    this.enabled = false;
  }

  /**
   * Resolve the four seat profiles and open a game row. `humanPlayerId` is the
   * human's persisted id (from /api/identity); null for an offline guest, which
   * disables recording. Idempotent + memoized: the first caller does the work.
   * @returns {Promise<?{seatPlayerIds:Object, startingMoney:number|null}>}
   */
  async init(humanPlayerId) {
    if (!this.enabled) return null;
    if (this._initPromise) return this._initPromise;
    this._initPromise = (async () => {
      if (!humanPlayerId) { this._disable(new Error('offline guest — no profile')); return null; }
      try {
        const human = await this.players.findById(humanPlayerId);
        if (!human) { this._disable(new Error(`player ${humanPlayerId} not found`)); return null; }
        const ids = { E: human.id };
        for (const s of ['S', 'W', 'N']) ids[s] = (await this.players.findOrCreateAi(AI_PROFILE_NAMES[s])).id;
        this.gameId = await this.hands.createGame();
        await this.hands.setSeats(this.gameId,
          SEATS.map((seat) => ({ seat, playerId: ids[seat], isAi: seat !== 'E' })));
        this.seatPlayerIds = ids;
        return { seatPlayerIds: ids, startingMoney: Number(human.current_money) };
      } catch (err) { this._disable(err); return null; }
    })();
    return this._initPromise;
  }

  /**
   * Persist one completed hand + apply the settlement delta to each profile.
   * No-op (and self-disabling) if recording is off or not initialized.
   */
  async recordHand({ handNumber, roundWind, dealerSeat, cfg, result, balancesAfter, pointsAfter }) {
    if (!this.enabled || !this.gameId || !this.seatPlayerIds) return;
    try {
      const { hand, results, events } = buildHandRecords({
        handNumber, roundWind, dealerSeat, cfg, result,
        seatPlayerIds: this.seatPlayerIds, balancesAfter, pointsAfter,
      });
      await this.hands.saveHand(this.gameId, hand, results, events);
      for (const r of results) {
        await this.players.applyHandDelta(r.player_id, {
          moneyDelta: r.money_delta + r.flower_pay_delta + r.penalty_delta,
          pointsDelta: r.points_delta,
          debtDelta: r.debt_incurred,
          moneyWonDelta: Math.max(0, r.money_delta + r.penalty_delta),
          playedInc: 1,
          wonInc: r.is_winner,
        });
      }
    } catch (err) { this._disable(err); }
  }

  async endGame(reason) {
    if (!this.enabled || !this.gameId) return;
    try { await this.hands.endGame(this.gameId, reason); } catch (err) { this._disable(err); }
  }
}
