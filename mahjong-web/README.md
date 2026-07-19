# Tiwa's Mah Jong — Web (design + schema + skeleton)

Single-player-vs-3-AI browser implementation of **Tiwa's Mah Jong** (custom
scoring/doubling/money rules — not generic Mahjong). This directory is the
**design + MySQL schema + compiling code skeleton** deliverable: interfaces,
contracts, DDL, and the reviewable pure-logic (scoring order, money/penalty math,
claim priority, identity) are implemented and unit-tested; the combinatorial
engine, AI heuristic, and transactional persistence bodies are contract-defined
stubs marked `not implemented (skeleton)`.

## Stack

- **Node.js 20+ / Express (ESM)** — small, MySQL-friendly, easy to host on whim.run.
- **MySQL 8 (mysql2/promise)** — committed DDL in `db/schema.sql`. Connects lazily
  so the server boots with no DB; DB-backed routes return `503` until MySQL is up.
- **Vanilla JS SPA** (`public/`) — mobile-first, no framework.

## Run locally

```bash
cd mahjong-web
npm install
npm test                 # runs the implemented pure-logic tests (10 passing)
npm start                # http://localhost:3000  (boots without a DB)
```

With MySQL:

```bash
mysql -u root -p -e "CREATE DATABASE tiwas_mahjong CHARACTER SET utf8mb4;"
mysql -u root -p tiwas_mahjong < db/schema.sql
cp .env.example .env     # fill in DB_* ; then:
DB_USER=... DB_PASSWORD=... npm start
```

## Deploy on whim.run

Node service exposing `PORT`; provision a MySQL instance and set `DB_*` env vars.
Run `db/schema.sql` once against it. Static SPA is served by the same Express
process (`public/`), so a single web service covers API + client.

## Architecture (module map)

```
src/
  domain/      pure data: tiles.js (144 set + wall), constants.js (§6 values),
               melds.js, — no I/O, no rules decisions
  engine/      seat-agnostic rules: GameEngine (turn state machine), HandAnalyzer
               (win decomposition + doubles), ScoringEngine (§6 order — DONE),
               ClaimResolver (§3 priority — DONE), Settlement (§7/§8 money — DONE)
  players/     SeatController interface + AIController + HumanController
               (AI/human/remote interchangeable — the multiplayer seam)
  persistence/ db.js (lazy pool) + PlayerRepository (identity — DONE) + HandRepository
  api/         routes.js, identity.js, validation.js (server-side guards)
  realtime/    transport.js (WebSocket seam; documents ready vs. not-ready parts)
public/        index.html (+ verbatim consent copy), app.js, styles.css
db/schema.sql  full MySQL DDL
```

### Multiplayer readiness (per the brief)

**Already multiplayer-ready:** the `SeatController` contract, the seat-agnostic
`GameEngine` and its redacted per-seat `viewFor`, hidden state that never leaves
the server, and the `games`/`game_seats` schema that maps any seat to any player.
**Intentionally still single-human-vs-AI (not painted into a corner):** in-memory
table (no cross-node store), REST transport instead of a persistent socket, and no
lobby/matchmaking/reconnection. Swapping in `realtime/transport.js` (WebSocket)
does not touch the engine or seat contracts.

## Design decisions & open questions

Confirmed with the product owner and encoded here:

1. **First-tile win (§5 "1000 flat" vs Heavenly/Earthly/Human = 13 doubles):**
   take the **higher of the two** (`ScoringEngine.resolveFirstTileWin`). The flat
   1000 is treated as a limit hand (can't exceed the table's Points Limit).
2. **Identity collision (name-only vs name+email):** unique key `(name, COALESCE(email,''))`.
   Name-only and name+email rows with the same name are distinct; guests get a
   generated unique display name with `email NULL`.
3. **False-Mahjong penalty in Limited money games:** convert the 1000-point penalty
   with the same money formula — `floor((1000/pointsLimit) * moneyLimit)` per opponent
   (`Settlement.falseMahjongPenaltyPerOpponent`).

Known simplifications (documented, not bugs):

- **Kongs auto-declare** for all seats (including the human): both concealed kongs
  (self-drawn four of a kind) and late/exposed-kong upgrades (an exposed pung whose
  4th tile is later drawn), each with the required replacement draw (§4). They are
  not offered as an explicit choice.

Resolved:

- **MySQL stats persistence wired (best-effort).** When a DB is reachable and the
  human authenticated via `/api/identity`, every completed hand is written in one
  transaction to `hands` + `hand_results` + `hand_events`, and each seated profile's
  stats are updated (`applyHandDelta`: money/points/debt/games/wins). AI seats map
  to stable `is_ai=1` profiles so head-to-head stats accumulate. The pure result→row
  mapping (`persistence/handRecords.js`) is unit-tested (settlement vs flower vs
  penalty money split, unlimited nulls). Persistence is **best-effort**: any DB
  failure disables the recorder for that table and play continues in-memory, so the
  no-DB guest path is byte-for-byte unchanged. **Bankroll carry-over:** a returning
  player (persisted profile) loads their lifetime `current_money` as seat East's
  opening stack on the first hand (`startingBalances`, unit-tested); the three AI
  opponents always start from a fresh $1000 buy-in so they can't go permanently
  bankrupt across sessions. Offline guests / no-DB keep the fresh $1000 buy-in.
  The profile stays the lifetime ledger (`current_money` = 1000 + Σ all recorded
  deltas). The carry-over path still needs a live-DB smoke test — no MySQL is
  provisioned in this container.
- **False-Mahjong enforced server-side (§8).** Declaring or claiming Mahjong on a
  hand that is not a legal win ends the round immediately; the offender pays the
  limit penalty (`floor((1000/pointsLimit)*moneyLimit)`, or $1000 Unlimited) to each
  of the three opponents, with §7 partial-payment + bankruptcy applied. The client
  is never trusted — an illegal declaration is caught by `_isWin`/`isWinningHand`,
  not by the UI. Covered by `test/engine.test.js` and `test/logic.test.js`.
- **Late kong (exposed pung → exposed kong) now supported.** On its own turn, a
  seat holding an exposed pung that draws the 4th matching tile promotes it to an
  exposed kong and takes the mandatory replacement draw (§4); chains, and is skipped
  when the wall is empty. Covered by `test/engine.test.js`.
- **Discard-completed winning meld now scored as exposed.** On a win by claiming
  the discard, the completed pung is scored exposed (lower base points); the
  winning tile is attributed to a chow/pair first when possible (0 penalty) so the
  winner is never over-penalised. Covered by `test/engine.test.js`.

Still open (defaults chosen; confirm or correct):

4. **"Kong" claim priority tier:** §4 forbids claiming a kong from a discard, so the
   Kong priority tier is vestigial for discards. `ClaimResolver` filters kong claims
   out and keeps Mahjong > Pung. Confirm no discard-kong variant is wanted.
5. **Which structural doubles are mutually exclusive / independently stackable**
   (e.g. can `NO_CHOWS` co-exist with `DOUBLE_PUNG` + `ALL_ONE_SUIT_HONORS`?). The
   scorer stacks **every flag `HandAnalyzer.detectStructuralDoubles` returns**; the
   exact detection rules (and any exclusions) are the main open specification item
   before that analyzer is implemented. The cap at Points Limit bounds the blow-up.
6. **Pure single-suit hand with no honors** (Full Flush) has no listed double —
   only "All One Suit with Honours (7)". Assumed: no extra double unless honors are
   present. Confirm.
7. **`FULLY_CONCEALED` vs `ALL_CONCEALED_HAND`:** §5 describes them as the same
   2-double bonus; treated as one bonus (not stacked). Confirm they never double up.

## Status of each module

| Module | State |
|---|---|
| `db/schema.sql` | **complete** — reviewable DDL |
| `domain/constants.js`, `domain/tiles.js`, `domain/melds.js` | **complete** |
| `engine/ScoringEngine.js` (§6 order) | **complete + tested** |
| `engine/Settlement.js` (§7/§8 money) | **complete + tested** |
| `engine/ClaimResolver.js` (§3 priority) | **complete + tested** |
| `engine/HandAnalyzer.js` | **complete + tested** — win detection, decomposition, 13 orphans, structural doubles |
| `engine/GameEngine.js` | **playable + tested** — deal, flowers/replacement, concealed kong + **late/exposed-kong upgrade**, self-draw + mahjong-on-discard wins, **pung claim + turn-transfer**, scoring, settlement, plus an **interactive async `run()`** for live human play (300 sync + 120 async seeded hands pass) |
| `engine/GameSession.js` | **complete** — in-memory table, running balances, dealer/wind rotation, bankruptcy |
| `players/*` | **AI + Human controllers implemented + tested**; async SeatController contract live |
| `engine/GameEngine.run()` | **interactive async loop** — awaits the human's draw/discard/claim, streams state; matches the sync oracle's invariants (120 async hands tested) |
| `realtime/LiveGame.js` + `/ws` | **complete** — WebSocket per-connection driver: human = seat East, S/W/N = AI |
| `persistence/PlayerRepository.js` (identity) | **complete** (MySQL) |
| `persistence/HandRepository.js` | **complete** — transactional `saveHand` (hands + results + events), `setSeats`, `endGame` (`db.withTransaction`) |
| `persistence/handRecords.js` + `StatsRecorder.js` | **complete + tested** — pure result→row mapper; best-effort recorder wired into `LiveGame` (self-disables with no DB) |
| `api/*` | identity + validation + auto-play REST endpoints live (WS is the interactive path) |
| `public/*` | full live client: rack (tap to discard), discard pile, opponents, **real 6s claim prompt**, mahjong button, settlement + reveal; guests play offline when no DB |

## What's playable right now

`npm start`, open the app, **Play as Guest** (works with no DB), pick Limited/Unlimited,
**Deal** → you play a real hand over WebSocket: draw is automatic, **tap a tile to
discard**, a **6-second claim prompt** appears when you can Pung/Mahjong an opponent's
discard, and a **Declare Mahjong** button shows when your hand is legal. The three AI
seats claim and discard under the same rules and 6s window. At hand end every hand is
revealed, settlement shows per-seat money deltas + running balances + hand badges
(fully-concealed / limit / doubles), the dealer/wind rotate, and a bankruptcy banner
appears when a seat hits $0. Engine, scoring, settlement, pung turn-transfer, and the
interactive human loop are the real rulebook implementation. The settlement screen
also shows the **full §6 scoring math for the winning hand**: each meld's base
points, flowers, the 1% Mahjong bonus, every double applied with its ×2 factor,
the running multiplier, the limit cap, and the money conversion per opponent.
```
