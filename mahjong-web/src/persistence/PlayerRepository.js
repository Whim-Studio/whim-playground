// =============================================================================
// persistence/PlayerRepository.js — passwordless identity + stats persistence.
// The identity policy (confirmed): unique key is (name, email_key) where
// email_key = COALESCE(email,''). Name-only and name+email rows never collide.
// Guests get a generated unique display name and email = NULL.
// =============================================================================

import { query } from './db.js';
import { STARTING_MONEY } from '../domain/constants.js';

const NEW_PROFILE_COLS =
  'games_played, games_won, current_money, money_won, current_points, owed_debt';

export class PlayerRepository {
  /**
   * Guest: create a fresh row with a generated unique display name, email NULL.
   * Caller supplies the generated name (see api/identity.generateGuestName) and
   * retries on the rare unique collision.
   */
  async createGuest(name) {
    const rows = await query(
      `INSERT INTO players (name, email, is_ai, is_guest, current_money)
       VALUES (:name, NULL, 0, 1, :money)`,
      { name, money: STARTING_MONEY });
    return this.findById(rows.insertId);
  }

  /**
   * Name-only OR name+email lookup-or-create. `email` null/'' => name-only row
   * (email_key=''). Because email_key is a generated column we match on
   * COALESCE(email,'') so we never create a second row for the same identity.
   */
  async findOrCreateNamed(name, email = null) {
    const key = email || '';
    const existing = await query(
      `SELECT * FROM players WHERE name = :name AND email_key = :key LIMIT 1`,
      { name, key });
    if (existing.length) return existing[0];

    await query(
      `INSERT INTO players (name, email, is_ai, is_guest, current_money)
       VALUES (:name, :email, 0, 0, :money)`,
      { name, email: email || null, money: STARTING_MONEY });
    const created = await query(
      `SELECT * FROM players WHERE name = :name AND email_key = :key LIMIT 1`,
      { name, key });
    return created[0];
  }

  async findById(id) {
    const rows = await query(`SELECT * FROM players WHERE id = :id`, { id });
    return rows[0] || null;
  }

  /** Ensure an AI profile exists for a stable AI identity (is_ai=1). */
  async findOrCreateAi(name) {
    const rows = await query(
      `SELECT * FROM players WHERE name = :name AND is_ai = 1 LIMIT 1`, { name });
    if (rows.length) return rows[0];
    const ins = await query(
      `INSERT INTO players (name, email, is_ai, is_guest, current_money)
       VALUES (:name, NULL, 1, 0, :money)`, { name, money: STARTING_MONEY });
    return this.findById(ins.insertId);
  }

  /**
   * Apply a per-hand settlement delta atomically. Stats update after EVERY hand
   * (§7). Money/points/debt are signed deltas; games_played/won are increments.
   */
  async applyHandDelta(playerId, { moneyDelta, pointsDelta, debtDelta = 0,
    moneyWonDelta = 0, playedInc = 0, wonInc = 0 }) {
    await query(
      `UPDATE players SET
         current_money  = current_money  + :moneyDelta,
         current_points = current_points + :pointsDelta,
         owed_debt      = owed_debt      + :debtDelta,
         money_won      = money_won      + :moneyWonDelta,
         games_played   = games_played   + :playedInc,
         games_won      = games_won      + :wonInc,
         last_played_at = CURRENT_TIMESTAMP
       WHERE id = :playerId`,
      { playerId, moneyDelta, pointsDelta, debtDelta, moneyWonDelta, playedInc, wonInc });
  }
}
