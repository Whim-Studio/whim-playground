// =============================================================================
// players/HumanController.js — bridges a human seat to the async transport.
// The engine awaits decideTurn/decideClaim; this controller parks a Promise and
// resolves it when the human's move arrives via `submit()` (called by the WS
// layer), or auto-resolves at the claim deadline (§3: 6s → pass). Swapping the
// transport (REST↔WS) changes only how submit() is invoked, not the engine.
// =============================================================================

import { SeatController } from './SeatController.js';
import { CLAIM_TIMEOUT_MS } from '../domain/constants.js';

export class HumanController extends SeatController {
  constructor(seat) { super(seat); this._pending = null; }

  /** Own turn: no hard timeout — the client drives. */
  decideTurn() {
    return new Promise((resolve) => { this._pending = { kind: 'turn', resolve, timer: null }; });
  }

  /** Claim window: resolve on the human's answer or auto-pass at the 6s deadline. */
  decideClaim() {
    return new Promise((resolve) => {
      const timer = setTimeout(() => { this._pending = null; resolve({ claim: 'pass' }); }, CLAIM_TIMEOUT_MS);
      this._pending = { kind: 'claim', resolve, timer };
    });
  }

  /** Called by the transport when the human submits a move. Returns handled?. */
  submit(move) {
    const p = this._pending;
    if (!p) return false;
    if (p.timer) clearTimeout(p.timer);
    this._pending = null;
    if (p.kind === 'turn') p.resolve({ action: move.action || 'discard', tile: move.tile });
    else p.resolve({ claim: move.claim || 'pass' });
    return true;
  }

  /** True if the engine is currently waiting on this seat. */
  get waiting() { return !!this._pending; }
  get waitingKind() { return this._pending?.kind ?? null; }
}
