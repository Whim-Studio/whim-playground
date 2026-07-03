# Ticket to Ride: Europe

A standalone Java 8 Swing adaptation of *Ticket to Ride: Europe*. Plain `javac`
build, zero external dependencies (only `java.util`, `java.awt`, `javax.swing`).
Supports 2–5 players and enforces the Europe-specific rules: **ferries**
(locomotive requirements), **tunnels** (mid-claim risk flip of 3 cards), and
**train stations** (borrow one opponent route for end-game ticket paths).

Built in three parallel tasks against a frozen `api` contract:
**(1) domain & state**, **(2) rules engine, pathfinding & scoring**,
**(3) Swing UI & app**. See [`CONTRACT.md`](CONTRACT.md).

## Build & run

```sh
find ticket-to-ride/src -name '*.java' > /tmp/ttr-srcs.txt
javac -source 1.8 -target 1.8 -d /tmp/ttr-out @/tmp/ttr-srcs.txt
java -cp /tmp/ttr-out com.whim.ttr.app.Main
```

## Package layout

```
com.whim.ttr.api      frozen contract: enums, GameConstants, GameEngine, ActionOutcome
com.whim.ttr.domain   City, Route, Board (full Europe map), Player, Deck,
                      TrainCard, DestinationTicket, GameState, PlayerScore
com.whim.ttr.engine   RulesEngine: claim/ferry/tunnel/station validation,
                      DFS ticket scoring (with station borrowing), longest path
com.whim.ttr.ui       BoardPanel (procedural Graphics2D map) + dashboard + dialogs
com.whim.ttr.app      Main entry point
```
