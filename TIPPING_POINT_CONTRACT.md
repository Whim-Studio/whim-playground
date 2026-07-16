# Tipping Point — Shared Interface Contract (Java 8)

Standalone Swing app: a faithful-in-spirit, rule-enforced digital adaptation of the
2020 environmental board game **Tipping Point** (designed by Ryan Smith / Treecer).
City-building economy coupled to a compounding global-CO₂ climate feedback loop:
as cities grow they emit CO₂, rising CO₂ escalates extreme-weather disasters, and
crossing the global tipping point is a **collective loss for everyone**.

- App dir: `tippingpoint/`
- Source root: `tippingpoint/src/` (plain layout, package dirs under it — e.g. `tippingpoint/src/com/whim/tippingpoint/domain/Player.java`)
- Base package: `com.whim.tippingpoint`
- **Java 8 ONLY.** No `var`, no text blocks, no switch expressions, no records, no `Stream` collectors that postdate Java 8, no `List.of`/`Map.of`. Plain Java 8 only. (Developed compiling with `javac --release 8`.)
- **No external libraries.** Only `javax.swing`, `java.awt`, `java.util`, `java.io`, `java.nio`. No Maven/Gradle/Gson.
- Compile: `javac --release 8 -d out $(find tippingpoint/src -name '*.java')`. Run: `java -cp out com.whim.tippingpoint.app.Main`.

## File ownership (NO overlap between tasks)

- **Task 1 (domain + data)** owns `tippingpoint/src/com/whim/tippingpoint/domain/**`.
- **Task 2 (engine + rules)** owns `tippingpoint/src/com/whim/tippingpoint/engine/**`.
- **Task 3 (Swing UI)** owns `tippingpoint/src/com/whim/tippingpoint/ui/**`.
- **Main class** (`tippingpoint/src/com/whim/tippingpoint/app/Main.java`) is written by the **orchestrator** during consolidation. Do NOT create it.

Tasks 2 and 3 MUST code against the `domain` interfaces below **verbatim** and MUST NOT create their own copies of `domain` types. Task 3 codes against the `engine` interfaces below **verbatim**. The orchestrator compiles the whole project during consolidation.

> The contract fixes **public signatures, constants, and rules**, not internal implementation. Concrete classes named `Default*` are authored by the owning task with the stated public constructors. Any extra helper type must live inside that task's own package and must not change a signature below.

---

## Ruleset this engine MUST enforce

**Players:** 2–4. Modes: `COMPETITIVE` or `COOPERATIVE`. Any player may be human or AI.

**Status Board (per player)** tracks three cylinders plus a derived risk factor:
- `cashFlow` — income (in $) granted to the player at the start of each Development Phase. Starts at `Rules.START_CASH_FLOW`.
- `foodProduction` — total food yield available to feed citizens each Weather Phase. Starts at `Rules.START_FOOD`.
- `co2` — this city's net CO₂ contribution (never below 0). Starts at 0.
- **Risk Factor** (derived from this city's own `co2`): `X1` while `co2 <= Rules.RISK_X2_AT-1`, `X2` while `co2 <= Rules.RISK_X3_AT-1`, else `X3`. Its `multiplier()` scales weather damage dealt to that city.

**Global CO₂** = sum of every player's `co2`. Drives the Weather Phase.

**Cards — three types:**
- **Citizen Card** — a population of 1. `WORKER` citizens each consume 1 food per Weather Phase; `FARMER` citizens consume 0 food and, when recruited, raise `foodProduction` by `Rules.FARMER_FOOD_YIELD`. Recruiting a citizen costs `Rules.CITIZEN_COST` cash.
- **Development Card** — bought from the 3×4 Central Market. Each has a cash `cost` and applies deltas to the buyer's Status Board (`cashFlowDelta`, `foodDelta`, `co2Delta`). `GREEN` developments reduce CO₂ (`co2Delta < 0`); `INDUSTRIAL` developments grow the economy but emit (`co2Delta > 0`); `INFRASTRUCTURE` is roughly CO₂-neutral. Bought cards go to the city tableau permanently (their deltas persist).
- **Weather Card** — an extreme-weather disaster with a `severity` and base damage (`citizensLost`, `cashLost`, `foodProductionLost`). When resolved against a player the base magnitudes are multiplied by that player's Risk Factor multiplier.

**Turn structure — one Round = Development Phase then Weather Phase:**

*Development Phase* (players act in clockwise order):
1. Player gains `cashFlow` cash (added to `Player.cash`).
2. Player may repeatedly: recruit a citizen (`WORKER` or `FARMER`, pay `CITIZEN_COST`), or buy a Development Card from any filled market slot they can afford. Each purchase deducts cash immediately and applies the card's Status-Board deltas. Bought market slots are refilled from the development deck at end of that player's sub-turn (or when the phase ends).
3. Player ends their sub-turn. When all players have acted, the phase ends and the Market is fully refilled.

*Weather Phase* (resolved once for the table, in this order):
1. **Feed citizens.** For each player: food need = number of `WORKER` citizens; supply = `foodProduction`. Shortfall = `max(0, need - supply)`; that many workers starve and are removed (farmers are removed only if no workers remain).
2. **Clear** the previous round's revealed weather cards.
3. **Compute reveal count** = `min(Rules.MAX_WEATHER_CARDS, 1 + globalCo2 / Rules.CO2_PER_EXTRA_CARD)`.
4. **Reveal** that many Weather Cards from the weather deck and **resolve each against every player**, scaling base damage by the player's Risk Factor multiplier (citizens lost, cash lost — cash floored at 0, foodProduction lost — floored at 0).
5. **Advance the Timeline** by `Rules.YEARS_PER_ROUND` years.
6. **Check win/loss** (see below).

**Victory & Defeat (checked at end of Weather Phase):**
- **Collective defeat (tipping point):** if `globalCo2 >= Rules.TIPPING_POINT_CO2`, everyone loses immediately, regardless of mode.
- **Competitive win:** the first player to reach `Rules.TARGET_POPULATION` citizens wins immediately. If the Timeline reaches `Rules.END_YEAR` (2100) first, the game ends and the player with the highest population wins (ties broken by most cash, then lowest CO₂).
- **Cooperative win:** if the Timeline reaches `Rules.END_YEAR` with **every** player at `>= Rules.TARGET_POPULATION` population and the tipping point not crossed, the team wins; otherwise the team loses (ran out of time).

---

## `com.whim.tippingpoint.domain` — authored by Task 1

### Constants — `Rules`

```java
public final class Rules {
    private Rules() {}
    public static final int   MIN_PLAYERS = 2;
    public static final int   MAX_PLAYERS = 4;
    public static final int   START_CASH_FLOW = 3;      // $ granted each Development Phase
    public static final int   START_CASH = 5;           // starting bank per player
    public static final int   START_FOOD = 2;           // starting foodProduction
    public static final int   CITIZEN_COST = 2;         // cash to recruit any citizen
    public static final int   FARMER_FOOD_YIELD = 2;    // foodProduction added per farmer recruited
    public static final int   RISK_X2_AT = 4;           // co2 >= 4 -> at least X2
    public static final int   RISK_X3_AT = 8;           // co2 >= 8 -> X3
    public static final int   CO2_PER_EXTRA_CARD = 5;   // weather cards = 1 + globalCo2/5
    public static final int   MAX_WEATHER_CARDS = 5;
    public static final int   TIPPING_POINT_CO2 = 30;   // global; >= is collective loss
    public static final int   TARGET_POPULATION = 20;   // citizens to win
    public static final int   START_YEAR = 2020;
    public static final int   END_YEAR = 2100;
    public static final int   YEARS_PER_ROUND = 10;
    public static final int   MARKET_ROWS = 3;
    public static final int   MARKET_COLS = 4;          // 3x4 = 12 face-up developments
}
```

### Enums

```java
public enum GameMode { COMPETITIVE, COOPERATIVE }
public enum Phase { DEVELOPMENT, WEATHER }
public enum CitizenType { WORKER, FARMER }
public enum DevelopmentType { GREEN, INDUSTRIAL, INFRASTRUCTURE }
public enum WeatherSeverity { MILD, MODERATE, SEVERE }

public enum RiskFactor {
    X1(1), X2(2), X3(3);
    private final int multiplier;
    RiskFactor(int m) { this.multiplier = m; }
    public int multiplier() { return multiplier; }
    // co2 -> risk factor, per Rules thresholds
    public static RiskFactor forCo2(int co2) {
        if (co2 >= Rules.RISK_X3_AT) return X3;
        if (co2 >= Rules.RISK_X2_AT) return X2;
        return X1;
    }
}
```

### Cards (inheritance hierarchy)

```java
public abstract class Card {
    protected final String id;
    protected final String name;
    protected final String description;
    protected Card(String id, String name, String description) { this.id=id; this.name=name; this.description=description; }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}

public final class CitizenCard extends Card {
    private final CitizenType type;
    public CitizenCard(String id, String name, CitizenType type) { super(id, name, type + " citizen"); this.type = type; }
    public CitizenType getType() { return type; }
    public boolean isFarmer() { return type == CitizenType.FARMER; }
}

public final class DevelopmentCard extends Card {
    private final DevelopmentType type;
    private final int cost, cashFlowDelta, foodDelta, co2Delta;
    public DevelopmentCard(String id, String name, DevelopmentType type,
                           int cost, int cashFlowDelta, int foodDelta, int co2Delta) {
        super(id, name, type + " development");
        this.type=type; this.cost=cost; this.cashFlowDelta=cashFlowDelta; this.foodDelta=foodDelta; this.co2Delta=co2Delta;
    }
    public DevelopmentType getType() { return type; }
    public int getCost() { return cost; }
    public int getCashFlowDelta() { return cashFlowDelta; }
    public int getFoodDelta() { return foodDelta; }
    public int getCo2Delta() { return co2Delta; }
}

public final class WeatherCard extends Card {
    private final WeatherSeverity severity;
    private final int citizensLost, cashLost, foodProductionLost;
    public WeatherCard(String id, String name, WeatherSeverity severity,
                       int citizensLost, int cashLost, int foodProductionLost) {
        super(id, name, severity + " weather");
        this.severity=severity; this.citizensLost=citizensLost; this.cashLost=cashLost; this.foodProductionLost=foodProductionLost;
    }
    public WeatherSeverity getSeverity() { return severity; }
    public int getCitizensLost() { return citizensLost; }
    public int getCashLost() { return cashLost; }
    public int getFoodProductionLost() { return foodProductionLost; }
}
```

### `StatusBoard`

```java
public final class StatusBoard {
    public StatusBoard();                 // cashFlow=START_CASH_FLOW, foodProduction=START_FOOD, co2=0
    public int getCashFlow();
    public int getFoodProduction();
    public int getCo2();                  // never < 0
    public RiskFactor getRiskFactor();    // RiskFactor.forCo2(co2)
    public void applyDevelopment(DevelopmentCard c); // cashFlow+=cashFlowDelta; foodProduction+=foodDelta; co2 = max(0, co2+co2Delta)
    public void addFoodProduction(int d); // for farmer recruit; floors at 0
    public void loseFoodProduction(int d);// floors at 0
    public void setCo2(int v);            // floors at 0
}
```

### `CityTableau`

```java
public final class CityTableau {
    public CityTableau();
    public java.util.List<CitizenCard> getCitizens();       // live list
    public java.util.List<DevelopmentCard> getDevelopments();
    public void addCitizen(CitizenCard c);
    public void addDevelopment(DevelopmentCard c);
    public int populationCount();                            // total citizens
    public int workerCount();                                // WORKER citizens
    public int farmerCount();                                // FARMER citizens
    public void removeCitizens(int n);                       // remove up to n, WORKERS first then farmers
}
```

### `Player`

```java
public final class Player {
    public Player(String name, boolean ai);
    public String getName();
    public boolean isAi();
    public int getCash();                 // starts Rules.START_CASH
    public void addCash(int d);
    public void spendCash(int d);         // caller must ensure affordable
    public boolean canAfford(int cost);
    public StatusBoard getBoard();
    public CityTableau getCity();
    public int getPopulation();           // convenience == city.populationCount()
}
```

### `Market` (3×4 face-up developments)

```java
public final class Market {
    public Market(java.util.List<DevelopmentCard> deck);    // deck is drawn from to fill slots
    public int rows();                                      // Rules.MARKET_ROWS
    public int cols();                                      // Rules.MARKET_COLS
    public DevelopmentCard get(int row, int col);           // null if empty slot
    public DevelopmentCard take(int row, int col);          // remove & return the card in that slot (leaves slot null)
    public void refill();                                   // fill every null slot from remaining deck (if any)
    public boolean deckEmpty();
}
```

### `TimelineTrack`

```java
public final class TimelineTrack {
    public TimelineTrack();               // START_YEAR
    public int getYear();
    public void advance();                // += Rules.YEARS_PER_ROUND
    public boolean isAtEnd();             // year >= Rules.END_YEAR
}
```

### `GameState`

```java
public final class GameState {
    public GameState(java.util.List<Player> players, Market market, TimelineTrack timeline,
                     java.util.List<WeatherCard> weatherDeck, GameMode mode);
    public java.util.List<Player> getPlayers();
    public Market getMarket();
    public TimelineTrack getTimeline();
    public GameMode getMode();
    public java.util.List<WeatherCard> getWeatherDeck();    // draw pile (mutable)
    public java.util.List<WeatherCard> getRevealedWeather(); // this round's revealed cards (engine sets)
    public int getGlobalCo2();                              // sum of players' co2
    public Phase getPhase();
    public void setPhase(Phase p);
    public int getCurrentPlayerIndex();
    public void setCurrentPlayerIndex(int i);
    public Player getCurrentPlayer();
    public int getRound();                                  // 1-based
    public void incrementRound();
}
```

### `CardData` (deck factories — Task 1)

```java
public final class CardData {
    private CardData();
    // A tuned development deck (>= 24 cards) mixing GREEN / INDUSTRIAL / INFRASTRUCTURE so the
    // game is winnable: greens must let players cut CO2 while growing. Order is shuffled by engine.
    public static java.util.List<DevelopmentCard> developmentDeck();
    // A weather deck (>= 16 cards) spanning MILD/MODERATE/SEVERE with base damages.
    public static java.util.List<WeatherCard> weatherDeck();
    // Fresh unique citizen cards to recruit (id-stamped).
    public static CitizenCard newCitizen(CitizenType type);
}
```

---

## `com.whim.tippingpoint.engine` — authored by Task 2

Task 2 depends ONLY on `domain`. It owns all rule enforcement, phase resolution, AI, and win/loss detection. Task 3 (UI) codes against these interfaces verbatim.

```java
package com.whim.tippingpoint.engine;
import com.whim.tippingpoint.domain.*;
import java.util.List;

public enum Outcome { IN_PROGRESS, PLAYER_WIN, TEAM_WIN, TEAM_LOSS_TIPPING, TEAM_LOSS_TIME }

// Immutable record of what a resolved Weather Phase did, for the UI log.
public final class WeatherReport {
    public WeatherReport(List<WeatherCard> revealed, int globalCo2, List<String> lines);
    public List<WeatherCard> getRevealed();
    public int getGlobalCo2();
    public List<String> getLines();   // human-readable per-player effect lines
}

public final class WinStatus {
    public WinStatus(Outcome outcome, Player winner /*nullable*/, String message);
    public Outcome getOutcome();
    public Player getWinner();         // null unless PLAYER_WIN
    public String getMessage();
    public boolean isOver();           // outcome != IN_PROGRESS
}

public interface GameEngine {
    GameState state();

    // ----- Development Phase -----
    void beginDevelopmentPhase();                 // grants cashFlow to the player whose sub-turn is starting as needed
    boolean canBuyDevelopment(Player p, int row, int col);
    void buyDevelopment(Player p, int row, int col);   // validates, deducts, applies deltas, refills slot
    boolean canRecruit(Player p, CitizenType type);
    void recruit(Player p, CitizenType type);          // validates cost, adds citizen, farmer raises foodProduction
    void endDevelopmentTurn();                     // advance to next player; when all done, refill market & move to Weather Phase
    boolean developmentPhaseComplete();

    // ----- AI -----
    void runAiDevelopmentTurn(Player p);           // engine plays a full sub-turn for an AI player

    // ----- Weather Phase -----
    WeatherReport resolveWeatherPhase();           // feed, clear, reveal, resolve, advance timeline

    // ----- Win/loss -----
    WinStatus checkStatus();
}

// Concrete engine. Shuffling uses the provided seed for reproducibility.
public final class DefaultGameEngine implements GameEngine {
    public static GameEngine newGame(List<String> playerNames, java.util.List<Boolean> isAi, GameMode mode, long seed);
    // ...implements GameEngine...
}
```

Notes for Task 2:
- `DefaultGameEngine.newGame` builds `Player`s (START_CASH etc.), a shuffled `Market` from `CardData.developmentDeck()`, a shuffled `TimelineTrack`+`weatherDeck`, wraps them in `GameState`, sets `Phase.DEVELOPMENT`, currentPlayerIndex 0, round 1.
- Use `new java.util.Random(seed)` for all shuffles/AI (Java-8 safe, deterministic). No `Math.random`.
- AI heuristic: keep foodProduction ≥ workers, prefer GREEN when own co2 is near a risk threshold or global co2 is high, otherwise grow economy/population toward TARGET_POPULATION.

---

## `com.whim.tippingpoint.ui` — authored by Task 3

Task 3 depends on `domain` + `engine`. It renders everything with `Graphics2D` (no external images).

- `GameFrame extends JFrame` — top-level window; constructs a `DefaultGameEngine.newGame(...)` (via a small new-game dialog for player count/names/mode) and hosts the board.
- **Procedural graphics only:** draw cards, wooden tracker cylinders, status boards, and the 3×4 market with geometric shapes, distinct colors per `DevelopmentType`/`WeatherSeverity`, and clean typography (`java.awt.Font`). No `ImageIO`, no asset files.
- **Layout:** Timeline Track across the top; 3×4 Central Market in the center; the active player's City Tableau + Status Board along the bottom; a compact opponents strip (their key stats) on a side.
- **Interaction:** `MouseListener`s to buy a market card, recruit `WORKER`/`FARMER`, and an "End Turn" / "Run Weather Phase" control that advances phases via the engine. Show the `WeatherReport` lines and `WinStatus` message in an on-screen log/overlay.
- The UI MUST only read state from `domain` objects and mutate the game exclusively through the `GameEngine` interface. It must not re-implement any rule.
- Provide `public static void run()` (builds and shows the frame on the EDT via `SwingUtilities.invokeLater`) so the orchestrator's `Main` can call `com.whim.tippingpoint.ui.GameFrame.run()` (or an equivalent documented no-arg entry point — state it in the PR).

---

## Reporting

When done, each task pushes its branch, opens a PR into `whim-wd-546`, and reports the PR link + any signature friction back to the orchestrator via `send_prompt` (NOT task comments).
