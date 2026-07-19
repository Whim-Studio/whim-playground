// =============================================================================
// api/validation.js — server-side input guards. The client is NEVER trusted for
// hand legality, scoring, money, or hidden tiles. Rule/score/settlement checks
// live in the engine; these are the request-shape guards at the boundary.
// =============================================================================

/** Round config chosen fresh before every hand (§ constraint). */
export function validateRoundConfig(body) {
  const errors = [];
  const mode = body?.limitMode;
  if (mode !== 'limited' && mode !== 'unlimited') errors.push('limitMode must be limited|unlimited');
  if (mode === 'limited') {
    if (!Number.isInteger(body.pointsLimit) || body.pointsLimit <= 0) errors.push('pointsLimit must be a positive integer');
    if (!Number.isInteger(body.moneyLimit) || body.moneyLimit <= 0) errors.push('moneyLimit must be a positive integer');
  }
  return errors.length ? { ok: false, errors } : {
    ok: true,
    value: mode === 'unlimited'
      ? { limitMode: 'unlimited', pointsLimit: null, moneyLimit: null }
      : { limitMode: 'limited', pointsLimit: body.pointsLimit, moneyLimit: body.moneyLimit },
  };
}

/** A move submitted by the human seat; the engine re-validates legality. */
export function validateMove(body) {
  const actions = ['discard', 'conceal_kong', 'kong_upgrade', 'mahjong', 'claim', 'pass'];
  if (!body || !actions.includes(body.action)) return { ok: false, errors: ['unknown action'] };
  if ((body.action === 'discard' || body.action === 'claim') && typeof body.tile !== 'string')
    return { ok: false, errors: ['tile required'] };
  return { ok: true, value: body };
}
