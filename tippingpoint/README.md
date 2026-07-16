# Tipping Point — clean-room Java 8 / Swing recreation

A from-scratch, standalone desktop recreation of **Tipping Point** (Ryan Smith /
Treecer, 2020): a city-building economy game coupled to a compounding
**global-CO₂ climate feedback loop**. As cities grow they emit CO₂; rising CO₂
escalates extreme-weather disasters; crossing the global tipping point is a
**collective loss for everyone**.

> **Clean-room / no original assets.** All code is original. Every graphic is
> placeholder art drawn procedurally with `Graphics2D` (colored polygons, tracker
> cylinders, typography) — no Treecer artwork, components, or text are used. This
> is an independent, non-commercial fan recreation of the game's *rules and
> mechanics* (which are not copyrightable) and is not affiliated with or endorsed
> by the designer or publisher. See `../TIPPING_POINT_CONTRACT.md` for the spec.

## How it plays

- **2–4 players**, **competitive** or **cooperative**; any seat can be human or AI.
- Each player has a **Status Board** tracking three wooden cylinders — **Cash Flow**,
  **Food Production**, **Net CO₂** — plus a derived **Risk Factor** (×1 / ×2 / ×3)
  that scales the weather damage their city takes.
- Three card types: **Citizen** (population of 1; workers eat, farmers feed the city),
  **Development** (bought from a 3×4 Central Market — green cuts CO₂, industrial grows
  the economy but emits), and **Weather** (extreme-weather disasters).
- Each round runs a **Development Phase** (earn income, buy citizens/developments)
  then a **Weather Phase** (feed citizens, reveal a number of weather cards that
  scales with global CO₂, resolve disasters × your Risk Factor, advance the Timeline).
- **Win:** reach 20 population before the Timeline hits **2100** (competitive: first/most;
  cooperative: everyone). **Lose (everyone):** global CO₂ reaches the tipping point (30).

## Architecture (strict separation)

- `com.whim.tippingpoint.domain` — game state + card data (no rules, no UI).
- `com.whim.tippingpoint.engine` — all economy/climate simulation, AI, win/loss.
  The heavy CO₂/weather math lives here, **off** the Swing thread.
- `com.whim.tippingpoint.ui` — Swing + `Graphics2D` presentation only; reads
  `domain`, mutates the game exclusively through the `engine.GameEngine` interface.
- `com.whim.tippingpoint.app.Main` — entry point.

## Build & run

Plain JDK (no Maven/Gradle, no third-party libraries; developed against Java 8):

```bash
cd tippingpoint
mkdir -p out
javac --release 8 -d out $(find src -name '*.java')

# Desktop UI (needs a display):
java -cp out com.whim.tippingpoint.app.Main
```
