# Albion (1995) — Java 8 / Swing Clean-Room Recreation

A standalone, zero-dependency **Java 8 + Swing** recreation of the *mechanics* of Blue
Byte's 1995 RPG **Albion**: a 2D top-down overworld, grid-based **first-person**
dungeons, turn-based tactical grid combat, topic-based dialogue and quests, a party
with stats/skills/leveling, inventory/equipment, and a four-school magic system.

This is a **clean-room functional recreation** — no original art, audio, text, or code
from the 1995 game. All graphics are drawn procedurally with `java.awt.Graphics2D`.

## Status

Foundation committed by the orchestrator:
- `com.whim.albion.api` — the frozen contract (enums, DTOs, views, controller + model
  seams). See [`../ALBION_CONTRACT.md`](../ALBION_CONTRACT.md).

Feature packages (`world/entities/items/magic/data`, `engine/combat/dialogue/
persistence`, `ui`, `app`) are built by parallel tasks per the contract.

## Build & run

Requires JDK 8+ and Maven.

```bash
cd albion
mvn -q compile
# once app.Main is wired by integration:
mvn -q exec:java -Dexec.mainClass=com.whim.albion.app.Main   # or:
java -cp target/classes com.whim.albion.app.Main
```

## Architecture

MVC via a single `api` seam. `GameController` is the only bridge between the Swing UI
and the engine; the engine drives a model built by a `ModelFactory`. See the contract
for full package ownership and the seam list.
