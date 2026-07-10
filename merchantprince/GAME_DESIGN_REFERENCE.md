# Merchant Prince (1994) — Game Design Reference

A clean-room recreation of the QQP (Quantum Quality Productions) 1994 trading &
political-intrigue strategy game. This document records what research confirmed and,
explicitly, what could not be confirmed and is therefore a tunable **assumption**.

> **Source note:** Web search was unavailable on the build proxy; the primary
> reachable source was the game's encyclopedia entry. MobyGames returned HTTP 403.
> Mechanics below marked *(assumption)* are internally-consistent design choices, not
> facts lifted from the original, and live as constants in `engine/Constants.java`.

## 1. Core gameplay loop (confirmed)
You lead one of **four Venetian merchant families** (1300–1492) competing to be the most
powerful clan. A turn (one year) mixes: buy/sell goods where your traders can reach,
move fleets/caravans along routes, conduct politics in Venice, then end the turn.

## 2. Map & travel (confirmed)
- Afro-Eurasian map (a randomized-map option existed).
- Cities from real (Venice, Khanbaliq) to legendary (Shangri-La / "Xiangrala").
- Many cities **start closed** and must be bribed or forced open.
- Travel is distance/time based; exploration is a core draw.

## 3. Economy & goods (confirmed goods, *assumed* formula)
- Goods: **Venetian glass, French wine ("grog"), Holy relics, African gold, ivory,
  Chinese silk, spices** (no salt or slaves).
- Income: uncle's stipend, indulgences, government offices, and trade.
- *(assumption)* Price model: local price rises inversely with local stock around a
  reference level; base prices mean-revert yearly with a random shock.

## 4. Transport units (confirmed)
Small/large galleys (fast, storm-prone), small/large cogs (slow, storm-resistant),
donkey teams and camel caravans; up to 15 units may travel grouped.

## 5. Events & hazards (confirmed events, *assumed* odds)
Plague, papal interdicts, wars, an early Reformation; storms and piracy at sea.

## 6. Politics & intrigue (confirmed)
Bribe the Council of Ten for offices (minister, admiral, general, council head),
become Doge; influence cardinals toward controlling the papacy; a "den of iniquities"
enables arson, rumour and assassination — being caught ruins reputation.

## 7. Win / loss (confirmed)
First to **1,000,000 florins** wins immediately; otherwise **highest net worth** at the
end year — net worth **includes** the value of bribed senators and cardinals.

## 8. Dynasty (confirmed)
Choose a **family crest and surname** (not an individual name); a late uncle Niccolò's
letter frames the start; the goal spans one or several lifetimes.

## Known Gaps / Assumptions
1. Exact pricing formula — *assumption*, tunable in `Constants`.
2. Precise city list & distances — *assumption*, curated representative map.
3. Exact UI layout & controls — *assumption*, CardLayout screen flow.
4. Turn/action economy — *assumption*, year-turns; trading free while docked.
5. Bribery/office cost tables & papacy control — *assumption*, constant-driven.
6. Combat/piracy resolution — *assumption*, per-leg probabilistic hazards by ship type.
