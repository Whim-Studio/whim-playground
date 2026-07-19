// =============================================================================
// persistence/HandRepository.js — persist games, hands, per-seat results, and the
// authoritative event log. SKELETON: signatures + the insert shapes are defined;
// bodies that need transactional multi-row writes are stubbed. All writes for one
// hand should run in a single transaction so settlement stays consistent (§7).
// =============================================================================

import { query, withTransaction } from './db.js';

export class HandRepository {
  async createGame() {
    const r = await query(`INSERT INTO games () VALUES ()`, {});
    return r.insertId;
  }

  async setSeats(gameId, seats /* [{seat, playerId, isAi}] */) {
    for (const s of seats) {
      await query(
        `INSERT INTO game_seats (game_id, seat, player_id, is_ai)
         VALUES (:gameId, :seat, :playerId, :isAi)
         ON DUPLICATE KEY UPDATE player_id = VALUES(player_id), is_ai = VALUES(is_ai)`,
        { gameId, seat: s.seat, playerId: s.playerId, isAi: s.isAi ? 1 : 0 });
    }
  }

  /**
   * Persist a completed hand + its 4 hand_results + hand_events in ONE tx, and
   * bump games.hands_played. Row shapes come from persistence/handRecords.js.
   * @param {Object} hand      maps to `hands` columns
   * @param {Object[]} results 4 rows -> `hand_results`
   * @param {Object[]} events  -> `hand_events` (the §8 authoritative log)
   * @returns {Promise<number>} the new hand id
   */
  async saveHand(gameId, hand, results, events) {
    return withTransaction(async ({ q }) => {
      const ins = await q(
        `INSERT INTO hands
           (game_id, hand_number, round_wind, dealer_seat, limit_mode, points_limit,
            money_limit, result_type, winner_seat, base_points, flower_points,
            mahjong_bonus, doubles_count, final_points, is_limit_hand, fully_concealed,
            special_hand, win_timing, wall_remaining, ended_at)
         VALUES
           (:game_id, :hand_number, :round_wind, :dealer_seat, :limit_mode, :points_limit,
            :money_limit, :result_type, :winner_seat, :base_points, :flower_points,
            :mahjong_bonus, :doubles_count, :final_points, :is_limit_hand, :fully_concealed,
            :special_hand, :win_timing, :wall_remaining, CURRENT_TIMESTAMP)`,
        { game_id: gameId, ...hand });
      const handId = ins.insertId;

      for (const r of results) {
        await q(
          `INSERT INTO hand_results
             (hand_id, player_id, seat, is_winner, points_delta, money_delta,
              flower_pay_delta, penalty_delta, debt_incurred, money_after, points_after)
           VALUES
             (:hand_id, :player_id, :seat, :is_winner, :points_delta, :money_delta,
              :flower_pay_delta, :penalty_delta, :debt_incurred, :money_after, :points_after)`,
          { hand_id: handId, ...r });
      }

      for (const e of events) {
        await q(
          `INSERT INTO hand_events (hand_id, seq, seat, event_type, tile_code, meta)
           VALUES (:hand_id, :seq, :seat, :event_type, :tile_code, :meta)`,
          { hand_id: handId, seq: e.seq, seat: e.seat, event_type: e.event_type,
            tile_code: e.tile_code, meta: e.meta == null ? null : JSON.stringify(e.meta) });
      }

      await q(`UPDATE games SET hands_played = hands_played + 1 WHERE id = :gameId`, { gameId });
      return handId;
    });
  }

  async endGame(gameId, reason /* 'bankruptcy'|'quit' */) {
    await query(`UPDATE games SET status='ended', ended_reason=:reason, ended_at=CURRENT_TIMESTAMP
                 WHERE id=:gameId`, { gameId, reason });
  }
}
