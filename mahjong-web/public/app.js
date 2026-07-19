// =============================================================================
// public/app.js — SPA controller (skeleton). Renders screens and talks to the
// REST API. Game rendering/turn wiring is stubbed to match the server skeleton;
// identity + round-config flows and the 6-second claim timer are wired so the
// UX and the consent boundary are demonstrable. NEVER holds hidden tile data —
// the client only receives the redacted view the server sends.
// =============================================================================

const $ = (sel) => document.querySelector(sel);
const show = (id) => document.querySelectorAll('.screen')
  .forEach((s) => s.classList.toggle('active', s.id === id));

const state = { player: null, mode: 'guest', limitMode: 'limited', gameId: null, lastCfg: null };

// ---- identity screen --------------------------------------------------------
document.querySelectorAll('#screen-identity .seg-btn').forEach((b) => {
  b.onclick = () => {
    document.querySelectorAll('#screen-identity .seg-btn').forEach((x) => x.classList.remove('active'));
    b.classList.add('active');
    state.mode = b.dataset.mode;
    $('#fields-name').classList.toggle('hidden', state.mode === 'guest');
    $('#fields-email').classList.toggle('hidden', state.mode !== 'name_email');
  };
});

$('#btn-start').onclick = async () => {
  const body = { mode: state.mode, name: $('#in-name').value.trim(), email: $('#in-email').value.trim() };
  try {
    const res = await fetch('/api/identity', {
      method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(body),
    });
    const data = await res.json();
    if (!res.ok) {
      // No DB provisioned -> 503. Guests can still play offline (no stats saved).
      if (res.status === 503 && state.mode === 'guest') return startOffline();
      return showError(data.errors?.join(', ') || data.error || 'could not start');
    }
    state.player = data.player;
    renderHud();
    show('screen-round');
  } catch (e) {
    if (state.mode === 'guest') return startOffline();
    showError('Server/database unavailable — run MySQL + schema.sql (see README).');
  }
};

function startOffline() {
  state.player = { name: 'Guest (unsaved)', currentMoney: 1000, currentPoints: 0 };
  renderHud();
  show('screen-round');
}
function showError(msg) { const el = $('#identity-error'); el.textContent = msg; el.classList.remove('hidden'); }

// ---- round setup ------------------------------------------------------------
document.querySelectorAll('#screen-round .seg-btn').forEach((b) => {
  b.onclick = () => {
    document.querySelectorAll('#screen-round .seg-btn').forEach((x) => x.classList.remove('active'));
    b.classList.add('active');
    state.limitMode = b.dataset.limit;
    $('#limit-fields').classList.toggle('hidden', state.limitMode === 'unlimited');
  };
});

// ---- WebSocket live play ----------------------------------------------------
let ws = null;
function connectWS() {
  return new Promise((resolve) => {
    if (ws && ws.readyState === WebSocket.OPEN) return resolve();
    const proto = location.protocol === 'https:' ? 'wss' : 'ws';
    ws = new WebSocket(`${proto}://${location.host}/ws`);
    ws.onopen = () => {
      ws.send(JSON.stringify({ type: 'hello',
        playerName: state.player?.name || 'You',
        playerId: state.player?.id ?? null }));   // null for offline guests → no stats saved
      resolve();
    };
    ws.onmessage = (ev) => handleServer(JSON.parse(ev.data));
    ws.onclose = () => { $('#rack').innerHTML = '<span class="sub">Disconnected. Reload to play again.</span>'; };
  });
}
function sendWS(obj) { if (ws && ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(obj)); }

$('#btn-deal').onclick = async () => {
  const cfg = state.limitMode === 'unlimited'
    ? { limitMode: 'unlimited' }
    : { limitMode: 'limited', pointsLimit: Number($('#in-points').value), moneyLimit: Number($('#in-money').value) };
  state.lastCfg = cfg;
  $('#hud-mode').textContent = cfg.limitMode === 'unlimited'
    ? 'Unlimited ($1/pt)' : `Limited ${cfg.pointsLimit}/$${cfg.moneyLimit}`;
  $('#rack').innerHTML = '<span class="sub">Dealing…</span>';
  show('screen-table');
  await connectWS();
  sendWS({ type: 'deal', config: cfg });
};

let myTurn = false;
function handleServer(m) {
  switch (m.type) {
    case 'state': renderTable(m.view); if (m.balances) $('#hud-money').textContent = m.balances.E; break;
    case 'turn':
      myTurn = m.yourTurn;
      $('#turn-note').textContent = m.yourTurn ? 'Your turn — tap a tile to discard' : `Waiting for ${nameOf(m.seat)}…`;
      renderTable(state.view); // refresh rack interactivity + win button
      break;
    case 'claim_request': openClaimWindow(m); break;
    case 'discard': case 'claim': break; // 'state' already refreshes the board
    case 'settlement': myTurn = false; renderSettlement(m); break;
    case 'error': $('#turn-note').textContent = '⚠ ' + m.message; break;
  }
}
function nameOf(seat) { return state.view?.namesBySeat?.[seat] || ({ E: 'You', S: 'AI South', W: 'AI West', N: 'AI North' }[seat]); }

const SEAT_ORDER = ['E', 'S', 'W', 'N'];
function renderSettlement(r) {
  const title = r.resultType === 'draw'
    ? `Hand ${r.handNumber}: drawn game`
    : r.resultType === 'false_mahjong'
    ? `Hand ${r.handNumber}: ${r.offenderName} declared a false Mahjong`
    : `Hand ${r.handNumber}: ${r.winnerName} wins ${r.finalPoints} pts`;
  $('#settle-title').textContent = title;

  const badges = [];
  if (r.fullyConcealed) badges.push('Fully concealed');
  if (r.isLimitHand) badges.push('Limit hand');
  if (r.specialHand) badges.push(r.specialHand.replace(/_/g, ' '));
  if (r.doublesCount) badges.push(`${r.doublesCount} doubles`);
  if (r.resultType === 'false_mahjong') badges.push(`False Mahjong · −$${r.penaltyPerOpponent}/player`);

  const rows = SEAT_ORDER.map((s) => {
    const d = r.moneyDeltas[s] || 0;
    const cls = d > 0 ? 'good' : d < 0 ? 'bad' : 'muted';
    const sign = d > 0 ? '+' : '';
    return `<div style="display:flex;justify-content:space-between;padding:4px 0;border-bottom:1px solid var(--line)">
      <span>${r.names[s]}${s === r.winnerSeat ? ' 🏆' : ''}</span>
      <span><b style="color:var(--${cls})">${sign}$${d}</b> &nbsp; → $${r.balances[s]}</span></div>`;
  }).join('');

  const scoring = r.scoring ? renderScoring(r.scoring, r.winnerName) : '';
  const revealed = r.hands ? renderHands(r) : '';
  $('#settle-body').innerHTML =
    (badges.length ? `<p>${badges.map((b) => `<span class="pill">${b}</span>`).join(' ')}</p>` : '') + rows + scoring + revealed;

  $('#bankrupt-banner').classList.toggle('hidden', !r.ended);
  $('#btn-next').classList.toggle('hidden', !!r.ended);
  $('#hud-money').textContent = r.balances.E; // human East running balance
  show('screen-settlement');
}

// Pretty tile glyphs for the reveal.
const SUIT_SYM = { D: '●', B: '┃', C: '萬' };
const WIND_SYM = { WE: '東', WS: '南', WW: '西', WN: '北' };
const DRAGON_SYM = { DR: '中', DG: '發', DW: '白' };
function tileGlyph(code) {
  if (/^[DBC][1-9]$/.test(code)) return code[1] + SUIT_SYM[code[0]];
  if (WIND_SYM[code]) return WIND_SYM[code];
  if (DRAGON_SYM[code]) return DRAGON_SYM[code];
  if (code[0] === 'F') return '🌸';
  if (code[0] === 'S') return '🍁';
  return code;
}
function tileHtml(code) {
  return `<span class="tile" style="min-width:26px;height:34px;font-size:14px">${tileGlyph(code)}</span>`;
}
function meldHtml(m) {
  const tiles = m.tiles.map(tileHtml).join('');
  const tag = m.concealed && m.type === 'kong' ? ' (concealed)' : '';
  return `<span style="display:inline-flex;gap:2px;margin-right:8px;opacity:.95">${tiles}<span class="sub" style="align-self:center">${m.type}${tag}</span></span>`;
}
// Itemized §6 scoring math for the winning hand.
function renderScoring(sc, winnerName) {
  const line = (l, v) => `<div style="display:flex;justify-content:space-between;padding:2px 0"><span class="sub">${l}</span><span>${v}</span></div>`;
  const meldRows = sc.melds.map((m) => {
    const tiles = m.tiles.map((t) => `<span class="tile" style="min-width:22px;height:28px;font-size:12px">${tileGlyph(t)}</span>`).join('');
    const kind = m.type + (m.type !== 'pair' ? (m.concealed ? ' (concealed)' : ' (exposed)') : '');
    return `<div style="display:flex;justify-content:space-between;align-items:center;padding:2px 0">
      <span style="display:inline-flex;gap:2px">${tiles}</span>
      <span class="sub">${kind} · ${m.points} pt${m.points === 1 ? '' : 's'}</span></div>`;
  }).join('');

  let doublesBlock;
  if (sc.specialHand) {
    doublesBlock = line('Limit hand', `${sc.specialHand.replace(/_/g, ' ')} — scored as the Points Limit (doubles skipped)`);
  } else if (!sc.appliedDoubles.length) {
    doublesBlock = line('Doubles', 'none (×1)');
  } else {
    doublesBlock = sc.appliedDoubles.map((d) => line(d.label, `${d.value} dbl → ×${2 ** d.value}`)).join('')
      + line('<b>Total multiplier</b>', `<b>×${sc.multiplier}</b> (${sc.doublesCount} doubles)`);
  }

  const limitLabel = sc.pointsLimit == null ? 'Unlimited' : sc.pointsLimit;
  const moneyLine = sc.pointsLimit == null
    ? `$1 × ${sc.finalPoints} pts = <b>$${sc.moneyPerOpponent}</b> per opponent`
    : `${sc.finalPoints} ÷ ${sc.pointsLimit} × $${sc.moneyLimit} = <b>$${sc.moneyPerOpponent}</b> per opponent`;
  const capNote = sc.isLimitHand ? ` <span class="pill">capped at ${limitLabel}</span>` : '';

  return `<div style="margin-top:12px;padding:10px;border:1px solid var(--line);border-radius:10px;background:#0c1520">
    <h3 style="margin:0 0 6px;font-size:15px">How ${winnerName} scored</h3>
    ${meldRows}
    <hr style="border:0;border-top:1px solid var(--line);margin:6px 0">
    ${line('Base (melds)', `${sc.basePoints} pts`)}
    ${sc.flowerCount ? line('Flowers / seasons', `${sc.flowerCount} × 4 = ${sc.flowerPoints} pts`) : ''}
    ${line('Mahjong bonus (1% of limit)', `${sc.mahjongBonus} pts`)}
    ${line('<b>Subtotal</b>', `<b>${sc.subtotal} pts</b>`)}
    <hr style="border:0;border-top:1px solid var(--line);margin:6px 0">
    ${doublesBlock}
    <hr style="border:0;border-top:1px solid var(--line);margin:6px 0">
    ${line('<b>Final score</b>', `<b>${sc.finalPoints} pts</b>${capNote}`)}
    ${line('Money (limit ' + limitLabel + ')', moneyLine)}
  </div>`;
}
function renderHands(r) {
  const blocks = SEAT_ORDER.map((s) => {
    const h = r.hands[s];
    const win = s === r.winnerSeat;
    const melds = h.melds.map(meldHtml).join('');
    const concealed = h.concealed.map(tileHtml).join('');
    const flowers = h.flowers.length ? `<div class="sub" style="margin-top:4px">flowers: ${h.flowers.map(tileHtml).join('')}</div>` : '';
    return `<div style="margin-top:10px;padding:8px;border-radius:9px;background:${win ? 'rgba(52,211,153,.12)' : '#0c1520'};border:1px solid ${win ? 'var(--good)' : 'var(--line)'}">
      <div style="font-size:13px;margin-bottom:4px">${r.names[s]}${win ? ' 🏆' : ''}</div>
      <div style="display:flex;flex-wrap:wrap;gap:3px;align-items:center">${melds}${concealed}</div>${flowers}
    </div>`;
  }).join('');
  return `<h3 style="margin:14px 0 2px;font-size:15px">Revealed hands</h3>${blocks}`;
}

// ---- live table rendering ---------------------------------------------------
function renderTable(view) {
  if (!view) return;
  state.view = view;

  $('#opponents').innerHTML = view.opponents.map((o) => `
    <div class="card" style="flex:1;padding:8px;font-size:12px">
      <div>${nameOf(o.seat)}</div>
      <div class="sub">${o.concealedCount} tiles${o.flowers.length ? ` · ${o.flowers.length}🌸` : ''}</div>
      <div style="display:flex;flex-wrap:wrap;gap:2px;margin-top:4px">${o.melds.map(meldHtml).join('')}</div>
    </div>`).join('');

  $('#discard-pile').innerHTML = view.discardPile.length
    ? view.discardPile.map((d) => tileHtml(d.tile)).join('')
    : '<span class="sub">none yet</span>';

  const myFlowers = view.flowers.length
    ? `<span class="sub" style="margin-left:6px">flowers ${view.flowers.map(tileHtml).join('')}</span>` : '';
  $('#melds').innerHTML = view.exposed.map(meldHtml).join('') + myFlowers;

  const canAct = myTurn;
  $('#rack').innerHTML = view.concealed.map((code) =>
    `<span class="tile rack-tile${canAct ? ' actionable' : ''}" data-tile="${code}">${tileGlyph(code)}</span>`).join('')
    + (myTurn && view.canWin ? `<button id="btn-mahjong" class="primary" style="width:auto;padding:8px 14px;margin-left:8px">Declare Mahjong 🀄</button>` : '');

  if (canAct) {
    $('#rack').querySelectorAll('.rack-tile').forEach((el) => {
      el.onclick = () => { if (!myTurn) return; myTurn = false; $('#turn-note').textContent = '…'; sendWS({ type: 'move', action: 'discard', tile: el.dataset.tile }); };
    });
    const mb = $('#btn-mahjong');
    if (mb) mb.onclick = () => { myTurn = false; sendWS({ type: 'move', action: 'mahjong' }); };
  }
}

// ---- 6-second claim window, driven by a real server claim_request ----------
function openClaimWindow(req) {
  const prompt = $('#claim-prompt'); const bar = $('#claim-bar');
  // Only show the actions the player is actually allowed.
  prompt.querySelector('[data-claim="pung"]').style.display = req.canPung ? '' : 'none';
  prompt.querySelector('[data-claim="mahjong"]').style.display = req.canMahjong ? '' : 'none';
  prompt.classList.remove('hidden');
  bar.style.transition = 'none'; bar.style.width = '100%';
  requestAnimationFrame(() => { bar.style.transition = 'width 6s linear'; bar.style.width = '0%'; });
  let done = false;
  const timer = setTimeout(() => finish('pass'), 6000); // server also auto-passes at 6s
  function finish(claim) {
    if (done) return; done = true;
    clearTimeout(timer); prompt.classList.add('hidden');
    sendWS({ type: 'move', claim });
  }
  prompt.querySelectorAll('button').forEach((btn) => { btn.onclick = () => finish(btn.dataset.claim); });
}

function renderHud() {
  if (!state.player) return;
  $('#hud-name').textContent = state.player.name;
  $('#hud-money').textContent = state.player.currentMoney;
  $('#hud-points').textContent = state.player.currentPoints;
}

$('#btn-next').onclick = () => show('screen-round');

show('screen-identity');
