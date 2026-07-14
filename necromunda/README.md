# Necromunda — Underhive Skirmish Engine (Java 8 / Swing)

A clean-room, mechanics-first recreation of the **1995 (original edition) Necromunda**
tabletop skirmish game, built as a pure Java 8 desktop application with a Swing UI.

This is an original engine that follows the same mechanics, turn structure, and
systems as the 1995 box (+ *Outlanders*). No copyrighted text, art, logos, or
trademarked background lore is reproduced — only functional/mechanical names
("Gang Leader", "Heavy Weapon", etc.).

## Architecture (strict MVC, engine is headless)

```
com.whim.necromunda
├── model/            pure data, NO Swing/AWT imports
│   ├── Stat, StatLine, FighterType, FighterStatus, WeaponRule,
│   │   Weapon, Armour, House, Fighter, Gang
│   └── board/        Position, TerrainType, Cover, Tile, Board
├── engine/           rules logic, NO Swing/AWT imports
│   ├── Dice          seedable D6/D3/2D6 wrapper
│   ├── Phase         RECOVERY, MOVEMENT, SHOOTING, CLOSE_COMBAT, END
│   ├── GameState     single source of truth (board, gangs, phase, turn, log)
│   ├── TurnManager   drives the phase state machine
│   ├── rules/        RangedToHit, WoundTable, ArmourSave, InjuryResolver, MeleeContest
│   ├── data/         WeaponCatalogue (starter armoury, keyed by id)
│   ├── roster/       RosterRules (legality)
│   └── setup/        DemoSetup (24x24 demo board + two placed gangs)
├── ui/               Swing only
│   ├── MainWindow, BoardPanel, RosterEditorPanel/Frame,
│   │   PhaseControlPanel, LogPanel
│   └── render/       TerrainRenderer, FighterRenderer  (Java2D placeholders)
├── persistence/      dependency-free JSON (Json, GangCodec, SaveManager)
├── app/              Main  (wires UI on the EDT)
└── test/             zero-dependency main() harnesses
```

Dependency direction is strict: `ui → engine → model`. `model` depends on nothing
and imports no `javax.swing.*` / `java.awt.*`. The whole battle can be driven from a
`main()` with no window — which is exactly what the test harnesses do.

## Milestones

- **M1** — data model + rules engine + stat-math tests (`TestRunner`).
- **M2** — board model + custom-painted Swing board view (`BoardRenderSmoke`).
- **M3** — gang/roster editor + dependency-free JSON save/load (`PersistenceTest`).
- **M4** — turn/phase engine: `GameState`, `TurnManager`, the
  RECOVERY→MOVEMENT→SHOOTING→CLOSE_COMBAT→END state machine, a phase indicator
  + action-log panel, and `PhaseControlPanel` (Next Phase / End Turn). Hotseat;
  per-fighter actions arrive in M5.

## Run

Requires a JDK (compiled/verified at Java 8 source/target level).

```bash
cd necromunda
javac -d out $(find src -name '*.java')

# interactive window (board + Gangs menu + phase controls + log)
java -cp out com.whim.necromunda.app.Main

# headless verification
java -cp out com.whim.necromunda.test.TestRunner        # rules stat-math
java -cp out com.whim.necromunda.test.TurnEngineTest    # turn/phase state machine
java -cp out com.whim.necromunda.test.PersistenceTest   # JSON round-trip + roster rules
java -Djava.awt.headless=true -cp out com.whim.necromunda.test.BoardRenderSmoke   # board.png
```

## Faithfulness / design decisions

- **Alternating turns** after an initial roll-off (1995 box), not a per-turn priority roll.
- **Cover = to-hit penalty** (−1 partial / −2 hard), not a save bonus.
- **Injury bands**: 1–2 Flesh Wound / 3–4 Down / 5–6 Out of Action.
- **Close combat** is the ORB contest: roll 1 D6 per Attack, add WS + bonuses, highest total wins.
- **Ranged to-hit** target = `7 − BS` (clamped to 2..6), natural 6 always hits.
- Board model: 1 tile ≈ 1 inch, square grid, with a Z/level for the vertical underhive.
- Save format: hand-rolled JSON, zero external dependencies.
