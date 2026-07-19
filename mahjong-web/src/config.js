// =============================================================================
// config.js — environment config (no secrets committed; see .env.example).
// Reads process.env directly; a real deploy can layer dotenv if desired.
// =============================================================================

const env = process.env;

export const config = {
  port: Number(env.PORT || 3000),
  persistenceDriver: env.PERSISTENCE_DRIVER || 'mysql', // 'mysql' | 'memory'
  db: {
    host: env.DB_HOST || '127.0.0.1',
    port: Number(env.DB_PORT || 3306),
    user: env.DB_USER || 'mahjong',
    password: env.DB_PASSWORD || '',
    name: env.DB_NAME || 'tiwas_mahjong',
    connectionLimit: Number(env.DB_CONNECTION_LIMIT || 10),
  },
};
