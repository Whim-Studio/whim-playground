# Aetherfall — a single-player Action RPG (Java 8 / Swing)

A standalone, dependency-free Action RPG desktop app built in **pure Java 8**. Class-based
combat, original realms and bestiary, an ability/cooldown system, loot with rarity tiers,
gear + reforge economy, buffs/debuffs, a companion pet, and boss encounters — all rendered
with a Swing UI using only `Graphics2D` primitives (no external art or libraries).

## Architecture (strict layering)

| Package          | Responsibility                                         | May depend on           |
|------------------|--------------------------------------------------------|-------------------------|
| `com.arpg.model` | Pure data + domain interfaces (no logic, no Swing)     | JDK only                |
| `com.arpg.engine`| Headless rules engine (combat, loot, progression, …)   | `com.arpg.model` only   |
| `com.arpg.ui`    | Swing presentation                                     | `model` + `GameEngine`  |
| `com.arpg.Main`  | Entry point: wires engine → UI on the EDT              | `engine` + `ui`         |

All cross-layer communication flows through interfaces defined in `com.arpg.model`
(`CombatParticipant`, `GameEventListener`, `GameStateSnapshot`, `PlayerAction`). The UI talks
to the engine exclusively through the `GameEngine` facade; the engine contains **zero** Swing
imports, and the model contains **zero** engine/UI imports.

## Build & run (plain `javac`/`java`, no build tool)

From the repository root:

```sh
# compile (targeting Java 8)
javac -source 8 -target 8 -d arpg/out $(find arpg/src -name '*.java')

# run
java -cp arpg/out com.arpg.Main
```

`com.arpg.Main` accepts an optional numeric RNG seed as its first argument for deterministic
runs, e.g. `java -cp arpg/out com.arpg.Main 12345`.

## Gameplay loop

Create a character (4 original classes) → travel to a realm → each encounter is a combat /
elite / boss / rest / treasure node. Issue a hero action (ability or basic attack), then the
round resolves enemy + pet actions, damage-over-time, cooldowns and regen. Clear encounters to
earn XP, gold and loot; level up to unlock abilities and allocate attribute points; equip and
reforge gear; and press on to the boss. Save/load uses Java object serialization.
