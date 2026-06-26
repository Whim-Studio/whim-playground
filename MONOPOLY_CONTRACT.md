# Monopoly — Shared Interface Contract (Java 8)

Standalone Swing app: a faithful, rule-enforced digital adaptation of the classic
board game *Monopoly*, **official standard ruleset only — zero house rules**.

- App dir: `monopoly/`
- Source root: `monopoly/src/` (plain layout, package dirs under it — e.g. `monopoly/src/com/whim/monopoly/domain/Player.java`)
- Base package: `com.whim.monopoly`
- **Java 8 ONLY.** No `var`, no text blocks, no switch expressions, no records, no `Stream` collectors that postdate Java 8. Plain Java 8 only.
- **No external libraries.** Only `javax.swing`, `java.awt`, `java.util`, `java.io`, `java.nio`. No Maven/Gradle/Gson at runtime.
- Compile: `javac -d out $(find monopoly/src -name '*.java')`. Run: `java -cp out com.whim.monopoly.app.Main`.

## File ownership (NO overlap between tasks)

- **Task 1 (domain + data)** owns `monopoly/src/com/whim/monopoly/domain/**` and `monopoly/src/com/whim/monopoly/data/**`.
- **Task 2 (engine + rules)** owns `monopoly/src/com/whim/monopoly/engine/**`.
- **Task 3 (Swing UI)** owns `monopoly/src/com/whim/monopoly/ui/**`.
- **Main class** (`monopoly/src/com/whim/monopoly/app/Main.java`) is written by the **orchestrator** during consolidation. Do NOT create it.

Tasks 2 and 3 MUST code against the interfaces below **verbatim** and MUST NOT create their own copies of `domain` types. Task 3 codes against the `engine` interfaces below **verbatim**. The orchestrator compiles the whole project during consolidation.

> The contract fixes **signatures and rules**, not internal implementation. Concrete classes named "Default*/Standard*" are authored by the owning task with the stated public constructors. If a task needs a small additional helper type, it must live inside that task's own package and must not change any signature below.

---

## Official ruleset this engine MUST enforce (no house rules)

- 40-space board; each player starts with **$1500**; 2–6 players.
- **Passing GO** collects **$200** (landing on GO is the same $200, no double).
- **Free Parking awards nothing.** No pot, no bonus.
- **Unowned property + decline to buy → mandatory auction** to ALL players (incl. the one who declined), bidding starts at any amount, highest bidder pays the bank and takes the deed. If everyone passes with no bid, it stays unowned.
- **Rent** is collected even while the owner is in Jail.
- **Street rent:** from the rent table `[base,1h,2h,3h,4h,hotel]`. Base rent is **doubled** when the owner holds the **entire color group and that street has 0 houses** (and is not mortgaged). Mortgaged property collects no rent.
- **Railroad rent:** $25/$50/$100/$200 for 1/2/3/4 railroads owned by that owner.
- **Utility rent:** owner has 1 utility → **4× dice roll**; both utilities → **10× dice roll** (dice = the roll that landed the player there; for the Chance "nearest utility" card, player re-rolls and pays 10×).
- **Even Build:** houses bought one at a time; you cannot build a 2nd house on a street until every street in the group has ≥1 house. Selling is the reverse (even). 32 houses / 12 hotels exist in the bank as a supply limit. Must own the full unmortgaged group to build. Hotel = 5th building (requires 4 houses on each group street first); building it returns its 4 houses to the bank.
- **Mortgage:** mortgage value = price/2. You cannot mortgage a street that has buildings (sell them first). Unmortgage cost = `Math.round(mortgageValue * 1.10)` (10% interest). Selling a building back to the bank returns **half** the house cost.
- **Jail entry:** landing on Go To Jail (index 30), drawing a Go-To-Jail card, or rolling **three consecutive doubles** in one turn. Jailed player does NOT pass GO; placed on index 10.
- **Jail exit:** use a Get-Out-of-Jail-Free card, **pay $50 before rolling**, or roll doubles on any of the next 3 turns. If still jailed after the 3rd failed roll, must pay $50 and move the rolled amount. A player rolling doubles to leave jail moves that amount but does **not** take another turn.
- **Doubles:** rolling doubles → move, resolve, then roll again (unless it sends you to jail). Third double in a turn → straight to jail.
- **Bankruptcy:** if a player owes more than cash and cannot raise it by mortgaging and selling buildings, they are bankrupt. Debt to a player → all assets (deeds, jail cards, cash) transfer to that creditor (mortgaged deeds transfer mortgaged; new owner may pay 10% to keep mortgaged or unmortgage). Debt to the bank → buildings sold to bank, deeds auctioned. Bankrupt player leaves the game. Last solvent player wins.
- Chance / Community Chest: 16 cards each (see Task 1 data spec).

---

## `com.whim.monopoly.domain` — authored by Task 1

### Enums

```java
public enum SpaceType {
    GO, STREET, RAILROAD, UTILITY, CHANCE, COMMUNITY_CHEST,
    TAX, JAIL, FREE_PARKING, GO_TO_JAIL
}

public enum ColorGroup {
    BROWN("Brown"), LIGHT_BLUE("Light Blue"), PINK("Pink"), ORANGE("Orange"),
    RED("Red"), YELLOW("Yellow"), GREEN("Green"), DARK_BLUE("Dark Blue");
    private final String label;
    ColorGroup(String label) { this.label = label; }
    public String getLabel() { return label; }
}

public enum Deck { CHANCE, COMMUNITY_CHEST }

public enum CardAction {
    COLLECT,                // +amount from bank
    PAY,                    // -amount to bank
    MOVE_TO_SPACE,          // advance to targetIndex; collect $200 if passing GO (engine decides)
    MOVE_BACK,              // move amount spaces backward; no GO pay
    GO_TO_JAIL,             // straight to jail, no GO
    GET_OUT_OF_JAIL_FREE,   // keep card
    NEAREST_RAILROAD,       // advance to next railroad; pay owner 2x rail rent (or buy/auction if unowned)
    NEAREST_UTILITY,        // advance to next utility; pay owner 10x a fresh dice roll (or buy/auction)
    STREET_REPAIRS,         // pay bank: amount per house + amount2 per hotel
    COLLECT_FROM_EACH,      // +amount from every other solvent player
    PAY_EACH                // -amount to every other solvent player
}
```

### Spaces

```java
public interface Space {
    int getIndex();            // 0..39
    String getName();
    SpaceType getType();
}

// Anything that can be owned: streets, railroads, utilities.
public interface OwnableSpace extends Space {
    int getPrice();
    int getMortgageValue();    // == price / 2
    int getUnmortgageCost();   // == Math.round(getMortgageValue() * 1.10)
}

public interface StreetSpace extends OwnableSpace {
    ColorGroup getColorGroup();
    int getHouseCost();        // cost per house AND per hotel
    int[] getRentTable();      // length 6: base,1h,2h,3h,4h,hotel (base = undoubled, unimproved)
}

public interface RailroadSpace extends OwnableSpace { }   // rent purely by count owned

public interface UtilitySpace extends OwnableSpace { }    // rent = multiplier * dice

public interface TaxSpace extends Space {
    int getTaxAmount();        // Income Tax (idx 4) = 200, Luxury Tax (idx 38) = 100
}

public interface CardSpace extends Space {
    Deck getDeck();
}
```

### Card

```java
public interface Card {
    Deck getDeck();
    String getText();          // the printed card text
    CardAction getAction();
    int getAmount();           // primary amount (per CardAction; per-house for STREET_REPAIRS)
    int getAmount2();          // secondary (per-hotel for STREET_REPAIRS; else 0)
    int getTargetIndex();      // for MOVE_TO_SPACE (else -1)
}
```

### Player (mutable state holder; engine mutates, UI reads)

```java
public interface Player {
    int getId();
    String getName();
    java.awt.Color getToken();         // distinct token color
    int getCash();
    void setCash(int cash);
    void addCash(int delta);           // may go negative transiently during debt resolution
    int getPosition();                 // 0..39
    void setPosition(int index);
    boolean isInJail();
    void setInJail(boolean jailed);
    int getJailTurns();                // failed doubles attempts so far (0..3)
    void setJailTurns(int n);
    int getJailCards();                // Get-Out-of-Jail-Free cards held
    void setJailCards(int n);
    boolean isBankrupt();
    void setBankrupt(boolean b);
    java.util.Set<Integer> getDeeds(); // mutable set of owned OwnableSpace indices
}
```

### Board

```java
public interface Board {
    int SIZE = 40;
    Space spaceAt(int index);                          // 0..39
    java.util.List<Space> spaces();                    // size 40, index order
    java.util.List<RailroadSpace> railroads();
    java.util.List<UtilitySpace> utilities();
    java.util.List<StreetSpace> streetsInGroup(ColorGroup group);
    int nextRailroadFrom(int index);                   // wraps; for NEAREST_RAILROAD
    int nextUtilityFrom(int index);                    // wraps; for NEAREST_UTILITY
}
```

### Concrete classes Task 1 MUST provide

- `DefaultPlayer implements Player` — `public DefaultPlayer(int id, String name, java.awt.Color token)` (starts cash 1500, position 0, not jailed).
- `StandardBoard implements Board` — `public StandardBoard()` builds the canonical 40 US-edition spaces with correct names, prices, color groups, rent tables, house costs.
- `data.Cards` — `public static java.util.List<Card> chance()` and `public static java.util.List<Card> communityChest()`, each returning the **16 standard cards** for that deck (Get-Out-of-Jail-Free appears in both).
- `data.BoardData` — optional internal helper for `StandardBoard`. Keep it inside `data`.

Use the canonical US board: GO, Mediterranean(60)/Baltic(60) BROWN, … Boardwalk(400) DARK_BLUE; railroads at 5/15/25/35 (price 200); utilities Electric(12)/Water(28) (price 150); Income Tax idx4 (200), Luxury Tax idx38 (100); Jail idx10, Go-To-Jail idx30, Free Parking idx20. Street rent tables and house costs per the standard deeds.

---

## `com.whim.monopoly.engine` — interfaces authored by Task 2, consumed by Task 3 **verbatim**

```java
public enum TurnPhase {
    AWAITING_ROLL,         // current player must roll (or act in jail)
    AWAITING_BUY,          // landed on unowned ownable: buy or decline→auction
    AUCTION,               // an auction is live
    AWAITING_END_TURN,     // landing resolved; may build/trade/mortgage then end turn
    GAME_OVER
}

// Read-only view of one ownable space's live ownership state.
public interface Holding {
    OwnableSpace getSpace();
    Player getOwner();         // null => bank-owned (unowned)
    boolean isMortgaged();
    int getHouseCount();       // 0..4 (hotel reported separately)
    boolean hasHotel();        // true == 5th building
}

public interface GameState {
    Board getBoard();
    java.util.List<Player> getPlayers();            // turn order; includes bankrupt (flagged)
    java.util.List<Player> getActivePlayers();      // not bankrupt
    Player getCurrentPlayer();
    Holding holdingAt(int spaceIndex);              // for any ownable index
    TurnPhase getPhase();
    boolean isGameOver();
    Player getWinner();                             // null until game over
    int[] getLastDice();                            // {d1,d2}; {0,0} before first roll
    // auction view (valid when phase == AUCTION)
    OwnableSpace getAuctionSpace();
    int getAuctionHighBid();
    Player getAuctionHighBidder();                  // null if no bid yet
}

// UI registers one listener; engine pushes all updates. All callbacks fire on the EDT-safe path
// the engine guarantees by invoking listeners synchronously from the calling (UI) thread.
public interface GameListener {
    void onLog(String message);          // append to the scrolling log
    void onStateChanged();               // re-render board + panels from GameState
    void onGameOver(Player winner);
}

// Optional trade payload (engine validates conservation + ownership).
public interface Trade {
    Player getProposer();
    Player getRecipient();
    int getProposerCash();               // cash proposer gives recipient
    int getRecipientCash();              // cash recipient gives proposer
    java.util.Set<Integer> getProposerDeeds();   // ownable indices proposer gives
    java.util.Set<Integer> getRecipientDeeds();  // ownable indices recipient gives
    int getProposerJailCards();
    int getRecipientJailCards();
}

public interface GameEngine {
    GameState getState();
    void addListener(GameListener l);

    // --- turn flow ---
    void rollDice();              // rolls, moves current player, resolves landing, emits logs,
                                  // sets phase to AWAITING_BUY / AWAITING_END_TURN / AWAITING_ROLL(doubles)
    void endTurn();              // advances to next active player; phase -> AWAITING_ROLL

    // --- buy / auction (phase AWAITING_BUY then AUCTION) ---
    void buyProperty();          // current player buys landed ownable at list price
    void declineProperty();      // start mandatory auction for the landed ownable
    void placeBid(Player p, int amount);   // amount must exceed current high bid
    void passAuction(Player p);            // p drops out; engine resolves when one bidder remains

    // --- building / mortgage (allowed in AWAITING_END_TURN for current player) ---
    boolean canBuildHouse(int spaceIndex);
    void buildHouse(int spaceIndex);
    void sellHouse(int spaceIndex);
    boolean canMortgage(int spaceIndex);
    void mortgage(int spaceIndex);
    void unmortgage(int spaceIndex);

    // --- jail (phase AWAITING_ROLL when current player isInJail) ---
    void payJailFine();          // pay $50, then must still rollDice() to move
    void useJailCard();          // spend a Get-Out-of-Jail-Free card, then rollDice()

    // --- trading ---
    boolean isTradeValid(Trade t);
    void executeTrade(Trade t);  // moves cash/deeds/jail cards; mortgaged deeds stay mortgaged

    // --- bankruptcy ---
    void declareBankruptcy();    // current player yields to current creditor (bank or player)
}
```

### Concrete engine classes Task 2 MUST provide

- `StandardGameEngine implements GameEngine` — `public StandardGameEngine(java.util.List<Player> players, Board board, java.util.List<Card> chance, java.util.List<Card> communityChest, java.util.Random rng)`. Owns the live `GameState` (ownership map, house/hotel counts, mortgage flags, decks with discard/reshuffle, building supply 32 houses/12 hotels), the turn state machine, rent math, auctions, mortgage math, even-build validation, and bankruptcy transfer. Drives everything through `GameListener` callbacks.
- `StandardTrade implements Trade` — public all-args constructor in field order above.
- Provide a small `engine.EngineSmokeTest` `main` that runs a seeded scripted game (fixed `Random` seed) exercising: buy, rent, pass-GO, jail in/out, auction, build with even-build rejection, mortgage/unmortgage, and a bankruptcy, printing a deterministic transcript. No assertions framework — plain `System.out` + `if` checks that print PASS/FAIL.

---

## `com.whim.monopoly.ui` — authored by Task 3

Build a Swing UI (`javax.swing`/`java.awt`/`java.awt.geom` only) consuming `domain` + `engine` interfaces verbatim. Provide:

- `ui.MonopolyFrame extends JFrame` — `public MonopolyFrame(GameEngine engine)`. Registers a `GameListener`, lays out the board canvas centrally with side/bottom panels.
- `ui.BoardPanel extends JPanel` — draws the classic square board with Java2D: 40 spaces around the border (11×11 grid feel via `GridBagLayout` or absolute geometry), color bands for street groups, space names/prices, house/hotel pips, and player tokens (filled circles in `Player.getToken()` color) on their current space; jailed tokens shown in the Jail corner.
- `ui.ControlPanel` — Roll button, Buy / Decline (→ auction) buttons, End Turn, jail actions (Pay $50 / Use Card), enabled/disabled per `GameState.getPhase()` and current player.
- `ui.PropertyPanel` — current player's holdings dashboard: build/sell house, mortgage/unmortgage buttons calling the engine; greys out illegal actions via `canBuildHouse`/`canMortgage`.
- `ui.AuctionDialog` (modal) — shown on `AUCTION` phase: current high bid/bidder, per-player Bid/Pass controls, calls `placeBid`/`passAuction`.
- `ui.TradeDialog` (modal) — pick recipient, cash both ways, deeds both ways, jail cards; builds a `StandardTrade`, validates via `isTradeValid`, calls `executeTrade`.
- A central scrolling `JTextArea` log appending every `onLog(...)` message (die rolls, moves, rent, purchases, auctions, transactions).
- A new-game setup (2–6 players, names) — keep it simple; the orchestrator's `Main` may instead construct a default 4-player game and pass the engine in. Support BOTH: `MonopolyFrame(GameEngine)` is the required entry; an optional setup dialog is a bonus.

UI must **never** mutate domain state directly except through `GameEngine` methods; it reads via `GameState`/`Holding` and re-renders on `onStateChanged()`.

---

## Integration notes (orchestrator consolidation)

- Orchestrator writes `app/Main.java`: builds `StandardBoard`, `DefaultPlayer` list (4 players, distinct token colors), `Cards.chance()/communityChest()`, a `new Random()`, constructs `StandardGameEngine`, then `SwingUtilities.invokeLater(() -> new MonopolyFrame(engine).setVisible(true))`.
- The orchestrator compiles the full tree and runs `EngineSmokeTest` headlessly to verify the rules engine before finishing.
- Each task pushes its branch and opens a PR back to the orchestrator branch `whim-wd-291`, then reports completion/blockers to the orchestrator via `send_prompt`.
