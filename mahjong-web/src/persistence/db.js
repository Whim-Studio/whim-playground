// =============================================================================
// persistence/db.js — lazy MySQL pool (mysql2/promise). Connects on first use so
// the server still boots with no DB reachable; DB-backed endpoints then return a
// clear 503. mysql2 is imported dynamically so the skeleton runs before
// `npm install`. Production target is MySQL (PERSISTENCE_DRIVER=mysql).
// =============================================================================

import { config } from '../config.js';

let pool = null;

export async function getPool() {
  if (pool) return pool;
  const mysql = await import('mysql2/promise');   // dynamic: optional at boot
  pool = mysql.createPool({
    host: config.db.host, port: config.db.port,
    user: config.db.user, password: config.db.password, database: config.db.name,
    connectionLimit: config.db.connectionLimit,
    waitForConnections: true, namedPlaceholders: true,
  });
  return pool;
}

/** Run a query; throws a tagged error the API layer maps to 503 if DB is down. */
export async function query(sql, params) {
  try {
    const p = await getPool();
    const [rows] = await p.query(sql, params);
    return rows;
  } catch (err) {
    err.dbUnavailable = true;
    throw err;
  }
}

/**
 * Run `fn(conn)` inside a single transaction on a dedicated connection, so a
 * multi-row hand write commits atomically (§7). `conn.q(sql, params)` returns the
 * rows (or the OkPacket for writes). Rolls back and re-throws on any error.
 */
export async function withTransaction(fn) {
  let conn;
  try {
    const p = await getPool();
    conn = await p.getConnection();
    await conn.beginTransaction();
    const q = async (sql, params) => { const [rows] = await conn.query(sql, params); return rows; };
    const out = await fn({ q });
    await conn.commit();
    return out;
  } catch (err) {
    if (conn) { try { await conn.rollback(); } catch { /* ignore */ } }
    err.dbUnavailable = true;
    throw err;
  } finally {
    if (conn) conn.release();
  }
}
