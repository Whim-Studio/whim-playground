# 大怪路子 (Da Guai Lu Zi) — Shared Interface Contract (binding for Tasks 1, 2, 3)

A standalone **Java 8 Swing** desktop app implementing Da Guai Lu Zi: a 6-player
shedding card game, 1 Human vs 5 AI, two alternating teams of 3, with a rule-enforced
engine and an interactive **Coach Mode**.

**Hard constraints (all tasks):**
- Java 8 only. **No `var`, no switch expressions, no text blocks**, no `java.time`-only
  APIs, no records. Streams/lambdas exist in Java 8 and are allowed but keep it simple.
- **Zero external libraries.** Only `javax.swing`, `java.awt`, `java.util`, `java.lang`.
  No Maven/Gradle dependency resolution. Plain `javac`.
- This document is the **single source of truth**. Every package, class name, enum
  constant, method signature, and field below is a hard contract. **Do not rename or
  restructure shared types.** If something is underspecified, pick the simplest Java-8
  implementation and keep signatures exactly as written.

## Directory & build

```
daguailuzi/
  README.md
  src/com/dglz/
    domain/    # Task 1 — data models + shared interfaces (no engine algorithms, no UI)
    engine/    # Task 1 — combination validation + game flow (imports domain)
    ai/        # Task 2 — AI strategy + Coach (imports domain + engine)
    ui/        # Task 3 — Swing views (imports domain + engine + domain interfaces)
    app/       # Task 3 — Main entry point (wires concrete ai impls into ui)
```

Compile & run (no Maven; plain javac):
```
javac -d daguailuzi/out $(find daguailuzi/src -name '*.java')
java -cp daguailuzi/out com.dglz.app.Main
```

Package root: `com.dglz`.

---

## Task 1 — Domain & Engine (`com.dglz.domain`, `com.dglz.engine`)

### `domain.Rank` enum — ascending strength
Constants IN THIS ORDER, each with `int order` (3=lowest .. 16=highest) and a `String label`:
```
THREE(3,"3") FOUR(4,"4") FIVE(5,"5") SIX(6,"6") SEVEN(7,"7") EIGHT(8,"8")
NINE(9,"9") TEN(10,"10") JACK(11,"J") QUEEN(12,"Q") KING(13,"K") ACE(14,"A")
TWO(15,"2") SMALL_JOKER(16,"小怪") BIG_JOKER(17,"大怪")
```
(Note: `2` is high. Joker orders 16/17 are above all natural ranks.)
Methods: `int order()`, `String label()`, `boolean isJoker()` (SMALL_JOKER or BIG_JOKER).
Static `Rank[] naturalAscending()` returns THREE..TWO (no jokers) for straight building.

### `domain.Suit` enum
```
CLUBS("♣") DIAMONDS("♦") HEARTS("♥") SPADES("♠") JOKER("★")
```
`String symbol()`. Jokers use `Suit.JOKER`.

### `domain.Card` (immutable)
Fields: `final Rank rank; final Suit suit; final int deckId;` (deckId 0..2, which of the
3 decks). Constructor `Card(Rank rank, Suit suit, int deckId)`.
Methods: `Rank rank()`, `Suit suit()`, `int deckId()`,
`boolean isWildcard()` (true iff `rank.isJoker()`),
`boolean isBigJoker()`, `boolean isSmallJoker()`,
`String shortName()` (e.g. `"♠K"`, `"小怪"`).
Implement `equals`/`hashCode` over (rank, suit, deckId) and a readable `toString()`.

### `domain.Deck`
- `Deck()` builds the full 162-card list: 3 decks × (52 ranked + 2 jokers).
- `List<Card> cards()` — the 162 cards.
- `void shuffle(Random rng)`.
- `List<List<Card>> deal(int players, int perPlayer)` — deals `perPlayer` to each;
  called with (6, 27). Returns 6 hands of 27.

### `domain.Team` enum
```
TEAM_A("Team A") TEAM_B("Team B")
```
`String label()`. Static `Team forSeat(int seat)` → seat even (0,2,4)=TEAM_A,
seat odd (1,3,5)=TEAM_B. (Seats 0..5 around the table; human is seat 0 / TEAM_A.)

### `domain.Player`
Fields: `final int seat; final String name; final boolean human; final Team team;
final List<Card> hand;` (mutable hand list).
Methods: `int seat()`, `String name()`, `boolean isHuman()`, `Team team()`,
`List<Card> hand()`, `int cardCount()`, `boolean isOut()` (hand empty).

### `domain.Road` enum — the 4 playable paths
```
SINGLE(1) PAIR(2) TRIPLE(3) FIVE(5)
```
`int size()`. Static `Road forSize(int n)` → null if not 1/2/3/5.

### `domain.ComboType` enum — ascending strength WITHIN a road
Single-card roads have one type each; the FIVE road is ranked low→high exactly:
```
// 1/2/3 road:
SINGLE(Road.SINGLE) PAIR(Road.PAIR) TRIPLE(Road.TRIPLE)
// 5 road, ascending:
STRAIGHT(Road.FIVE)        // 顺子   (5 consecutive ranks, mixed suits)
FLUSH(Road.FIVE)           // 同花   (5 same suit, not a straight)
FULL_HOUSE(Road.FIVE)      // 葫芦   (3+2)
FOUR_PLUS_ONE(Road.FIVE)   // 炸弹   (4 same rank + any 1)
STRAIGHT_FLUSH(Road.FIVE)  // 同花顺 (straight, all same suit)
FIVE_OF_A_KIND(Road.FIVE)  // 5根    (5 same rank)
```
`Road road()`. The enum's natural `ordinal()` gives intra-road tier ordering
(use a `tier()` that returns ordinal among the FIVE-road group for comparison).

### `domain.Combination`
Represents a validated play. Fields:
`final Road road; final ComboType type; final List<Card> cards;
final Rank primaryRank; final int wildcardsUsed;`
- `primaryRank`: the rank used to compare two combos of the SAME type (for STRAIGHT /
  STRAIGHT_FLUSH = highest rank of the run; FLUSH = highest card; FULL_HOUSE / TRIPLE /
  FOUR_PLUS_ONE / FIVE_OF_A_KIND = the triple/quad/quint rank; PAIR/SINGLE = the rank).
Methods: getters, `int size()`.
`boolean beats(Combination other)` — same `road` required; a higher `type.tier()` wins;
if same type, higher `primaryRank.order()` wins. Different road → throws
IllegalArgumentException (callers must pre-check road equality).
Provide static factory helpers only as needed; construction is done by `ComboValidator`.

### `domain.MoveSuggestion` (shared value object — produced by Task 2, consumed by Task 3)
Fields: `final Combination play;`  (null == recommend PASS)
`final List<Card> highlightCards;` (cards in the human hand to highlight; empty if pass)
`final String explanation;` (plain-English strategic reasoning for Coach Mode).
Getters: `Combination play()`, `List<Card> highlightCards()`, `String explanation()`.

### `domain.PlayerStrategy` interface (implemented by Task 2)
```
Combination decideMove(GameState state, int seat);
```
Return the combination this seat plays, or `null` to PASS. Must be legal for the
current trick (engine re-validates).

### `domain.MoveAdvisor` interface (implemented by Task 2, used by Task 3 UI)
```
MoveSuggestion advise(GameState state, int humanSeat);
```

### `engine.ComboValidator` (Task 1 — the rules core)
Pure functions; no state.
- `Combination identify(List<Card> cards)` — returns the single best legal Combination
  these cards form, applying wildcards (jokers) to complete 5-card hands, or `null` if
  the cards form no legal combination. Wildcard rules:
  - A joker may substitute for a needed natural card to complete STRAIGHT, FLUSH,
    FULL_HOUSE, FOUR_PLUS_ONE, STRAIGHT_FLUSH, FIVE_OF_A_KIND, PAIR, or TRIPLE.
  - **A Small Joker may NOT represent a Big Joker** (i.e. when filling, a small-joker
    wildcard may stand in for any natural rank but never for BIG_JOKER; a big joker may
    stand in for anything). Prefer the interpretation yielding the **highest** ComboType.
  - For SINGLE: any one card (a bare joker is its own rank).
- `boolean sameRoad(Combination a, Combination b)`.
- `boolean isLegalFollow(Combination lead, Combination candidate)` — candidate must be
  the same Road as lead and `candidate.beats(lead)`.
- `List<Combination> enumerate(List<Card> hand, Road leadRoad, Combination toBeat)` —
  all legal combinations from `hand` for the given `leadRoad` that beat `toBeat`
  (`toBeat` null when leading). Used by Task 2. Keep it correct but may bound 5-road
  enumeration pragmatically; document any bound in the README.

### `engine.GameState` (Task 1)
The full mutable game snapshot. Fields/accessors:
- `List<Player> players()` (size 6, index == seat).
- `int leaderSeat()` — seat that led the current trick.
- `int currentSeat()` — whose turn it is.
- `Road currentRoad()` — road of the current trick (null before first lead).
- `Combination currentBest()` — best combination played so far in the trick (null if none).
- `int currentBestSeat()` — seat that played `currentBest` (the seat currently winning
  the trick), or -1.
- `List<Play> trickPlays()` — plays this trick in order.
- `boolean gameOver()` and `Team winningTeam()` (null until a team has all 3 out).
- `List<String> log()` — human-readable event log lines.

### `domain.Play`
Fields: `final int seat; final Combination combo;` (`combo == null` ⇒ PASS).
Getters; `boolean isPass()`.

### `engine.GameEngine` (Task 1 — drives the game; UI & AI call into it)
Constructor: `GameEngine(long seed, String[] playerNames)` (names size 6; seat 0 human).
- `void start()` — build deck, shuffle with seed, deal 27 each, set first leader (seat 0).
- `GameState state()`.
- `List<Card> humanHand()` — convenience for seat 0.
- `Combination validateSelection(List<Card> selected)` — identify + legality vs current
  trick; returns the Combination if the human's selection is a legal play right now, else
  null.
- `boolean playHuman(List<Card> selected)` — if legal, applies the play and advances.
- `boolean passHuman()` — pass if legal (cannot pass when leading).
- `void setStrategy(PlayerStrategy strategy)` — inject Task 2's AI.
- `boolean stepAI()` — if `currentSeat` is an AI seat, query the strategy, apply its
  move (validate; PASS if it returns null/illegal), advance, and return true. Returns
  false if it's the human's turn or game over.
- Trick resolution: when all other active players have passed on the current best, the
  winner of the trick leads the next trick; a player who is out is skipped. First team
  with all 3 members out wins.

---

## Task 2 — AI Opponents & Coach (`com.dglz.ai`, imports `domain` + `engine`)

Implements `domain.PlayerStrategy` and `domain.MoveAdvisor`.

### `ai.HandEvaluator`
Static evaluation helpers used by both AI and Coach. At minimum:
- `int handStrength(List<Card> hand)` — heuristic score (more high cards / bombs / jokers
  = stronger; fewer scattered singles = better).
- `boolean teammateWinning(GameState state, int seat)` — true if the seat that played
  `currentBest` is on the same team as `seat` and is not `seat` itself.

### `ai.AiStrategy implements domain.PlayerStrategy`
`Combination decideMove(GameState state, int seat)`:
- Enumerate legal moves via `ComboValidator.enumerate`.
- **Team synergy rule (required):** if `teammateWinning` is true and the seat is not
  forced to lead, PASS (do not outbid your own teammate).
- Otherwise pick using card-counting + trick-control heuristics: when leading, prefer
  shedding weak/awkward cards and keeping bombs/jokers; when following, win as cheaply as
  possible (smallest combo that beats `currentBest`); hold strong bombs for contested
  tricks. Avoid breaking pairs/triples/bombs to win a trivial single.

### `ai.CoachTranslator implements domain.MoveAdvisor`
`MoveSuggestion advise(GameState state, int humanSeat)`:
- Compute the objectively best legal move for the human (reuse `AiStrategy` logic or a
  shared `chooseBest` method), then build a `MoveSuggestion` whose `explanation` is a
  plain-English paragraph covering WHAT to play, WHY now, and the TEAM/board effect —
  e.g. "Play your three Kings. This forces out the opponents' Aces and lets your
  teammate (seat 2) take control of the next trick." If passing is best, explain why
  holding is correct. `highlightCards` lists exactly the cards of `play` (empty on pass).

Constructor note: both classes are no-arg constructible so `app.Main` can `new` them.

---

## Task 3 — Swing UI & App (`com.dglz.ui`, `com.dglz.app`)

Imports `domain`, `engine`, and the `domain.MoveAdvisor` interface. **Must not import
`com.dglz.ai` concretely except in `app.Main`** (Main wires the impls).

### `ui.GameWindow extends JFrame`
- 6-seat round table layout: human hand at the BOTTOM, 5 AI seats arranged around (use a
  layout that reads clearly — e.g. BorderLayout regions + a custom center `JPanel`).
- Human hand: cards rendered as clickable components; clicking toggles select/deselect.
  Selected cards visibly lift/highlight.
- Each AI seat shows: name, team color, remaining card count, and a marker when that seat
  is currently winning the trick.
- Center overlay panel shows the current trick: the active Road (1/2/3/5) and each seat's
  latest play (or PASS).
- Buttons: **Play** (validate selection via `engine.validateSelection`; on null show a
  brief inline message; on success `engine.playHuman`), **Pass**, **New Game**.
- An AI turn driver: after the human acts, advance AI seats (e.g. a `javax.swing.Timer`
  calling `engine.stepAI()` until it's the human's turn or game over) so the UI animates
  turn-by-turn. Keep all engine interaction on the EDT or marshal back with
  `SwingUtilities.invokeLater`.

### Coach Mode (Task 3 renders, Task 2 generates)
- A **Coach toggle** (JToggleButton/JCheckBox). The UI holds a `domain.MoveAdvisor`
  (injected by Main).
- When ON and it's the human's turn: call `advisor.advise(state, 0)`, then:
  - Highlight `suggestion.highlightCards()` in the hand (distinct from manual selection).
  - Show `suggestion.explanation()` in a persistent side panel (or prominent area).
- When OFF, hide the coach panel. Re-query whenever the board state changes.

### `app.Main`
`public static void main(String[] args)`:
```
GameEngine engine = new GameEngine(seed, names);
engine.setStrategy(new com.dglz.ai.AiStrategy());
engine.start();
MoveAdvisor advisor = new com.dglz.ai.CoachTranslator();
SwingUtilities.invokeLater(() -> new GameWindow(engine, advisor).setVisible(true));
```
(This is the ONLY place `com.dglz.ai` is referenced by name in Task 3's tree.)

---

## Reporting
Each child task: implement only its packages, push its branch, open a PR, and
`send_prompt` a short report (what shipped + any contract gaps found) back to the
orchestrator. Do **not** edit another task's packages.
