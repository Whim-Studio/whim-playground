// =============================================================================
// api/identity.js — passwordless identity helpers. NO auth, NO password, NO email
// verification (by design). The disclosure copy in public/index.html must be shown
// before any name/email submission — this module only handles lookup/creation.
// =============================================================================

const ADJECTIVES = ['Swift', 'Jade', 'Lucky', 'Bamboo', 'Golden', 'Quiet', 'Bold', 'Nimble'];
const ANIMALS = ['Panda', 'Crane', 'Dragon', 'Tiger', 'Koi', 'Sparrow', 'Fox', 'Turtle'];

/** e.g. "Swift Panda 482". Uniqueness enforced by the DB unique key + retry. */
export function generateGuestName(rng = Math.random) {
  const a = ADJECTIVES[Math.floor(rng() * ADJECTIVES.length)];
  const n = ANIMALS[Math.floor(rng() * ANIMALS.length)];
  const num = 100 + Math.floor(rng() * 900);
  return `${a} ${n} ${num}`;
}

const NAME_RE = /^[\p{L}\p{N} ._-]{1,64}$/u;
const EMAIL_RE = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

/** Server-side validation of identity input; never trust the client. */
export function validateIdentityInput({ mode, name, email }) {
  const errors = [];
  if (mode === 'guest') return { ok: true };
  if (!name || !NAME_RE.test(name)) errors.push('name must be 1–64 valid characters');
  if (mode === 'name_email') {
    if (!email || !EMAIL_RE.test(email)) errors.push('email must be a valid address');
  } else if (email) {
    errors.push('email not allowed in name-only mode');
  }
  return errors.length ? { ok: false, errors } : { ok: true };
}

/**
 * Resolve identity input to a player row via the repository.
 * @param {PlayerRepository} repo
 */
export async function resolveIdentity(repo, { mode, name, email }) {
  if (mode === 'guest') {
    // Retry a few times on the rare generated-name collision.
    for (let i = 0; i < 5; i++) {
      try { return await repo.createGuest(generateGuestName()); }
      catch (e) { if (!/duplicate/i.test(e.message)) throw e; }
    }
    throw new Error('could not allocate a unique guest name');
  }
  return repo.findOrCreateNamed(name, mode === 'name_email' ? email : null);
}
