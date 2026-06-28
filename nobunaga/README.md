# Nobunaga's Ambition: Zenkokuban

A standalone **Java 8 + Swing** adaptation of the 1986 grand-strategy classic
*Nobunaga's Ambition: Zenkokuban (Country-Wide Edition)*. A turn-based Sengoku
unification sim: 50 provinces, fief economy, peasant loyalty and rebellions,
random disasters, and grid-based tactical battles. No external libraries and no
build system — every visual is drawn procedurally with `Graphics2D` (no image
files are ever loaded).

## Build & run

```bash
cd nobunaga
javac -d out $(find src -name "*.java")
java -cp out com.whim.nobunaga.app.Main
```

This compiles every source file under `src/` into `out/` and launches the game.
A Java 8 (or newer) JDK is required.

Engine self-check (no UI):

```bash
java -cp out com.whim.nobunaga.engine.EngineSmokeTest
```

## How to play

1. **Start screen** — pick one of the eight roster daimyo (each shown with a
   procedurally drawn clan flag). Oda Nobunaga is the default.
2. **Map** — the realm is laid out as 50 provinces. Each province is tinted by
   its owner's clan color (gray = neutral), connected to its neighbors by
   adjacency lines, and labeled with the clan abbreviation and stationed soldier
   count. **Click a province** to select it.
3. **Dashboard (right)** — a dense monospaced readout of the selected province:
   owner, Gold, Rice, Loyalty, Tax %, Flood control, Wealth (cultivation),
   Soldiers, and its adjacency list.
4. **Actions (bottom)** — operate on the selected province:

   | Button          | Effect                                                         |
   | --------------- | -------------------------------------------------------------- |
   | **Tax**         | Set the province tax rate (0–100). Higher tax, lower loyalty.  |
   | **Cultivate**   | Spend gold to raise Wealth (cultivation → more gold + rice).   |
   | **Flood Control** | Spend gold to raise flood control (mitigates disasters).     |
   | **Recruit**     | Spend gold + rice to raise soldiers.                           |
   | **Transfer**    | Move gold / rice / soldiers to a friendly adjacent province.   |
   | **War**         | Attack an adjacent enemy/neutral province → tactical battle.   |
   | **End Season**  | Run economy, disasters, rebellions and rival-AI for everyone,  |
   |                 | then advance the season clock. Results appear in the log.      |

   Every action shows the engine's one-line result in the scrolling log and then
   repaints the map and dashboard.
5. **Battle** — select a target enemy/neutral province, choose which of your
   adjacent provinces attacks and how many soldiers / how much rice to commit.
   On the `14×10` battle grid:
   - Click one of **your units** (highlighted), then click a target tile to queue
     its order for the next day.
   - **Next Day »** resolves a day: orders execute, melee is fought, daily
     supplies burn, and victory is checked.
   - When the battle is decided, the outcome is applied (troop losses; the
     province changes hands if the attacker won) and the result is shown.

The whole run is reproducible from its seed: a single shared `Random` (owned by
the domain `GameState`) drives every disaster, battle and AI decision.

## Architecture

Three packages, split for a parallel build against a fixed interface contract
(`NOBUNAGA_CONTRACT.md` at the repo root):

- `com.whim.nobunaga.domain` / `.map` — core model, the `GameEngine` interface,
  `GameLoopManager`, and the `ProvinceData` world factory.
- `com.whim.nobunaga.engine` — pure game-rule logic implementing `GameEngine`
  (economy, events, battle resolution, rival AI). No Swing/AWT.
- `com.whim.nobunaga.ui` / `.app` — this layer. Swing only; it reads domain state
  and calls the engine through a thin `GameController`. It never computes a game
  rule itself.

UI files: `StartScreen`, `GameFrame`, `MapPanel`, `DashboardPanel`, `ActionPanel`,
`BattlePanel`, `GameController`, and the `Main` EDT bootstrap.
