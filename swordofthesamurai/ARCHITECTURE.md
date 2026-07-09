# Sword of the Samurai — Architecture

This document describes the system/class design of the Java 8 + Swing recreation.
Every gameplay rule referenced here is traceable to `GAME_DESIGN_REFERENCE.md` (the
research document); section markers like *(design ref §2a)* point back to it.

## 1. Goals & constraints

- **Java 8 only**, **Swing only**, **no third-party dependencies** (JDK only).
- Clean separation of **state / logic / rendering / input** — no god-classes.
- All art is **procedurally drawn with Java2D** or clearly-labelled placeholders;
  no assets from the original game.
- Runnable as a single `main` class: `com.whim.samurai.SwordOfTheSamurai`.

## 2. Package layout

```
com.whim.samurai
├─ SwordOfTheSamurai        // main(): builds Game, registers screens, shows menu
├─ app/                     // application framework (no game rules)
│  ├─ Game                  // shared context: GameState + Rng + ScreenManager + hand-off fields
│  ├─ Screen                // abstract JPanel base for a full-window screen
│  └─ ScreenManager         // CardLayout-backed screen switcher (by name)
├─ model/                   // pure serialisable data (no Swing, no logic)
│  ├─ GameState             // the whole save-able world
│  ├─ Samurai               // player / heir: honor, power, skills, koku, family
│  ├─ FamilyMember          // wife / sons / daughters (heirs, kidnap targets)
│  ├─ Province, Clan, Rival // strategic map + politics
│  ├─ Rank, Season, Calendar
├─ engine/                  // game rules & simulation (no Swing)
│  ├─ Rng                   // seedable randomness helpers
│  ├─ WorldGen              // builds a fresh GameState
│  ├─ TurnEngine            // season advance, income, AI clan/rival actions
│  ├─ PoliticsEngine        // favour, promotion, province conquest, territory win/loss
│  ├─ DuelEngine / BattleEngine / StealthEngine   // action-sequence resolution
│  ├─ HonorEngine           // honor/power bands, consequences, dynasty score
│  ├─ FamilyEngine          // marriage, births, aging, succession, heir-less game over
│  └─ SaveManager           // GameState (de)serialisation to ./saves/
├─ render/                  // reusable Java2D drawing (Palette, sprites)
└─ ui/                      // one Screen per game screen (Swing views + input)
   ├─ MainMenuScreen, CharCreateScreen, HelpScreen, GameOverScreen
   ├─ MapScreen             // strategic hub
   ├─ CharacterScreen, FamilyScreen
   └─ DuelScreen, BattleScreen, NinjaScreen, EncounterScreen  // the arcade sub-games
```

**Dependency direction:** `ui` → `engine` → `model`. `model` depends on nothing
but the JDK. `render`/`ui` may use `app`. Rules never live in `ui`; screens call
into `engine` and render `model`.

## 3. State machine (screen flow)

`ScreenManager` is a finite state machine over `CardLayout`. Transitions are by
name (constants on `Game`). The core flow mirrors the original's screen tree:

```
MENU ─newgame→ CREATE ─→ MAP ⇄ CHARACTER
                          MAP ⇄ FAMILY
                          MAP ─action→ {DUEL, BATTLE, NINJA, ENCOUNTER} ─→ MAP
                          MAP ─death / win / heirless→ GAMEOVER ─→ MENU
MENU ─continue→ MAP        (any screen) ─help→ HELP
```

`MAP` is the strategic hub. Action sequences are launched by setting a hand-off
field on `Game` and switching cards; the action screen resolves via its engine,
applies the outcome to `GameState`, then calls `game.returnFromAction()`.

## 4. Cross-screen hand-off contract

Screens never call each other directly — they communicate only through `Game`
fields + `GameState` + card names, so each screen can be developed independently:

| Launch | Set on `Game` | Then |
|---|---|---|
| Sword duel *(§2a)* | `duelTarget` (Rival), `duelReason`, `duelToDeath`, `afterAction` | `screens.show(DUEL)` |
| Field battle *(§2b)* | `battleTarget` (Province), `afterAction` | `screens.show(BATTLE)` |
| Ninja infiltration *(§2c)* | `ninjaTarget` (Province), `afterAction` | `screens.show(NINJA)` |
| Random encounter *(§2)* | (reads world state) | `screens.show(ENCOUNTER)` |
| Game over *(§8)* | `state.gameOver`, `state.victory`, `state.gameOverReason` | `screens.show(GAMEOVER)` |

## 5. Data model

`GameState` is the single serialisable aggregate root; `SaveManager` writes the
whole tree with `ObjectOutputStream`. All model classes implement
`Serializable` with an explicit `serialVersionUID`. Key entities:

- **Samurai** — the player *or* the heir who continues after death. Holds the two
  central currencies **honor** and **power** *(design ref §3)*, the three arcade
  skills (swordsmanship / generalship / stealth), `koku` (wealth), owned `fiefs`,
  and the household (`wife`, `children`).
- **Clan / Province / Rival** — the political layer *(§6)*: clans own provinces;
  rivals inside your clan compete for the daimyo's favour, rivals in enemy clans
  defend the provinces you assault.
- **Calendar / Season** — the strategic clock; one turn = one season *(§1)*.

## 6. Honor vs Power (the design's spine)

Both axes are first-class integers on `Samurai`. `HonorEngine` owns their bands
and consequences: honourable acts (won duels, completed quests, rescues) raise
honor; dishonourable acts (assassination via `NINJA`, ambushes) raise power but
cost honor *(design ref §3)*. Becoming Shogun requires enough of **both** land
(power) and standing (honor); the final **dynasty score** blends them at death or
retirement.

## 7. Save / load

Single slot at `./saves/sword_save.ser`. `SaveManager.save/load` serialise
`GameState`. "Continue" on the main menu is enabled when `SaveManager.saveExists()`.

## 8. Build / run

Java 8 bytecode via `javac --release 8` (or Maven `maven.compiler.source/target=1.8`).
Data JSON (if any) is bundled from `src/**/ *.json`. Entry point:
`com.whim.samurai.SwordOfTheSamurai`. See `README.md` for exact commands.

## 9. Implementation ownership (parallel build)

The game was built as three independent slices over this shared skeleton, each
touching a **disjoint** set of files and referencing only skeleton classes:

- **Campaign** — `MapScreen`, `WorldGen`, `TurnEngine`, `PoliticsEngine`.
- **Action** — `DuelScreen`/`BattleScreen`/`NinjaScreen`/`EncounterScreen` + their
  engines + `render/SamuraiSprite`.
- **Dynasty** — menus, `CharacterScreen`, `FamilyScreen`, `GameOverScreen`,
  `HonorEngine`, `FamilyEngine`.

They integrate purely at runtime through the §4 hand-off contract.
