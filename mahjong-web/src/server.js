// =============================================================================
// server.js — Express bootstrap + WebSocket transport. Serves the SPA (public/),
// the REST API (identity/stats + auto-play demo), and a WS endpoint at /ws that
// drives interactive human-vs-AI hands. Boots WITHOUT a database; DB-backed REST
// routes return 503 until MySQL is reachable.
// =============================================================================

import express from 'express';
import { createServer } from 'node:http';
import { WebSocketServer } from 'ws';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { config } from './config.js';
import { buildRouter } from './api/routes.js';
import { LiveGame } from './realtime/LiveGame.js';

const __dirname = dirname(fileURLToPath(import.meta.url));

const app = express();
app.use(express.json());
app.use('/api', buildRouter());
app.use(express.static(join(__dirname, '..', 'public')));

const server = createServer(app);

// One LiveGame per socket. The human is seat East; S/W/N are AI.
const wss = new WebSocketServer({ server, path: '/ws' });
wss.on('connection', (ws) => {
  const send = (type, payload) => { if (ws.readyState === ws.OPEN) ws.send(JSON.stringify({ type, ...payload })); };
  const live = new LiveGame(send);
  send('connected', { seat: 'E' });
  ws.on('message', (data) => {
    let msg; try { msg = JSON.parse(data.toString()); } catch { return send('error', { message: 'bad json' }); }
    Promise.resolve(live.handle(msg)).catch((err) => send('error', { message: err.message }));
  });
});

server.listen(config.port, () => {
  console.log(`Tiwa's Mah Jong on http://localhost:${config.port}  (WS at /ws)`);
  console.log(`Persistence driver: ${config.persistenceDriver} — DB '${config.db.name}' @ ${config.db.host}:${config.db.port}`);
});
