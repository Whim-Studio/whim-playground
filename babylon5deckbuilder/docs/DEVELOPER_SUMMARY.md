# Developer Summary

## What this is

A Java 8 / Swing deck-building adaptation of the Babylon 5 CCG, generated from
the project GDD (`babylon5/docs/B5_DECKBUILDER_GDD.md`). It is a **self-contained
Maven module** with a modular engine, a JSON card-effect DSL, rule-based and
Monte-Carlo AI, a deterministic headless simulation harness, save/load, TTS CSV
export, unit + integration tests, and a functional local hotseat UI. No runtime
dependencies beyond the JDK (a ~180-line JSON parser lives in `io/Json.java`).

## Architecture at a glance

MVC with an engine core (`model` / `engine` / `ai` / `io` / `sim` / `app` / `ui`).
The **UI and the simulator drive the same `GameEngine`**, so interactive and
headless play share identical, seed-reproducible rules. The five-phase loop
(START → STRATEGY → ACTION → ACQUISITION → CLEANUP) is the single source of
truth; the only decision delegated to an `Agent` (or a human) is which cards to
acquire. Details and a class diagram are in `DEVELOPER_GUIDE.md`.

## Faction flavour preserved from the GDD

Cards use real B5 names, factions and stats: Centauri intrigue (Londo, Refa,
Imperial Telepaths), Narn military/vengeance (G'Kar, G'Sten, Heavy Fleet,
Revenge), Minbari psi/defence/tempo (Delenn, Shakiri, Grey Council Fleet), and
Earth's flexible toolbox (Sheridan, Garibaldi, Bester, Alliance of Races).
Ambassadors are cost-0 Heroes that begin in the COMMAND_ROW; the generic
10-card starter (7 Credit Chit + 2 Minor Diplomat + 1 Junior Officer) matches
the GDD. INFLUENCE is the spend currency; PRESTIGE is the win metric.

## Fully implemented

- Engine: deterministic 5-phase loop, zones, markets (hybrid RIM + CORRIDOR),
  Ally Bonus, DIPLOMACY→INFLUENCE conversion, board conflict resolution,
  end-game triggers and tiebreak.
- JSON card-effect DSL + loader (bundled resources **and** external `assets/cards`,
  so adding a JSON file and restarting exposes the card).
- AI: `RandomAgent`, `HeuristicAgent` (EASY/NORMAL/HARD), `MonteCarloAgent`.
- Headless `Simulator` + `BalanceReport` (CSV/JSON, 20–35% win-rate band check),
  fully deterministic per seed.
- Save/load (JSON snapshot) and Tabletop Simulator CSV export.
- Swing hotseat UI: Main Menu, New Game setup (2–5 seats), Game Board with
  market/tableau/scoreboard/log, and an Endgame summary; accessible fonts and
  keyboard mnemonics.
- Tests: `EngineTest` (shuffle/draw, purchase, permanent routing, conflict win/loss,
  agenda ticking, determinism) and `SimulationIT` (100 deterministic games).

## Scoped as extension points (not fully built here)

These are specified in the GDD and wired as clear seams, but intentionally left
as next steps to keep the core tight and verifiable:

- Head-to-head opponent conflicts and PSI-negation (only board contests auto-resolve).
- Conditional / triggered effects (e.g. "on winning an Intrigue conflict, force a
  discard") — effects currently fire on play; the trigger system is the natural
  next DSL addition.
- The full 150-card catalogue and 4×5 faction tiers (a representative ~39-card
  set ships in `resources/cards/cards.json`; the loader already caps at 150).
- Richer market tiers (early/mid/late pools) — `Market` isolates this.
- Polished per-card art / Card Detail modal (placeholder text renderer today).

## Verification status (important)

This module was authored in an environment **without a JDK or Maven installed**,
so it was **not compiled or executed here**. The code is written to Java 8 and
the standard Maven layout and is intended to build with `mvn package`. The
included `reports/balance_sample.*` files are **illustrative placeholders**, not
output from an executed run — regenerate them with the `--sim` command once you
have built the JAR. Treat a first local `mvn package` as the acceptance check.

## Next-steps checklist

1. `mvn package` locally (JDK 8 + Maven); fix any environment-specific issues.
2. Run `--sim --games 1000` and tune `GameConfig` / card costs to hit the
   20–35% per-faction band; commit the real balance report.
3. Add the trigger system to the DSL; wire head-to-head conflicts + PSI-negation.
4. Grow the catalogue toward 150 cards and the 4×5 faction tiers.
5. Card Detail modal + placeholder art; move AI turns off the EDT if MC is used
   in the UI.
6. (Later) online multiplayer: extract a headless `GameEngine` server session,
   add a serialised action protocol (the JSON save format is a starting point),
   and a lobby/networking layer — the engine is already UI- and IO-agnostic.
