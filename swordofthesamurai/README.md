# Sword of the Samurai — Java 8 / Swing recreation

A standalone, zero-dependency **Java 8 + Swing** desktop game that recreates the *spirit and
systems* of **Sword of the Samurai** (MicroProse, **1989**; design lead Lawrence Schick, with
Sid Meier contributing the NPC AI and dueling). It is an original, clean-room re-implementation:
**all graphics are drawn procedurally with Java2D and all text is original** — no art, sprites,
manual scans, or verbatim text from the original game are used.

You are a young samurai (a *gokenin*) heading a leading family in Sengoku-era Japan. Serve your
liege daimyo, balance **Honor** and **Power**, fight **sword duels**, lead **battles**, infiltrate
castles as a **ninja**, marry and raise an **heir**, and climb *gokenin → hatamoto → daimyo →
Shogun* — founding a dynasty whose "Verdict of History" is scored when your line ends.

> **Date correction:** the task brief called this a *1992* title; the verified release year is
> **1989**. See the top of `GAME_DESIGN_REFERENCE.md`.

## Documents

| File | What it is |
|---|---|
| `GAME_DESIGN_REFERENCE.md` | The research document — mechanics/numbers/controls reconstructed from the original 1989 manual + corroborating sources, with every unverifiable detail tagged `[UNVERIFIED]`. |
| `ARCHITECTURE.md` | System/class design: MVC split, screen state machine, data model, hand-off contract. |
| `README.md` | This file — build/run, controls, fidelity notes. |

## What's implemented

| System | Status |
|---|---|
| Main menu → new game / continue / help | ✅ |
| Character creation (name, clan, skill allocation) | ✅ |
| Strategic **provincial map** (48 provinces, clan colours, adjacency), seasonal turns | ✅ |
| Per-season **home actions** (improve estate, patrol, duel a rival, raise troops, besiege, infiltrate, attend daimyo, end season) | ✅ |
| **Sword Duel** action game (stances, strike lines, parry, charged cut, 4 wounds) | ✅ |
| **Battle** action game (named formations + counters, advance/hold/charge/retreat, troop tiers) | ✅ |
| **Ninja** infiltration (Melee-engine variant: stealth, guards, shuriken, honor↓/power↑) | ✅ |
| Random **encounters** (bandits, rival insults → duel, roadside events) | ✅ |
| **Honor vs Power** dual axis with descriptive bands + consequences | ✅ |
| **Marriage / children / heir**, aging, **succession** (continue as your heir) | ✅ |
| **Win** (proclaimed Shogun: hold Omi + ≥24 provinces) / **loss** (death without heir, dishonor, conquest) | ✅ |
| **Save / load** (single slot, whole-state serialization) + **dynasty score** | ✅ |

## Build & run

Requires a **JDK 8+** (compiled and tested to Java 8 bytecode via `--release 8`).

### Plain JDK
```bash
cd swordofthesamurai
javac --release 8 -d out $(find src -name '*.java')
java -cp out com.whim.samurai.SwordOfTheSamurai
```

### Maven
```bash
cd swordofthesamurai
mvn -q package
java -jar target/swordofthesamurai-1.0.0-SNAPSHOT.jar
```

Saves are written to `./saves/sword_save.ser`.

## Controls

- **Menus / map:** mouse for buttons; on-screen key hints where bound. The map is the strategic hub —
  each button is a home action or a screen jump (Character, Family, Help). "End Season" advances the clock.
- **Sword Duel:** on-screen prompts show the stance (high/middle/low), strike, parry-side, and the
  charged over-shoulder cut. A duel ends at 4 wounds (or first blood, depending on how it was provoked).
- **Battle:** pick a formation, then issue advance / hold / charge / retreat each round.
- **Ninja:** move to sneak past guards' vision, throw shuriken; a raised alarm turns the sequence into a
  2-wound melee.

Exact key letters/timings differ from the original DOS controls (those were `[UNVERIFIED]` in research);
the on-screen hints are authoritative for this build.

## Fidelity: faithful reconstructions vs. approximations

**Faithful to the manual** (see `GAME_DESIGN_REFERENCE.md`): the *gokenin → hatamoto → daimyo → Shogun*
ladder; **Honor vs Power** as the twin currencies; three action engines (**Duel / Melee / Battle**) with
ninja infiltration as a Melee variant; **48 provinces** with Omi as the imperial prize and the **≥24-incl.-Omi**
Shogun threshold; the named battle formations with counters; the 6/60/250 troop-per-figure scale;
duels resolved over ~4 wounds and melees over ~2; seppuku/succession and the multi-generational dynasty.

**`[UNVERIFIED]` / best-effort approximations** (labelled in code comments): exact per-hit damage,
hit-probability slopes, the honor/power band cut-points and the `dynastyScore` formula (the manual shows
honor as an icon scale and never prints the score formula), the seasonal-turn discretisation and the
stylised province coordinates/adjacency, and the exact original DOS key bindings. All placeholder/procedural
art is Java2D and clearly intended to be swapped later.

## How it was built

Over a shared skeleton (framework + serialisable data model + `WorldGen`/`SaveManager` + palette/UI kit),
three independent slices were developed in parallel and integrated at runtime purely through the
`Game` hand-off contract (`ARCHITECTURE.md` §4): **Campaign** (map + politics + turns), **Action**
(the duel/battle/ninja/encounter sub-games + engines + sprite), and **Dynasty** (menus, character/family/
game-over screens, honor + family engines, save/load wiring).
