# Ticket to Ride: Europe — Build Contract

Standalone Java 8 Swing adaptation. **Zero external libraries** — only
`java.util`, `java.awt`, `javax.swing`. Plain `javac`, no Maven/Gradle. No
Java 9+ syntax (no `var`, switch expressions, text blocks, or post-8 collectors).

Package root: `com.whim.ttr` under `ticket-to-ride/src/`.

```
com.whim.ttr.api      OWNED BY ORCHESTRATOR — frozen contract. Do not modify.
com.whim.ttr.domain   TASK 1 — pure data + state (no engine/UI imports)
com.whim.ttr.engine   TASK 2 — rules, validation, pathfinding, scoring
com.whim.ttr.ui       TASK 3 — Swing panels, procedural Graphics2D board
com.whim.ttr.app      TASK 3 — Main entry point
```

**Hard rules for every task**
- Only create/modify files inside *your* package(s). Never edit `api/`.
- Compile against the signatures below, not against another task's private fields.
- Full-tree compilation happens at orchestrator integration; you are not blocked
  waiting on another task's files.
- Report results/blockers back to the orchestrator via `send_prompt`, then push
  your branch and open a PR.

---

## `api` package (frozen — already on the branch)

- `CardColor` enum: PURPLE, BLUE, ORANGE, WHITE, GREEN, YELLOW, BLACK, RED, LOCOMOTIVE. `isLocomotive()`, `trainColors()`.
- `RouteKind` enum: NORMAL, FERRY, TUNNEL.
- `GamePhase` enum: SETUP, PLAYING, LAST_ROUND, GAME_OVER.
- `GameConstants`: 45 trains, 3 stations, station cost {1,2,3}, +4/unused station,
  deck 110 (8×12 + 14 loco), 5 face-up, 3-loco reshuffle, tunnel flip 3,
  hand 4, tickets {start 3 keep≥2; normal 3 keep≥1}, endgame ≤2 trains,
  longest-path +10, ROUTE_POINTS[len] = {0,1,2,4,7,10,15,18,21}, 2–5 players.
- `ActionOutcome`: `isSuccess()`, `getMessage()`, `isAwaitingTunnel()`,
  `getTunnelDraw()`, `getTunnelExtra()`; factories `of(bool,msg)`, `tunnel(draw,extra)`.
- `GameEngine`: the interface Task 2 implements and Task 3 consumes (see source).

---

## TASK 1 — `com.whim.ttr.domain` (exact signatures the others rely on)

Create these public types. Fields shown as the accessors other tasks will call.

### `City`
```java
String id();          // stable key, e.g. "Paris"
String name();        // display name
int x();  int y();     // board layout coords (0..1000 virtual space)
```

### `Route`  (an edge; a double route is two Route objects with the same endpoints)
```java
String id();                 // stable unique key, e.g. "Paris-Frankfurt-1"
String cityA();  String cityB();
int length();                // 1..8
CardColor color();           // null == GRAY (any single color)
RouteKind kind();            // NORMAL / FERRY / TUNNEL
int locomotivesRequired();   // ferries: min locomotives in payment (else 0)
Integer ownerId();           // null if unclaimed, else player seat index
```

### `Board`  (the graph; builds the full official Europe map)
```java
Board();                              // constructs all cities + routes
Collection<City> cities();
City city(String id);
Collection<Route> routes();
Route route(String id);
List<Route> routesFrom(String cityId);
List<Route> routesBetween(String a, String b);   // handles double routes
```
Must contain the real Ticket to Ride: Europe map: ~47 cities and the full route
list with correct colors, lengths, ferries (locomotive requirements) and the
tunnels. If any single route is uncertain, approximate but keep the graph
connected and playable; note approximations in the PR.

### `TrainCard` — trivial wrapper: `CardColor color()`.

### `DestinationTicket`
```java
String from();  String to();  int points();
```

### `Player`
```java
int id();  String name();  java.awt.Color token();
int trainsLeft();       // starts 45
int stationsLeft();     // starts 3
List<CardColor> hand(); // train cards in hand
List<DestinationTicket> tickets();
List<String> stationCities();   // cities where this player built a station
int score();            // running claimed-route score
// mutators used by the engine:
void addCard(CardColor c); boolean removeCards(CardColor c, int n);
void useTrains(int n); void useStation(String cityId);
void addTicket(DestinationTicket t); void addScore(int pts);
```

### `Deck`
```java
Deck(long seed);                 // deterministic shuffle for testing
CardColor draw();                // reshuffles discards when empty
void discard(CardColor c);
List<CardColor> faceUp();        // size 5; auto-redeals on 3 locomotives
void refillFaceUp();
List<DestinationTicket> ticketDeck();  // the official ticket list, shuffled
DestinationTicket drawTicket();
```

### `GameState`  (single shared aggregate; engine mutates, UI reads)
```java
GameState(List<Player> players, Board board, Deck deck);
Board board();  Deck deck();
List<Player> players();  Player player(int id);
int currentPlayerId();  void setCurrentPlayerId(int id);
GamePhase phase();  void setPhase(GamePhase p);
String lastMessage(); void setLastMessage(String m);
```

### `PlayerScore` (final breakdown carrier)
```java
int playerId(); int routePoints(); int ticketPoints();
int stationBonus(); boolean hasLongestPath(); int total();
List<String> completedTickets(); List<String> failedTickets();
```

---

## TASK 2 — `com.whim.ttr.engine`

Implement `com.whim.ttr.api.GameEngine` (class e.g. `RulesEngine`).

- **Claim validation**: enough cards of the route color (or LOCOMOTIVE wilds);
  GRAY routes accept any one color; FERRY routes require ≥ `locomotivesRequired`
  locomotives in the payment; enforce train supply and double-route rules
  (a player may not own both routes of a double route in a 2–3 player game).
- **Tunnel**: on a TUNNEL claim, flip `GameConstants.TUNNEL_FLIP` cards; count
  those matching the paid color or LOCOMOTIVE; require that many extra matching
  cards. Return `ActionOutcome.tunnel(draw, extra)` and hold a pending claim
  resolved by `confirmTunnel`/`cancelTunnel`.
- **Stations**: cost `STATION_COST[stationsUsed]`; a built station lets the owner
  borrow exactly ONE opponent route at that city for end-game ticket paths only.
- **DFS ticket scoring**: at game end, for each player build the subgraph of their
  claimed routes plus (optimally) one borrowed opponent edge per station, then
  DFS to decide which `DestinationTicket`s connect. Choose station-borrow edges to
  maximize completed-ticket value.
- **Longest path (European Express)**: longest continuous trail over a player's
  claimed edges (edges not reused); award +10 to the max (ties: all tied get it).
- **Endgame**: arm LAST_ROUND when a player hits ≤2 trains; everyone gets one more
  turn; then GAME_OVER and `finalScores()`.
- Keep all graph work synchronous behind the interface (UI runs it off the EDT).

---

## TASK 3 — `com.whim.ttr.ui` + `com.whim.ttr.app`

- Consume **only** `api.GameEngine` + read `domain.GameState`. Do not import
  `engine` except a single wiring line in `app.Main`.
- `BoardPanel` (JPanel): draw the map procedurally with `Graphics2D` — cities as
  labeled circular nodes at `City.x()/y()`, routes as segmented colored boxes
  (parallel offset for double routes), owned routes recolored to the owner token.
  Click a route to attempt a claim; click a city to build a station.
- Bottom dashboard: active player's hand (grouped by color w/ counts), their
  tickets, the 5 face-up cards + deck (click-to-draw), station/trains left, score,
  a turn/status line fed from `GameState.lastMessage()`, and End Turn.
- Tunnel prompt + ticket-selection use modal `JDialog`s. Never block the EDT with
  engine work — run engine calls on a worker and update Swing on `invokeLater`.
- `app.Main`: build Board→Deck→Players→GameState→RulesEngine→GameFrame; support
  2–5 players (a simple start dialog is fine). Runnable via the README's `javac`.

---

## Build (orchestrator integration)
```sh
find ticket-to-ride/src -name '*.java' > /tmp/ttr-srcs.txt
javac -source 1.8 -target 1.8 -d /tmp/ttr-out @/tmp/ttr-srcs.txt
java -cp /tmp/ttr-out com.whim.ttr.app.Main
```
