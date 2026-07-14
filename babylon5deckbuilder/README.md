# Babylon 5: The Shadow War — Deck-Building Game

A Java 8 / Swing desktop deck-building game adapting the **Babylon 5 Collectible
Card Game**, built from `babylon5/docs/B5_DECKBUILDER_GDD.md`. Local hotseat +
AI for 2–5 players, a modular engine with a **JSON card-effect DSL**, three AI
difficulty tiers, a **Monte-Carlo balance-simulation harness**, JSON save/load,
and CSV export for Tabletop Simulator. **No runtime dependencies beyond the JDK.**

> Status: this module is a complete, self-contained core (engine, DSL, AI,
> simulator, tests, and a functional hotseat UI). See
> [`docs/DEVELOPER_SUMMARY.md`](docs/DEVELOPER_SUMMARY.md) for what is fully
> implemented versus scoped as extension points, and the design tradeoffs.

## Build

Requires **JDK 8** and **Maven 3.6+**.

```bash
cd babylon5deckbuilder
mvn -q package          # compiles, runs unit + integration tests, builds the JAR
# or use the helper:
./build.sh
```

The runnable JAR is written to `target/babylon5-deckbuilder.jar`.

## Run the game (Swing UI)

```bash
java -jar target/babylon5-deckbuilder.jar
```

Main Menu → **New Game** → choose 2–5 seats (faction + Human/AI per seat) →
**Start Game**. On your turn the engine auto-draws and plays your hand and
accumulates **Influence**; pick cards from the market to **Buy**, then **End
Turn**. AI seats play automatically. First to the **Prestige** target (default
40) wins. Keyboard: `Alt+N` New, `Alt+S` Start, `Alt+U` Buy, `Alt+E` End Turn.

## Run the headless balance simulation

```bash
# 1,000 games, 4 players, HARD AI, deterministic seed -> reports/balance.{csv,json}
java -jar target/babylon5-deckbuilder.jar --sim --games 1000 --players 4 --seed 42 --ai hard
```

Flags: `--games N`, `--players 2..5`, `--seed S`, `--target P` (prestige goal),
`--ai random|easy|normal|hard|mc`, `--cards <dir>` (external card dir).
Output is written to `reports/balance.csv` and `reports/balance.json` and a
per-faction win-rate summary is printed to the console.

## Export cards for Tabletop Simulator

```bash
java -jar target/babylon5-deckbuilder.jar --export-tts assets/tts_cards.csv
```

## Adding cards without recompiling

Drop a `*.json` file into `assets/cards/` and restart. Each file is a single
card object or an array of them, following the DSL:

```json
{
  "id": "g_quan",
  "name": "G'Quan Eth",
  "faction": "NARN_REGIME",
  "type": "CHARACTER",
  "cost": 2,
  "prestige": 0,
  "attributes": { "DIPLOMACY": 3, "INTRIGUE": 2, "MILITARY": 2 },
  "effects": [ { "type": "GAIN_INFLUENCE", "amount": 1 } ],
  "text": "Narn religious relic-bearer."
}
```

Bundled cards live in `src/main/resources/cards/` (listed in `index.txt`).
See [`docs/DEVELOPER_GUIDE.md`](docs/DEVELOPER_GUIDE.md) for the full DSL grammar,
architecture, and extension points.

## License

MIT — see [`LICENSE`](LICENSE).
