# Babylon 5 CCG — Shared Interface Contract (Java 8)

Standalone single-player desktop adaptation of the **Babylon 5 Collectible Card Game**
(Premiere / Base Set + Deluxe Edition only). One human player vs **exactly three AI
opponents**. Pure Java 8 + Swing, **zero external libraries**.

- App dir: `babylon5/`
- Base package: `com.whim.babylon5`
- **Java 8 ONLY.** No `var`, no switch expressions, no text blocks, no records, no
  post-Java-8 APIs. Lambdas + standard functional interfaces are fine. **No external
  libraries** — only `javax.swing`, `java.awt`, `java.util`, `java.io`, `java.nio`,
  `java.net`, `java.util.concurrent`. No Maven/Gradle deps for core logic.
- **No model leaks:** Tasks 2 and 3 **import and operate on** the `domain` / `data`
  types authored by Task 1. They MUST NOT redeclare them.
- **Rule authority:** `babylon5/docs/rulebook-source.txt` (machine-extracted from the
  user's uploaded *Babylon 5 CCG — Psi Corps v1.3b* rulebook) is the absolute authority.
  Cite section/topic when implementing a rule. If the rulebook is silent or ambiguous,
  state the ambiguity, cite a community source (e.g. BoardGameGeek), and propose a
  deterministic ruling.

## File ownership (NO overlap between tasks)

- **Task 1 (domain/state/data/research)** owns:
  - `babylon5/src/main/java/com/whim/babylon5/domain/**`
  - `babylon5/src/main/java/com/whim/babylon5/data/**`
  - `babylon5/src/main/resources/cards/**` (JSON card data)
  - `babylon5/docs/research-dossier.md`
- **Task 2 (engine/AI)** owns:
  - `babylon5/src/main/java/com/whim/babylon5/engine/**`
  - `babylon5/src/test/java/com/whim/babylon5/engine/**`
  - `babylon5/docs/design-and-ai.md`
- **Task 3 (ui/testing-plan)** owns:
  - `babylon5/src/main/java/com/whim/babylon5/ui/**`
  - `babylon5/docs/ui-and-testing.md`
- **`com/whim/babylon5/Main.java`** is written by the **orchestrator** at consolidation.
  Do NOT create it.

Every public type below is part of the contract. Match the package, name, and signatures
**verbatim**. You may add fields/helpers freely, but do not change or remove anything
specified here. Where a body is shown, treat it as required behavior.

---

## DOMAIN (Task 1 — package `com.whim.babylon5.domain`)

### Enums

```java
public enum ConflictType { DIPLOMACY, INTRIGUE, PSI, MILITARY }

// Strict turn sequence from the rulebook: Ready -> Conflict -> Action -> Resolution
// (Aftermath card play happens in RESOLUTION) -> Draw, then play passes to next player.
public enum Phase { READY, CONFLICT, ACTION, RESOLUTION, DRAW }

public enum CardType { AMBASSADOR, CHARACTER, CONFLICT, AFTERMATH, AGENDA, LOCATION, SUPPORT }

public enum FactionId { HUMAN, MINBARI, NARN, CENTAURI, VORLON, SHADOW, PSI_CORPS, NONALIGNED }

public enum ZoneType { DRAW_DECK, HAND, INNER_CIRCLE, SUPPORTING, DISCARD, REMOVED }
```

### Card — immutable printed definition + lightweight in-play flags

```java
public final class Card {
    public Card(String id, String name, CardType type, FactionId faction, int cost,
                int influence, int diplomacy, int intrigue, int psi, int military,
                String text, String imageUrl) { ... }

    public String   getId();
    public String   getName();
    public CardType getType();
    public FactionId getFaction();
    public int      getCost();
    public int      getInfluence();
    public int      getDiplomacy();
    public int      getIntrigue();
    public int      getPsi();
    public int      getMilitary();
    public String   getText();
    public String   getImageUrl();

    /** Attribute used as support/opposition for the given conflict type. */
    public int support(ConflictType t);   // DIPLOMACY->diplomacy, INTRIGUE->intrigue, PSI->psi, MILITARY->military

    // In-play physical state (single-player engine treats each Card as one physical card).
    public boolean isReady();
    public void    setReady(boolean ready);   // false == marked/exhausted/tapped
    public int     getDamage();
    public void    addDamage(int d);
    public void    clearDamage();
}
```

### Zone

```java
public final class Zone {
    public Zone(ZoneType type);
    public ZoneType getType();
    public java.util.List<Card> getCards();   // live, ordered; index 0 == top for DRAW_DECK
    public void    add(Card c);
    public boolean remove(Card c);
    public int     size();
    public boolean isEmpty();
    public void    shuffle(java.util.Random rng);
    public Card    draw();                     // remove & return top (index 0), or null if empty
}
```

### Deck — a named card list used to seed a player

```java
public final class Deck {
    public Deck(String name, FactionId faction, java.util.List<Card> cards);
    public String getName();
    public FactionId getFaction();
    public java.util.List<Card> getCards();
}
```

### PlayerState — a faction in play (the user's "Faction")

```java
public final class PlayerState {
    public PlayerState(String name, FactionId faction, boolean human);
    public String   getName();
    public FactionId getFaction();
    public boolean  isHuman();

    public int  getInfluenceRating();          // starts at 4; base Power == this
    public void setInfluenceRating(int r);
    public int  getInfluencePool();            // spendable influence; starts at 4
    public void setInfluencePool(int p);
    public void adjustInfluencePool(int delta);

    public int  getPower();                    // total Power; engine computes & sets
    public void setPower(int power);

    public Zone zone(ZoneType t);              // accessor for this player's zone
    public Card getAmbassador();               // the Ambassador in INNER_CIRCLE (or null)
}
```

### GameState

```java
public final class GameState {
    public GameState(java.util.List<PlayerState> players, long seed);
    public java.util.List<PlayerState> getPlayers();
    public PlayerState getActivePlayer();
    public int  getActiveIndex();
    public void setActiveIndex(int i);
    public Phase getPhase();
    public void  setPhase(Phase p);
    public int  getTurn();
    public void incrementTurn();
    public java.util.Random getRng();          // deterministic, seeded
}
```

### GameFactory — builds the standard 1-human + 3-AI game

```java
public final class GameFactory {
    /** 4 players (index 0 human, 1..3 AI). Each: influence rating & pool = 4,
     *  Ambassador placed in INNER_CIRCLE, deck built from CardDatabase, shuffled,
     *  opening hand drawn. Deterministic for a given seed. */
    public static GameState newStandardGame(long seed);
    public static final int OPENING_HAND_SIZE = 6;
}
```

### GameListener — engine -> UI events (UI implements this)

```java
public interface GameListener {
    void onPhaseChanged(Phase phase, int activeIndex);
    void onConflictResolved(com.whim.babylon5.engine.ConflictResult result);
    void onStateChanged();
    void onLog(String message);
}
```
Also provide `public final class NoOpGameListener implements GameListener` with empty methods.

> Note: `GameListener` references `engine.ConflictResult`. This is the single permitted
> domain→engine reference and exists only so the UI can receive results. Keep it.

---

## DATA (Task 1 — package `com.whim.babylon5.data`)

### CardDatabase — loads JSON card data, embedded fallback

```java
public final class CardDatabase {
    public static java.util.List<Card> all();                  // all Premiere+Deluxe cards
    public static java.util.List<Card> forFaction(FactionId f);
    public static Card byId(String id);                        // null if absent
    public static Card copyOf(Card c);                         // fresh in-play instance (reset ready/damage)
}
```
Loads from `src/main/resources/cards/*.json` on the classpath. If resources are missing,
fall back to a small embedded set so the prototype always runs. **JSON schema** is defined
in the research dossier; provide at least one complete example card.

### ImageLoader — legal network fetch + cache (NEVER embeds copyrighted art)

```java
public final class ImageLoader {
    /** Returns a cached image immediately if present, else a placeholder, and loads
     *  asynchronously off the EDT. Disk cache under ~/.b5ccg/cache. */
    public static java.awt.Image get(String url);
    public static void preload(java.util.Collection<String> urls);
    public static java.awt.Image placeholder();
}
```

---

## ENGINE (Task 2 — package `com.whim.babylon5.engine`)

### AiDifficulty

```java
public enum AiDifficulty { EASY, MEDIUM, HARD }
```

### Conflict — a declared conflict + committed cards

```java
public final class Conflict {
    public Conflict(int initiatorIndex, ConflictType type, int targetIndex);
    public int getInitiator();
    public ConflictType getType();
    public int getTarget();
    public java.util.List<Card> getSupport();      // initiator's committed supporters (mutable)
    public java.util.List<Card> getOpposition();   // target's committed opposers (mutable)
}
```

### ConflictResult — immutable outcome

```java
public final class ConflictResult {
    public boolean initiatorWon();        // true iff modified support STRICTLY exceeds modified opposition
    public int     supportTotal();
    public int     oppositionTotal();
    public ConflictType type();
    public java.util.List<Card> neutralized();   // cards neutralized/damaged out
    public String  summary();             // human-readable one-liner for the log
}
```

### GameEngine — turn loop + rule validation

```java
public final class GameEngine {
    public GameEngine(GameState state);
    public GameState getState();
    public void addListener(GameListener l);

    /** Advance one phase along READY->CONFLICT->ACTION->RESOLUTION->DRAW; after DRAW,
     *  pass play to the next player and begin their READY. Fires GameListener events. */
    public void advancePhase();

    /** Pay influence from the player's pool and move a CHARACTER/AMBASSADOR from HAND
     *  into SUPPORTING (or INNER_CIRCLE). Returns false if illegal (cost, phase, ...). */
    public boolean sponsorCharacter(int playerIndex, Card character);

    /** Core conflict math: modified support vs modified opposition; initiator succeeds
     *  iff support strictly exceeds opposition. Applies damage/neutralization. */
    public ConflictResult resolveConflict(Conflict c);

    public int computePower(PlayerState p);   // base = influence rating, + bonuses

    /** Standard Victory: returns the player with >= 20 Power AND strictly more Power
     *  than every other player; null otherwise. */
    public PlayerState checkVictory();

    /** Run an AI player's full turn using pure logic only — SAFE to call off the EDT. */
    public void runAiTurn(int playerIndex);

    public static final int VICTORY_POWER = 20;
}
```

### AIPlayer — three difficulty tiers

```java
public final class AIPlayer {
    public AIPlayer(AiDifficulty difficulty);
    public AiDifficulty getDifficulty();

    /** REQUIRED fully-implemented demonstrated decision. */
    public Card chooseCharacterToSponsor(GameState state, int playerIndex);

    public Conflict chooseConflict(GameState state, int playerIndex);   // null == decline
    public boolean  willCommit(Card c, Conflict pending, boolean asSupport);
}
```

---

## UI (Task 3 — package `com.whim.babylon5.ui`)

### MainWindow

```java
public final class MainWindow extends javax.swing.JFrame
        implements com.whim.babylon5.domain.GameListener {
    public MainWindow(GameEngine engine);
    /** Build & show the window, then start the game loop on a BACKGROUND thread.
     *  ALL GameEngine / AIPlayer computation runs OFF the EDT; UI mutations are
     *  marshalled back via SwingUtilities.invokeLater. */
    public void start();
}
```
All other UI classes/panels live under `com.whim.babylon5.ui` and are owned by Task 3.

---

## Orchestrator entry point (do NOT write — for reference)

```java
// com/whim/babylon5/Main.java
GameState state = GameFactory.newStandardGame(System.currentTimeMillis());
GameEngine engine = new GameEngine(state);
javax.swing.SwingUtilities.invokeLater(() -> new com.whim.babylon5.ui.MainWindow(engine).start());
```

## Reporting

When done, **push your branch, open a PR into `whim-wd-215`**, and report back to the
orchestrator task via `send_prompt` with: branch name, PR link, what you built, and any
contract deviations. Do not report via task comments.
