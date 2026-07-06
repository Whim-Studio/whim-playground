# Star Command — Java 8 / Swing recreation

A standalone, zero-dependency **Java 8 + Swing** desktop game that recreates the *spirit and
mechanics* of **Star Command** (Strategic Simulations, Inc., 1988; designed by Winston Douglas
Wood). It is an original re-implementation: all graphics are drawn procedurally with Java2D and
all text is original — **no art, sprites, manual scans, or verbatim text from the original game
are used**.

You command a Star Command officer: recruit a crew, outfit a starship, take missions from HQ,
fly out into "The Triangle," fight pirates and insectoid aliens ship-to-ship, **disable and
board** enemy vessels, and hunt down the pirate warlord **Blackbeard**.

## What's implemented (vertical slice)

| Milestone | Status |
|-----------|--------|
| Main menu + starfield | ✅ |
| Character creation (crew of up to 8, roles, stat rolling) | ✅ |
| Starport / shop (weapons, ship trade-up, repair, missions, save/load) | ✅ |
| Galaxy map (grid navigation, planet scanning) | ✅ |
| Ship-to-ship combat (turn-based, disable-vs-destroy) | ✅ |
| Tactical ground / boarding combat (squad on a tile grid) | ✅ |
| Multi-room "unique area" crawl (corridor maze, fog of war, loot/enemy/objective rooms) | ✅ |
| Blackbeard mission + win condition (space, boarding, **or** stronghold raid) | ✅ |
| Data-driven content (ships/weapons/roles in external JSON) | ✅ |
| Broader mission variety, Beta Frontier campaign | ⏳ next milestone |

## Build & run

Requires a **JDK 8+** (compiled and tested against Java 8 bytecode via `--release 8`).

### Plain JDK
```bash
cd starcommand
javac -d out $(find src -name '*.java')
cp -r src/com/whim/starcommand/data out/com/whim/starcommand/   # bundle JSON data on the classpath
java -cp out com.whim.starcommand.StarCommand
```
(The `cp` step mirrors what Maven's resource copy does automatically; the game loads its
ship/weapon/role tables from those JSON files at startup.)

### Maven
```bash
cd starcommand
mvn -q clean package
java -jar target/starcommand-1.0.0-SNAPSHOT.jar
```

### Tests (headless)
```bash
mvn -q test
```

## Controls

Every screen supports **mouse and keyboard**; shortcut letters appear in `()` on the buttons.

| Screen | Keys |
|--------|------|
| Main menu | `N` new · `C` continue · `H` help · `Q` quit |
| Crew | `R` re-roll · `A` add · `Enter` launch |
| Starport | click Buy/Accept · `S` save · `G` launch to galaxy |
| Galaxy map | arrow keys move · `S` scan · `D` deploy drop ship · `B` dock at HQ |
| Ship combat | `1` beam · `2` missile · `3` shields · `4` disable · `5` flee |
| Ground combat | click/arrows move · `A` attack · `Tab` next unit · `Space` end turn |
| Unique area | arrows/click move room · `X` extract (from the entrance) |

## Architecture

```
com.whim.starcommand
├── StarCommand        entry point / frame wiring
├── app                Game context, Screen base, ScreenManager (CardLayout)
├── model              Character, Ship, Weapon, Planet, Sector, Mission, GameState (POJOs)
├── engine             Rng, Json, Content (loader), CharacterGen, GalaxyGen,
│                      CombatEngine, GroundCombat, UniqueAreaGen, SaveManager
├── data               ships.json, weapons.json, roles.json (tunable content)
├── ui                 one class per screen + UiKit/Keys widgets
└── render             Palette, Starfield, ShipSprite (procedural Java2D art)
```

Game/combat logic in `engine` is fully decoupled from Swing so it can be unit-tested headlessly
(`test/`).

## Known deviations from the 1988 original (and why)

- **Data-driven content**: ship/weapon/role tables live in external JSON (`data/*.json`), loaded
  at startup by a small dependency-free reader (`engine.Json`). Enemy stat blocks and the mission
  ladder are still in code — externalizing those is a follow-up.
- **Save format** uses Java serialization rather than JSON, for the same zero-dependency reason.
- **Combat model** is a simplified range-agnostic exchange (shields soak, then hull; a "disable"
  called-shot trades damage for a boardable capture). The original's exact to-hit/subsystem math
  is not fully documented in secondary sources; values here are tunable and were balance-swept so
  the opening raider fight is winnable with starting gear.
- **Ground/boarding combat** is a real turn-based tactical grid (squad move + ranged/melee
  attacks, enemy AI). Boarding a disabled ship drops you into a single-deck fight; deploying a
  drop ship onto a hostile base/hive opens a **multi-room "unique area" crawl** (fog of war;
  loot, enemy, and objective rooms; enemy rooms hand off to the tactical grid and return).
  Rooms are a fully-connected grid — corridor/wall layouts are a future refinement.
- **Stat names/ranges, school list, and the endgame chain** are a reasonable reconstruction where
  sources conflict — see `GDD.md` for each assumption.

## Sources consulted

Reconstructed from general knowledge of the title plus standard secondary sources: MobyGames
entry, The CRPG Addict's playthrough series, and contemporaneous magazine coverage
(Computer Gaming World, Dragon #138). No primary text was copied.
