# The Warlock of Firetop Mountain — Boardgame (unofficial Java 8 / Swing recreation)

A playable, self-contained desktop recreation of the **1986 Fighting Fantasy
boardgame** *The Warlock of Firetop Mountain*: a turn-based dungeon crawl beneath
the mountain for **1–4 local hot-seat players**, culminating in a confrontation
with the warlock **Zagor**. Built with Java 8 and Swing/AWT only.

> ### Disclaimer
> This is an **unofficial, fan-made, educational recreation**. It is **not
> affiliated with, endorsed by, or connected to Games Workshop, Steve Jackson,
> Ian Livingstone, or any rights holder** of *Fighting Fantasy* or *The Warlock of
> Firetop Mountain*. **No copyrighted text, artwork, logos, or trademarks are
> reproduced.** All rooms, monsters, items, cards and flavor text are original
> writing; all graphics are drawn programmatically. The original title is used
> only to describe what this hobby project recreates.

## What it is

You lead a party of adventurers, each defined by the three Fighting Fantasy
attributes — **SKILL**, **STAMINA**, **LUCK** — through a graph of dungeon rooms.
On your turn you roll to move, then resolve the room you enter: fight a monster in
opposed **2d6 + SKILL** combat, draw treasure/event cards, spring traps, or receive
a blessing. Reach Zagor's throne room and defeat him to **win**; if the whole party
dies, you **lose**.

The faithful Fighting Fantasy core (dice generation, opposed combat, Test Your Luck
with the correct combat modifiers, provisions) is implemented exactly and covered
by unit tests. Boardgame-specific structure that could not be verified against a
primary source (exact tile layout, card list, Zagor's printed stats) is
reconstructed and clearly flagged — see `RESEARCH.md` and `CHECKLIST.md`.

## Build & run

Requires a **Java 8+ JDK**. The project targets Java 8 (`source/target 1.8`) and
makes **no network calls** — it is fully offline.

### With Maven (single command)
```bash
mvn -f warlock/pom.xml package
java -jar warlock/target/warlock-of-firetop-mountain-1.0.0-SNAPSHOT.jar
```
`mvn package` compiles, runs the JUnit tests, and produces a runnable jar with
`com.whim.firetop.ui.Main` as the entry point.

### Without Maven (javac only)
```bash
cd warlock
find src -name '*.java' > sources.txt
javac --release 8 -d out @sources.txt
jar cfe warlock.jar com.whim.firetop.ui.Main -C out .
java -jar warlock.jar
```

## Controls

- **Mouse:** click a green (reachable) room to move; click buttons and menu items.
- **Roll to Move** (Alt+R) → reachable rooms glow green and appear as destination
  buttons → click a room or a destination button to move.
- **End Turn** (Alt+E), **Eat Provision** (Alt+P, +4 STAMINA), **Use Item** (Alt+U).
- **Space** triggers the primary action (Roll, else End Turn).
- **In combat:** Attack Round (Alt+A), Test Your Luck (Alt+L, when offered),
  Eat Provision (Alt+P), Continue (Alt+C).
- **Menu → Game:** New Game, Save…, Load…, How to Play, Exit.

## Rules: implemented vs adapted

**Implemented faithfully (FF core, unit-tested):** attribute generation
(SKILL 1d6+6, STAMINA 2d6+12, LUCK 1d6+6); opposed 2d6+SKILL combat with 2-STAMINA
wounds and ties; Test Your Luck (2d6 ≤ LUCK, −1 LUCK per test) with the correct
combat modifiers for pressing an attack (+2 / graze) and softening a blow
(−1 loss / +1 loss); death at 0 STAMINA; current-never-above-initial clamping;
card decks with shuffle/draw/discard/reshuffle; save/load round-trip; 1–4 hot-seat
turn order skipping fallen adventurers; win (defeat Zagor) / lose (party wipe).

**Adapted / adopted rulings (documented, because a primary source could not be
verified in this build):** the 16-room dungeon layout; dice-based movement (1d6
steps); the original three-deck card set; provisions healing +4; Zagor's stat line
(SKILL 12 / STAMINA 18). See `RESEARCH.md` §3 for the full audit and
`CHECKLIST.md` for the rule-by-rule mapping.

## Project layout

```
warlock/
├── pom.xml                     Maven build (Java 8, JUnit 4.13.2, runnable-jar)
├── RESEARCH.md  DESIGN.md  ASSETS.md  CHECKLIST.md  README.md
├── src/com/whim/firetop/
│   ├── model/                  entities: Character, Monster, Item, Card, Room, Board, GameState, Content
│   ├── engine/                 rules: Dice, LuckTest, Combat, Deck, GameEngine
│   ├── persistence/            SaveGame (Java serialization)
│   └── ui/                     Swing: Main, GameFrame, BoardPanel, StatusPanel, CombatDialog, CardDialog, SetupPanel, HowToPlayDialog, EndScreen, Theme
└── test/com/whim/firetop/
    ├── engine/                 DiceTest, LuckTestTest, CombatTest (18 tests)
    └── SmokeTest.java          headless end-to-end playthrough + save/load
```

## Verification

- `javac --release 8` compiles all sources cleanly (Java 8 language + API level).
- **18/18 JUnit tests** pass (dice, combat, luck).
- Headless `SmokeTest` plays a full game to a Zagor victory and verifies a
  save/load round-trip.
