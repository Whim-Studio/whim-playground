# Alganon — Single-Player Reimagining (Java 8 / Swing)

A standalone, zero-dependency, single-player desktop RPG that rebuilds the **systems** of
the defunct MMORPG *Alganon* (2009) as original Java code with procedural placeholder art.
See `../DESIGN.md` for the researched design spec (with confidence tags) and
`../ALGANON_CONTRACT.md` for the build architecture.

## Requirements
- JDK 8+ (`maven.compiler.source/target = 1.8`)
- Maven (build) — no runtime dependencies beyond the JDK

## Build & run
```bash
cd alganon
mvn -q compile
mvn -q exec:java -Dexec.mainClass=com.whim.alganon.app.Main   # or run the jar below
```
Or without the exec plugin:
```bash
cd alganon
mvn -q compile
java -cp target/classes com.whim.alganon.app.Main
```

## Highlights
- Guided **race → family → class** character-creation wizard.
- Six classes with distinct resources & mechanics (Champion stances; Magus Flame/Frost/Storm;
  Mystic Words/Touches; Ranger pet/traps/tracking; Reaver; Cabalist).
- Top-down zone exploration, real-time-with-cooldowns combat, static + procedural quests.
- Offline **"Study"** progression: assign a skill to train while the game is closed
  (real-elapsed-time, capped at 8h per session away).
- Gather → process → craft tradeskills with an NPC auction/requisition house.
- Simulated background **faction war** (single-player substitute for Towers/Keeps PvP).

## Single-player substitutions
Multiplayer-only Alganon systems are replaced with single-player analogs (all labeled in
`DESIGN.md`): live PvP → AI duels + simulated faction war; Legion/Battalion/raid tiers →
flavor + small NPC companion party; family/group chat → scripted NPC-driven chat channels;
player auction economy → NPC-populated auction house.
