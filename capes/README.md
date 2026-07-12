# CapesTabletop

A Java 8 + Swing digital tabletop for **Capes** (Tony Lower-Basch, Muse of Fire Games, 2005) — a GM-less
super-hero RPG. Local, pass-and-play (hotseat) for 2+ players on one shared screen. No network, no external
dependencies beyond the JDK (JUnit for tests only). All art is programmatically drawn; no copyrighted material.

> Clean-room digitization. Terminology (Stakes, Claims, Resolve, Drives, Click-and-Lock, Gloating, Comics Code)
> follows the 2005 source text. Where the rulebook is ambiguous, interpretations are documented, not invented.

## Build & run

```bash
mvn test        # compile + run the rules-core unit tests
mvn package     # build the jar
java -cp target/classes com.whim.capes.app.Main
```

No Maven? Plain JDK works too:

```bash
javac --release 8 -d out $(find src -name '*.java')
java -cp out com.whim.capes.app.Main
```

## Architecture (Phase 1)

```
com.whim.capes
  app/      Main — entry point, seeds a demo table, launches the Swing shell
  model/    Pure domain: Die, Ability, Drive, Stake, Inspiration, ConflictSide,
            Conflict, Character, Exemplar, Player, Scene, Page, GameState, EventLog
            + enums DriveType, AbilityKind, ConflictType
  engine/   GameEngine — stateful core loop (Scenes/Pages/Actions/Conflicts,
            Stake/Split, Abilities, Reactions, Resolve, Gloat, participants);
            RulesMath (pure arithmetic); CharacterFactory; Roller; ResolveResult
  content/  ClickLockData (15 Power-Sets, 17 Skill-Sets, 17 Personae),
            ExtendedData (20 Ch.5 participants), module records
  io/       Persistence — save/load the whole GameState (Java serialization)
  ui/       MainFrame (CardLayout router + File menu), TableView (interactive),
            CharacterCreationView, CharacterSheetView, ComicsCodeView, RulesHelpView,
            AbilityColumnEditor, DrivesEditor, EventLogPanel, Palette, UiKit, View
```

The rules logic in `engine` and `model` is deliberately decoupled from Swing so it is testable headlessly
(`test/com/whim/capes/engine/RulesMathTest.java`).

## Phase roadmap

- **Phase 0** — Rules digest & ambiguity resolutions ✅
- **Phase 1** — Architecture, data model, Swing shell ✅
- **Phase 2** — Click-and-Lock + Freeform character creation, rendered sheet ✅
- **Phase 3** — Core game loop: Scenes, Pages, Actions, Conflicts, Stake/Split, Abilities, Reactions, Resolve ✅
- **Phase 4** — Overdraw penalty rolls, Gloating/Comics Code, Ch.5 participant types ✅
- **Phase 5** — Save/load, rules-reference panel, polish ✅

All five build phases are complete. 27 unit tests cover the rules core (`mvn test`).
