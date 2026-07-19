// =============================================================================
// players/SeatController.js — the seat-agnostic controller interface.
// GameEngine talks to seats ONLY through this contract, so an AI, a local human,
// or (future) a remote human over WebSocket are interchangeable. This is the
// single most important abstraction for painless multiplayer later.
// =============================================================================

/**
 * @interface SeatController
 * All methods receive a redacted `view` (GameEngine.viewFor(seat)) and a
 * `deadline` (epoch ms). Implementations MUST resolve within the deadline; the
 * engine treats a late/absent answer as "pass" (for claims) or an auto-discard
 * (for turns) so one slow/disconnected seat never stalls the table.
 */
export class SeatController {
  constructor(seat) { this.seat = seat; }

  /** @returns {Promise<{action:'discard'|'conceal_kong'|'kong_upgrade'|'mahjong', tile?:string}>} */
  async decideTurn(view, deadline) { throw new Error('decideTurn not implemented'); }

  /** @returns {Promise<{claim:'mahjong'|'pung'|'pass'}>} within the 6s window */
  async decideClaim(view, discard, deadline) { throw new Error('decideClaim not implemented'); }

  /** Optional lifecycle hook (e.g. push state to a connected client). */
  onStateChange(view) {}
}
