// =============================================================================
// api/routes.js — REST surface. In v1 the human drives a table via these routes;
// the engine runs the 3 AI seats internally. The transport is intentionally thin
// so a future WebSocket layer (realtime/transport.js) can replace polling without
// touching the engine. DB-unavailable errors map to 503 so the skeleton degrades
// clearly instead of crashing.
// =============================================================================

import { Router } from 'express';
import { PlayerRepository } from '../persistence/PlayerRepository.js';
import { resolveIdentity, validateIdentityInput } from './identity.js';
import { validateRoundConfig, validateMove } from './validation.js';
import { GameSession } from '../engine/GameSession.js';

export function buildRouter() {
  const router = Router();
  const players = new PlayerRepository();
  const sessions = new Map(); // in-memory tables: id -> GameSession (v1, single node)
  let nextId = 1;

  const guard = (fn) => async (req, res) => {
    try { await fn(req, res); }
    catch (err) {
      if (err.dbUnavailable) return res.status(503).json({ error: 'database unavailable', detail: err.code });
      res.status(500).json({ error: 'internal', detail: err.message });
    }
  };

  router.get('/health', (_req, res) => res.json({ ok: true, version: '0.1.0-skeleton' }));

  // ---- identity (passwordless) --------------------------------------------
  router.post('/identity', guard(async (req, res) => {
    const v = validateIdentityInput(req.body || {});
    if (!v.ok) return res.status(400).json({ errors: v.errors });
    const player = await resolveIdentity(players, req.body);
    res.json({ player: publicPlayer(player) });
  }));

  router.get('/players/:id', guard(async (req, res) => {
    const player = await players.findById(Number(req.params.id));
    if (!player) return res.status(404).json({ error: 'not found' });
    res.json({ player: publicPlayer(player) });
  }));

  // ---- table / hand lifecycle ---------------------------------------------
  // A table is an in-memory GameSession (human East + 3 AI). No DB needed to play;
  // stats persistence is separate + MySQL-gated. Interactive per-tile human turns
  // are the documented next step — v1 auto-plays the hand and returns settlement.
  router.post('/games', (req, res) => {
    const id = String(nextId++);
    sessions.set(id, new GameSession({ humanName: req.body?.playerName || 'You' }));
    res.json({ gameId: id });
  });

  router.post('/games/:id/hands', (req, res) => {
    const session = sessions.get(req.params.id);
    if (!session) return res.status(404).json({ error: 'game not found' });
    if (session.ended) return res.status(409).json({ error: 'game ended', reason: session.endedReason });
    const cfg = validateRoundConfig(req.body || {}); // Limited/Unlimited chosen per hand
    if (!cfg.ok) return res.status(400).json({ errors: cfg.errors });
    res.json({ result: session.playHand(cfg.value) });
  });

  // Reserved for interactive human moves (draw/discard/claim) once the engine
  // suspends mid-turn for a human seat. Validates shape today.
  router.post('/games/:id/moves', (req, res) => {
    const mv = validateMove(req.body || {});
    if (!mv.ok) return res.status(400).json({ errors: mv.errors });
    res.status(501).json({ error: 'interactive turns not implemented (skeleton)', move: mv.value });
  });

  return router;
}

/** Strip nothing sensitive (no secrets), but normalize types for the client. */
function publicPlayer(p) {
  return {
    id: p.id, name: p.name, email: p.email ?? null,
    isAi: !!p.is_ai, isGuest: !!p.is_guest,
    gamesPlayed: p.games_played, gamesWon: p.games_won,
    currentMoney: p.current_money, moneyWon: p.money_won,
    currentPoints: p.current_points, owedDebt: p.owed_debt,
  };
}
