# Tarot Reader — Shared Interface Contract (Java 8)

Standalone Swing app: an authentic 78-card Rider-Waite-Smith Tarot reading engine.

- App dir: `tarot/`
- Source root: `tarot/src/` (plain layout, package dirs under it — e.g. `tarot/src/com/whim/tarot/domain/Card.java`)
- Base package: `com.whim.tarot`
- **Java 8 ONLY.** No `var`, no text blocks, no switch expressions, no records, no `Stream` collectors that postdate Java 8. Stick to plain Java 8.
- **No external libraries.** Only `javax.swing`, `java.awt`, `java.util`, `java.io`, `java.net`, `java.nio`. No Maven/Gradle/Gson/Jackson at runtime.
- Compile target: `javac -d out $(find tarot/src -name '*.java')`. Run: `java -cp out com.whim.tarot.app.Main`.

## File ownership (NO overlap between tasks)

- **Task 1 (domain + data + images)** owns `tarot/src/com/whim/tarot/domain/**` and `tarot/src/com/whim/tarot/data/**` and `tarot/src/com/whim/tarot/image/**`.
- **Task 2 (engine + interpreter)** owns `tarot/src/com/whim/tarot/engine/**`.
- **Task 3 (Swing UI)** owns `tarot/src/com/whim/tarot/ui/**`.
- **Main class** (`tarot/src/com/whim/tarot/app/Main.java`) is written by the orchestrator during consolidation. Do NOT create it.

Tasks 2 and 3 MUST code against the interfaces below **verbatim** and MUST NOT create their own copies of `domain` or shared `engine` interfaces. The orchestrator compiles the whole project during consolidation.

---

## Domain interfaces — authored by Task 1, package `com.whim.tarot.domain`

```java
public enum Orientation { UPRIGHT, REVERSED }

// Suit also identifies the Major Arcana group.
public enum Suit {
    MAJOR("Major Arcana"), WANDS("Wands"), CUPS("Cups"),
    SWORDS("Swords"), PENTACLES("Pentacles");
    private final String label;
    Suit(String label) { this.label = label; }
    public String getLabel() { return label; }
    public boolean isMajor() { return this == MAJOR; }
}

public interface Card {
    int getId();                  // 0..77, unique, stable
    String getName();             // e.g. "The Fool", "Three of Cups"
    Suit getSuit();
    int getNumber();              // Major: 0..21 (Roman numeral order). Minor: 1..14 (Ace=1..King=14)
    String getUprightMeaning();   // 1-3 sentences, keyword-rich
    String getReversedMeaning();  // 1-3 sentences
    String getDescription();      // high-level imagery / symbolism, 1-3 sentences
    String getImageUrl();         // public-domain RWS image URL (Wikimedia Commons direct file URL)
    default boolean isMajor() { return getSuit().isMajor(); }
}

// A card paired with the orientation it was drawn in.
public interface DrawnCard {
    Card getCard();
    Orientation getOrientation();
    boolean isReversed();
    String getActiveMeaning();    // upright or reversed meaning per orientation
}

// Concrete provided by Task 1:
//   DefaultCard implements Card (public constructor taking all fields)
//   DefaultDrawnCard implements DrawnCard (public ctor: DefaultDrawnCard(Card, Orientation))
```

### Card repository — Task 1, package `com.whim.tarot.data`

```java
public interface CardRepository {
    java.util.List<Card> getAllCards();   // EXACTLY 78, ordered by id 0..77
    Card getById(int id);
    int size();                            // 78
}

// Concrete provided by Task 1: TarotDeckData implements CardRepository,
// public no-arg constructor, all 78 cards hardcoded with authentic RWS meanings + Commons URLs.
// Card id ordering: 0..21 Major Arcana (Fool..World), then Wands 22..35, Cups 36..49,
// Swords 50..63, Pentacles 64..77 (Ace..King within each suit).
```

### Image loader — Task 1, package `com.whim.tarot.image`

```java
public final class ImageLoader {
    // Singleton or static accessor.
    public static ImageLoader getInstance();
    // Returns a cached image (memory cache + optional on-disk cache under java.io.tmpdir),
    // fetching over HTTP(S) on first use. Returns null on failure (caller shows placeholder).
    // MUST be safe to call from a background thread. Never call from the EDT for network I/O.
    public java.awt.image.BufferedImage load(String imageUrl) throws java.io.IOException;
    // Non-throwing convenience: returns null instead of throwing.
    public java.awt.image.BufferedImage loadQuietly(String imageUrl);
}
```

---

## Spread / position model — authored by Task 1, package `com.whim.tarot.domain`

```java
public enum SpreadType {
    SINGLE("Daily Focus", 1),
    THREE_CARD("Past, Present, Future", 3),
    CELTIC_CROSS("Celtic Cross", 10);
    private final String label; private final int size;
    SpreadType(String label, int size) { this.label = label; this.size = size; }
    public String getLabel() { return label; }
    public int getCardCount() { return size; }
    // Ordered positions for this spread; list size == getCardCount().
    public java.util.List<SpreadPosition> getPositions() { /* Task 1 fills authentic position names */ }
}

public interface SpreadPosition {
    int getIndex();        // 0-based, matches deal order
    String getName();      // e.g. "The Present", "Hopes & Fears"
    String getMeaning();   // what this slot represents in a reading
}
```

Authentic positions Task 1 must encode:
- SINGLE: [0] "Daily Focus".
- THREE_CARD: [0] "The Past", [1] "The Present", [2] "The Future".
- CELTIC_CROSS: [0] "The Present / Significator", [1] "The Challenge (Crossing)", [2] "The Foundation / Past", [3] "The Recent Past", [4] "The Crown / Possible Outcome", [5] "The Near Future", [6] "Self / Your Attitude", [7] "External Influences / Environment", [8] "Hopes & Fears", [9] "The Final Outcome".

---

## Engine interfaces — authored by Task 2, package `com.whim.tarot.engine`

Task 2 owns these. Task 3 codes against them verbatim.

```java
public interface PositionedCard {
    SpreadPosition getPosition();
    DrawnCard getDrawnCard();
}

public interface Reading {
    SpreadType getSpreadType();
    java.util.List<PositionedCard> getPositionedCards();   // size == spread card count, ordered by position index
    String getSynthesis();   // full plain-English synthesized interpretation (from ReadingInterpreter)
}

public final class TarotEngine {
    public TarotEngine();                       // builds deck from a TarotDeckData()
    public TarotEngine(CardRepository repo);    // injectable for testing
    public void shuffle();                      // Fisher-Yates over the 78-card deck
    public void shuffle(long seed);             // deterministic shuffle for tests
    // Deals SpreadType.getCardCount() distinct cards, each 50% upright/reversed,
    // maps them to that spread's positions in order, runs ReadingInterpreter, returns Reading.
    public Reading deal(SpreadType type);
}

public final class ReadingInterpreter {
    public ReadingInterpreter();
    // Produces a cohesive, multi-paragraph plain-English reading that references each
    // card by its position name, its orientation, and stitches a synthesized conclusion.
    public String interpret(Reading reading);
}
```

Concrete impls Task 2 provides: `DefaultPositionedCard`, `DefaultReading`. Reading returned by `deal` already has `getSynthesis()` populated.

---

## UI — authored by Task 3, package `com.whim.tarot.ui`

```java
public final class MainWindow extends javax.swing.JFrame {
    public MainWindow();              // builds full UI; constructs its own TarotEngine
    public void showApp();            // pack + setVisible(true) on the EDT
}
```

Requirements:
- Spread selector (SINGLE / THREE_CARD / CELTIC_CROSS), "Shuffle Deck", "Draw Cards" buttons.
- Visually arrange `Reading.getPositionedCards()` to match the spread shape; Celtic Cross must overlap card 0 and card 1 (the crossing) at center, with the staff column of cards 6-9 to the right. Use absolute/`GridBagLayout`/custom painting as needed.
- Clicking a card shows that card's name, orientation, position name+meaning, and active meaning in a detail panel; a separate reading panel shows `Reading.getSynthesis()`.
- Card images via `ImageLoader` fetched on a background thread (`SwingWorker` or a worker pool), never on the EDT; show a text placeholder until the image arrives, then repaint. Interpretation/deal also off the EDT.
- Reversed cards rendered rotated 180° (or clearly badged "Reversed").

## Integration notes
- Everyone uses `com.whim.tarot.*` packages exactly as above.
- Do NOT write `Main.java`. The orchestrator wires `MainWindow` in `com.whim.tarot.app.Main` during consolidation.
- Report results or blockers back to the orchestrator via `send_prompt`, push your branch, and open a PR when done.
