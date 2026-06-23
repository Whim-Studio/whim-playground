# Tiwa's Mah Jong — Demo Version

A standalone, single-player Java 8 Swing implementation of Tiwa's Mah Jong:
one human seat versus three AI opponents, playing the full 16-hand game
(4 round winds × 4 dealer hands) under the rules in the project brief.

Zero external dependencies: only `javax.swing`, `java.awt`, `java.util`,
`java.io`, and `java.lang`. Java 8 source/target.

This is a **Demo Version** (the label is shown prominently in the UI).

## Architecture (strict package boundaries)

```
mahjong/
└── src/com/tiwas/mahjong/
    ├── model/                  # Domain only — no logic, no UI
    │   ├── TileSuit, Wind, Dragon, MeldType   # enums
    │   ├── Tile                # immutable tile (suit/rank/wind/dragon/flower/season)
    │   ├── Meld, Hand, Player, Wall, GameState # game pieces & state
    │   ├── ScoreSheet          # human-readable scoring breakdown
    │   ├── Constants           # all point / double values (§6)
    │   ├── Playable, Scorable  # interfaces the UI talks through
    ├── engine/                 # All rules, turn flow, claims, kongs, scoring, AI
    │   ├── GameEngine          # drives a full hand (deal → loop → win/draw → score → next)
    │   ├── HandAnalyzer        # win decomposition (4 sets + pair), 13 Orphans
    │   ├── ScoringEngine       # §6 scoring: base → bonus → doubles → cap → round down
    │   ├── ClaimResolver       # legal claims + Mahjong > Pung priority + tie-breaks
    │   ├── AIPlayerLogic       # chooseDiscard / decideClaim heuristics
    │   ├── WinContext, HandResult, TurnStatus  # engine ↔ UI carriers
    ├── ui/                     # Swing only — calls the engine via public methods
    │   ├── MainFrame, GamePanel               # window + table layout & controller
    │   ├── HandPanel, MeldPanel, WallPanel, ScorePanel, TileView, DialogUtils
    └── app/
        └── Main                # entry point; launches the UI and starts a game
```

The UI imports `model` and calls the engine through `Playable`, `GameEngine.advance()`,
and the `human*` input methods only — never engine internals. The engine imports only
from `model`. The model contains no logic or UI.

## How a turn flows

The UI loops: call `engine.advance()`, which plays out the AI seats internally and
returns a `TurnStatus` whenever it needs the human — to **draw**, to **discard**
(or declare a kong / mahjong), or to **claim** a discard (a ~6-second timer is shown;
on timeout the human passes). At hand end the UI shows the full scoring breakdown.

### Rules implemented

- 144-tile set; full wall, no dead wall; replacement tiles for flowers/seasons/kongs.
- Counter-clockwise play, clockwise draws; dealer dealt to 14.
- Claims from a discard: **Pung** or **Mahjong** only (no Chow, no Kong from a discard).
  Mahjong > Pung priority; multiple mahjong → nearest counter-clockwise to discarder.
- Concealed kong (self-draw) and exposed kong upgrade (own turn), each with a
  replacement draw; kong with no replacement → drawn game.
- Flowers/Seasons: revealed on draw, replacement drawn, 4 pts each, never double.
- Winning = 4 sets + a pair; chows are always concealed and worth 0.
- Fully Concealed Hand (never claimed a discard) → 2 doubles.
- Special limit hands: Thirteen Orphans and All Flowers & Seasons.
- §6 scoring order: base + flowers → add mahjong bonus (1% of limit) → apply
  doubles multiplicatively → cap at the limit → round down. False mahjong = −1000
  to each other player.

## Build & run from the command line

From the `mahjong/` directory:

```bash
# Compile everything (Java 8 compatible)
mkdir -p out
javac -source 8 -target 8 -d out -cp src $(find src test -name '*.java')

# Run the game (opens the Swing window)
java -cp out com.tiwas.mahjong.app.Main

# Run the self-contained test suite (no JUnit; exits non-zero on failure)
java -cp out com.tiwas.mahjong.EngineSmokeTest
```

A JDK 8 or newer is required (verified compiling to Java 8 bytecode with `javac 17`).
The test runner exercises hand analysis, scoring, and five full headless games.
