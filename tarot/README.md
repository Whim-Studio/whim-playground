# Tarot Reader (Java 8 Swing)

A standalone, dependency-free digital Tarot reading application. Full 78-card
Rider-Waite-Smith deck, authentic upright/reversed meanings, three classic
spreads, and a synthesized plain-English interpretation engine.

## Features

- **Full 78-card deck** — 22 Major Arcana + 56 Minor Arcana (Wands, Cups,
  Swords, Pentacles), each with authentic upright meaning, reversed meaning, and
  symbolic description.
- **Upright / Reversed** — every dealt card is independently 50% upright / 50%
  reversed, which flips its meaning.
- **Three spreads** — Single Card (Daily Focus), 3-Card (Past / Present /
  Future), and the 10-card Celtic Cross.
- **Interpretation engine** — stitches each card's active meaning to its
  position into a cohesive, multi-paragraph reading.
- **RWS artwork** — public-domain 1909 Rider-Waite-Smith images fetched and
  cached from Wikimedia Commons at runtime (text placeholders shown offline).

## Run

Requires a JDK (8+). From the repo root:

```sh
javac -d out $(find tarot/src -name '*.java')
java -cp out com.whim.tarot.app.Main
```

## Architecture

| Package | Responsibility |
|---------|----------------|
| `com.whim.tarot.domain` | `Card`, `DrawnCard`, `Suit`, `Orientation`, `SpreadType`/`SpreadPosition` model |
| `com.whim.tarot.data`   | `TarotDeckData` — all 78 hardcoded cards + Commons image URLs |
| `com.whim.tarot.image`  | `ImageLoader` — thread-safe memory + disk image cache, off-EDT fetch |
| `com.whim.tarot.engine` | `TarotEngine` (shuffle/deal), `Reading`, `ReadingInterpreter` (synthesis) |
| `com.whim.tarot.ui`     | `MainWindow`, `SpreadPanel`, `CardView` — Swing presentation |
| `com.whim.tarot.app`    | `Main` — entry point |

All network I/O and deal/interpret work runs off the Event Dispatch Thread.

## Constraints

Java 8 only. No external libraries — `javax.swing`, `java.awt`, `java.util`,
`java.io`, `java.net`, `java.nio` exclusively. No Maven/Gradle/JSON libs.
