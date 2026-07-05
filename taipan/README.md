# Taipan! — Java 8 / Swing recreation

A faithful, single-player desktop recreation of the 1979 Far East trading game
**Taipan!** (Art Canfil), built with **Java 8 + Swing only** — no JavaFX, no
third-party libraries, no network, no external assets.

You are a *Taipan* (merchant lord) sailing your lorcha between seven Asian ports,
buying and selling four goods at fluctuating prices, juggling a debt to Elder
Brother Wu, dodging pirates, storms and warlord Li Yuen, upgrading your ship —
and retiring with the greatest net worth you can amass.

---

## Build

Requires a JDK 8 or newer on your `PATH` (the code compiles at Java 8 level).

```bash
./build.sh
```

or manually:

```bash
mkdir -p out
find src -name '*.java' > sources.txt
javac --release 8 -d out @sources.txt      # or: javac -source 8 -target 8 ...
jar cfe taipan.jar com.taipan.Main -C out .
```

This produces a single runnable **`taipan.jar`**.

> A build tool (Maven/Gradle) was intentionally *not* used: the project has zero
> external dependencies, so plain `javac`/`jar` is the simplest reproducible path.

## Run

```bash
java -jar taipan.jar
```

(A graphical desktop is required — this is a Swing app.)

---

## Controls / how to play

- **New Game screen** — enter your name and firm, then *Set Sail*. (Optional: tick
  the debug box and enter a fixed random seed for reproducible runs.)
- **Port screen** — your hub. Shows the port, date, cash/bank/debt/net worth and
  ship status, plus a live price table.
  - **Buy / Sell** — click next to a good and enter a quantity. Invalid amounts
    (too expensive, no hold space, more than you own) show an error and let you retry.
  - **Bank & Debt** *(Hong Kong only)* — deposit/withdraw safe money, borrow from
    or repay Elder Brother Wu.
  - **Shipyard** *(Hong Kong only)* — repair hull damage (McHenry), enlarge your
    cargo hold, or buy guns.
  - **Set Sail (Travel)** — choose your next port. Every voyage advances one month.
  - **Retire** — end the game and see your final net-worth report and rank.
- **At sea**, random events resolve automatically or prompt you:
  - **Li Yuen** may demand tribute — pay for one voyage of protection, or refuse.
  - **Pirates** trigger the **combat screen**: *Fight* (fire your guns), *Run*
    (escape chance rises each round), or *Throw Cargo* (jettison to appease them).
  - **Storms** damage your hull and can blow you off course to another port.
  - Carrying **opium** risks seizure and a fine.

The game ends when you **retire** or your **ship is sunk** (hull damage reaches 100%).

---

## Architecture

Clean Model / Controller / View separation; no business logic in UI handlers
beyond delegation.

```
src/com/taipan/
  Main.java                     entry point (launches Swing on the EDT)
  model/                        pure state, no logic beyond derived getters
    Good.java  PortCity.java  Ship.java  GameState.java  GameConstants.java
  controller/                   all rules: prices, trading, banking, voyages, combat, scoring
    GameController.java  CombatSession.java  VoyageResult.java
  view/                         Swing screens, switched via CardLayout
    GameFrame.java  NewGamePanel.java  PortPanel.java  TravelPanel.java
    CombatPanel.java  EndPanel.java  HarborCanvas.java
```

Screen flow (single `JFrame` + `CardLayout`):

```
NewGame → Port ⇄ Travel → (Li Yuen? / Combat?) → Port
                                   ↓ sunk / retire
                                  End → NewGame
```

- **Randomness:** `java.util.Random`, unseeded by default; the New Game screen
  exposes an optional fixed seed for debugging/testability.
- **No unhandled exceptions reach the player:** every player action returns a
  human-readable error string on invalid input rather than throwing; the UI shows
  it and lets the player retry.
- **Placeholder art** is drawn programmatically with `Graphics2D`
  (`HarborCanvas`) — no copyrighted or downloaded images.

---

## Chosen ruleset (research summary)

Numbers were reconciled from two independent descriptions of the original — Jay
Link's widely-distributed C port `taipan` (derived from the Apple II original by
Ronald Cain / Karl Hassel) and community playthrough/wiki descriptions of the
Apple II and MS-DOS versions. Where sources disagreed, an internally-consistent,
sitting-length-playable value was chosen. All constants live in
[`GameConstants.java`](src/com/taipan/model/GameConstants.java).

- **Ports (7):** Hong Kong (home), Shanghai, Nagasaki, Saigon, Manila, Singapore,
  Batavia. Travel is chosen freely each turn; any voyage = one month.
- **Goods (4):** General Cargo (base $90), Arms ($250), Silk ($500), Opium ($1000).
  Prices roll each port visit within ~0.45×–2.6× of base, with an ~18% chance of a
  "glut/shortage" price event pushing well beyond that band (with flavour text).
- **Ship:** starts 60 hold units, 5 guns; each gun occupies 10 hold units. Enlarge
  the hold (+10 for $5000) and buy guns ($1000) at the Hong Kong shipyard.
- **Finances:** start with $400 cash and $5000 debt to Elder Brother Wu. Debt grows
  **10%/month**; the Hong Kong bank keeps money safe from pirates (and pays a token
  0.5%). Borrow/repay/deposit/withdraw at Hong Kong only.
- **Events:** pirates (~40%), storms (~20%), Li Yuen tribute (~20%), opium seizure
  (~12% when carrying opium).
- **Combat:** each gun hits at ~65%; each hit has ~35% to sink an enemy ship.
  Enemies return fire for ~1–4% hull damage per surviving ship per round. Ships may
  flee; you may fight, run (escape odds rise each round) or throw cargo. Winning
  yields booty scaled to the number of attackers.
- **End / scoring:** no forced time limit — retire voluntarily at any port, or lose
  by sinking. Final **net worth = cash + bank − debt + cargo (at base value)**,
  reported with a rank from *Penniless Peddler* up to *Ma Tsu (Taipan!)* at
  $1,000,000.

---

## Faithfulness notes (deliberate deviations)

- **Menu-driven → graphical widgets.** The original is a text terminal with
  numbered prompts; this version keeps the same interaction *model* (a port hub
  with explicit choices, quantity prompts, an interactive battle) but uses Swing
  buttons/dialogs instead of typed menu numbers.
- **Exact prices/odds are a chosen, documented ruleset**, not a byte-for-byte copy
  of any one port — the historical versions themselves disagree. All values are in
  one file so they are easy to inspect or tune.
- **Combat math is simplified** to a transparent per-gun hit/sink model rather than
  the original's exact tables, while preserving the fight/run/throw-cargo choices
  and the guns-vs-hold-space trade-off.
- **Bank pays a token interest** (the original's bank is mainly a safe store); this
  is minor flavour and can be set to 0 in `GameConstants`.
- **Save/Load** of a game in progress was scoped as a stretch goal and is **not**
  implemented; the core loop is complete and stable instead.
- **No original artwork** is used; the harbour scene is placeholder vector art.
```
