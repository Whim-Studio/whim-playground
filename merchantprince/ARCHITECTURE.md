# Merchant Prince — Architecture

Java 8, Swing only, no third-party runtime deps. Save/load = Java serialization of the
whole `GameState` tree. Strict model / engine / view separation (no god-classes).

## Packages
- `com.whim.merchantprince` — `MerchantPrince` (entry point).
- `model` — pure serialisable data: `GameState`, `Family`, `City`, `TransportUnit`,
  enums `Good`, `UnitType`, `Office`; `model.event` (`Event`, `EventType`). No Swing.
- `engine` — all rules, no Swing: `Rng`, `Constants` (every tunable number),
  `PricingEngine`, `TravelEngine`, `EventEngine`, `PoliticsEngine`, `WinConditions`,
  `AIPlayer`, `TurnManager`, `SaveManager`.
- `data` — `WorldFactory` builds a fresh game (families, city map, starting units).
- `app` — `Game` (shared context), `Screen`, `ScreenManager` (CardLayout).
- `render` — `Palette`. `ui` — `UiKit` + one `Screen` per game screen.

## Screen flow (CardLayout)
`MainMenu → NewGame → Map ⇄ {Market, Fleet, Venice} → GameOver`. The Map hosts the
top nav and **End Turn**; `TurnManager.endTurn` resolves rivals → unit movement/hazards
→ world events → price drift → year++ → win check.

## Engine contracts (frozen at T0)
- `PricingEngine.buyPrice/sellPrice/driftPrices`
- `TravelEngine.turnsBetween/dispatch/advanceUnits`
- `EventEngine.rollYearlyEvents`
- `PoliticsEngine.buyOffice/bribeSenator/bribeCardinal/buildDen/dirtyTrick`
- `WinConditions.netWorth/checkVictory`
- `AIPlayer.takeTurn`
- `WorldFactory.newGame`

## Parallel build ownership
- **T1 Economy**: `PricingEngine`, `TravelEngine`, `EventEngine`, `WorldFactory` (full map).
- **T2 Politics**: `PoliticsEngine`, `WinConditions`, `AIPlayer`.
- **T3 Map/Fleet UI**: `MapScreen`, `FleetScreen`.
- **T4 Market/Venice UI**: `MarketScreen`, `VeniceScreen`, event dialogs.
- **T5 Integration**: finalise `TurnManager` wiring, compile, playtest.

Model/enum signatures are the shared contract — engines and UI extend behaviour behind
these signatures without changing them, so tasks don't collide.
