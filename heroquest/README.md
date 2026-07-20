# HeroQuest (Java 8 / Swing)

A standalone digital adaptation of the classic 1989 board game **HeroQuest**.
Pure Java 8 + Swing + `Graphics2D` — **no external libraries, no Maven/Gradle**.
All dungeon art is drawn procedurally.

## Requirements
- A **Java 8+** JDK (`javac` / `java`). Source is written to Java 8 language level.
- A graphical display (it opens a Swing window).

## Build & Run
From this `heroquest/` directory:

```bash
# compile
javac -d out $(find src -name '*.java')

# run
java -cp out com.heroquest.Main
```

To enforce the Java 8 language level explicitly:

```bash
javac -source 8 -target 8 -d out $(find src -name '*.java')
```

## How to play
- **Move**: click a highlighted (blue) square to walk the active Hero there. Movement is 2d6, rolled at the start of each Hero's turn; you can spend it across several clicks.
- **Open doors**: click a closed door adjacent to the active Hero (free action). Rooms reveal as you see them; corridors reveal square-by-square (Fog of War).
- **Attack**: click **Attack**, then click a highlighted (red) adjacent monster. Combat uses the custom HeroQuest dice — 3 Skulls, 2 White Shields, 1 Black Shield. Each attacker Skull that the defender fails to block (Heroes block on White Shields, monsters on Black Shields) costs 1 Body Point. One action per turn.
- **Search / Cast Spell**: the Hero's single action alternatives (search the treasure deck, or cast a spell — Wizard & Elf only).
- **End Turn**: passes to the next Hero. After the last Hero, **Zargon** moves every discovered monster toward the nearest Hero and attacks.
- **New Quest**: restarts "The Trial".

Clear every monster to win; lose all four Heroes and Zargon wins.

## Architecture
The three packages map 1:1 to the three design responsibilities:

| Package | Responsibility |
|---|---|
| `com.heroquest.model` | Domain state: `DungeonMap`/`Tile`, `Entity`→`Hero`/`Monster`, dice faces, spells & decks, `GameState`. Pure data. |
| `com.heroquest.logic` | Rules engine: `CombatEngine` (opposed dice), `TurnManager` (turn state machine), `Visibility` (line-of-sight / Fog of War), `Pathfinding`, `ZargonAI`. Mutates the model. |
| `com.heroquest.ui` | Swing presentation: `BoardPanel` (procedural `Graphics2D` board), `SidePanel` (dashboard + log), `GameController` (view↔logic mediator), `GameFrame`. Reads model, calls logic via the controller only. |

`com.heroquest.QuestFactory` assembles the sample quest; `com.heroquest.Main` launches the EDT.

## Verified
- Compiles cleanly at Java 8 language level (`-source 8 -target 8`).
- Headless smoke test exercises quest setup, door/reveal, Hero movement & pathfinding, and multiple Hero→Zargon rounds with combat — no runtime errors.
- The Swing window itself was not launched here (headless container); run the command above on a machine with a display.
