// =============================================================================
// realtime/transport.js — abstraction seam for future real-time multiplayer.
// v1 ships REST only (see api/routes.js). This interface documents where a
// WebSocket implementation plugs in so the engine/seat layers stay untouched.
//
// MULTIPLAYER READINESS SUMMARY
//   Already multiplayer-ready:
//     * SeatController contract (AI/human/remote interchangeable)
//     * GameEngine is seat-agnostic and emits redacted per-seat views
//     * schema: games + game_seats map any seat to any player row
//     * hidden state (wall/opponent hands) never leaves the server
//   Intentionally still single-human-vs-AI in v1 (not painted into a corner):
//     * one process holds the table in memory; no cross-node session store
//     * human decisions arrive via REST, not a persistent socket
//     * no lobby/matchmaking, no reconnection/resume protocol
// =============================================================================

/**
 * @interface Transport
 * pushView(seat, view): deliver a redacted state snapshot to a seat's client.
 * onDecision(handler): register a callback invoked when a seat submits a move.
 */
export class Transport {
  pushView(seat, view) { throw new Error('Transport.pushView not implemented'); }
  onDecision(handler) { throw new Error('Transport.onDecision not implemented'); }
}
