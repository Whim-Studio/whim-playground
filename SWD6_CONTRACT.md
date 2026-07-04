# Star Wars D6 Digital Tabletop — Build Contract

A standalone, **Java 8**, zero-dependency Swing recreation of the West End Games
**Star Wars D6** tabletop RPG, defaulting to the **Revised & Expanded (2nd ed.)**
rules. Three parallel tasks build against the shared `api` package (already
committed). This file is the single source of truth for the seams between them.

**Legal:** original mechanics implementation and original flavor text only. No
copyrighted WEG rulebook text, no official character-sheet scans, no
Lucasfilm/Disney artwork or logos. All visuals are drawn with `Graphics2D` or are
simple Swing components.

## Hard constraints (ALL tasks)

- **Java 8 ONLY.** No `var`, no switch expressions, no text blocks, no records, no
  `List.of`/`Map.of`, no `Stream.toList()`. Standard `switch` statements and Java 8
  streams/lambdas are fine. `maven.compiler.source/target = 1.8`.
- **Zero external libraries.** Only `javax.swing`, `java.awt`, `java.awt.print`,
  `java.util`, `java.io`, `java.text`, `java.lang`. No JSON/XML library — Task 1
  hand-rolls JSON. No Maven plugins beyond the jar plugin already in `pom.xml`.
- **Package root:** `com.whim.swd6` — **Source root:** `swd6/src/`
- Every task compiles and imports ONLY from `com.whim.swd6.api` (and its own
  sub-package). **No task edits another task's sub-package or the `api` package.**
- All randomness goes through `java.util.Random` (seedable for tests).
- When done: **push your branch and `send_prompt` a short report to the
  orchestrator task.** Do NOT open a PR into main yourself. Do NOT merge.

## Package ownership

| Package | Owner | Contents |
|---|---|---|
| `com.whim.swd6.api` | **Orchestrator (DONE — do not edit)** | Value types, enums, model POJOs, result types, service interfaces (below) |
| `com.whim.swd6.rules` + `com.whim.swd6.persistence` | **Task 1** | `GameContent` (implements `api.ContentProvider`): full skill catalog, 6 original templates, weapon/armor/equipment catalogs, the original test adventure. `JsonCharacterRepository` (implements `api.CharacterRepository`) with a tiny hand-rolled JSON reader/writer. `RulesSelfCheck` main. |
| `com.whim.swd6.engine` | **Task 2** | `D6Engine` (implements `api.RpgEngine`), `Dice` (seedable), `Encounter` (implements `api.CombatTracker`), `EngineSelfCheck` main. |
| `com.whim.swd6.ui` | **Task 3** | `MainFrame`, creation wizard panels, `CharacterSheetPanel` (drawn + printable), `DiceRollerPanel`, `CombatTrackerPanel`, `AdventurePanel`, `Palette`, `StubContent`/`StubEngine` (dev-only fakes so the UI runs standalone), `UiPreview` main. |
| `com.whim.swd6.app` | **Orchestrator (final)** | `Main` — wires `GameContent` + `D6Engine` + `JsonCharacterRepository` into `MainFrame`. |

## The `api` package (already committed — READ IT, DO NOT MODIFY)

Concrete value/model types (construct freely):
- **`DiceCode`** — immutable `ND+P`. `parse("3D+2")`, `of(dice,pips)`, `ofPips(n)`,
  `pipValue()` (=dice*3+pips), `addDice/addPips/add/subtract/doubled`, `toString`.
  **3 pips = 1 die**; all math normalizes.
- **Enums:** `Attribute` (DEX,KNO,MEC,PER,STR,TEC), `ForceSkill` (CONTROL,SENSE,
  ALTER), `WoundLevel` (HEALTHY→KILLED, with `penaltyDice()`,
  `fromDamageMargin(int)`, `escalate(WoundLevel)`), `DifficultyTier`
  (VERY_EASY…HEROIC, `fromTarget(int)`, `representativeTarget()`).
- **Model POJOs (mutable):** `SkillDef`, `Skill`, `Specialization`, `Weapon`,
  `Armor`, `Equipment`, `Template`, `PlayerCharacter` (the central object — named
  PlayerCharacter, NOT Character; `skillCode(Skill)` = attribute + added),
  `Combatant`.
- **Result types (immutable):** `RollResult` (per-die detail + total +
  `isComplication()` + `isWildExploded()` + `isSuccess()`), `DamageResult`.
- **`Scenario`** — branching adventure graph: `Scenario` → `Scene` (type NARRATIVE/
  SKILL_CHECK/COMBAT/DECISION/ENDING) → `Choice`. `sceneById(id)`.
- **`CreationRules`** — shared constants + pure validators: 18D attributes
  (2D–4D each), 7D skill dice, +2D per-skill creation cap, starting 1 Force Point /
  5 Character Points, `attributePipsRemaining`, `skillPipsRemaining`, etc.

Service interfaces (implement exactly these):
- **`ContentProvider`** (Task 1) — `skillCatalog()`, `templates()`, `weapons()`,
  `armorCatalog()`, `equipmentCatalog()`, `scenario()`, `instantiate(Template)`.
- **`CharacterRepository`** (Task 1) — `save(pc,file)`, `load(file)`,
  `defaultDirectory()`, `listSaved()`.
- **`RpgEngine`** (Task 2) — `roll(code,useWildDie,target)`,
  `roll(code,useWildDie,tier)`, `opposedRoll(a,b)`, `resolveDamage(dmg,resist)`,
  `multiActionPenalty(actions)`, `effectiveAttackCode(Combatant)`.
- **`CombatTracker`** (Task 2) — `add`, `rollInitiative`, `order`, `current`,
  `next`, `round`, `applyHit(target,result)`, `isOver`.

## Rules the engine MUST implement (R&E; conflicts flagged, default to R&E)

1. **Wild Die** — exactly one die of every skill/attribute roll is the Wild Die.
   On a **6** it explodes: reroll, add, repeat while it keeps rolling 6
   (`isWildExploded`). On a **1** flag a **complication** (`isComplication`); the
   engine's default handling is to subtract the 1 **and** the single highest other
   die from the total. A 0D code still rolls the Wild Die alone.
2. **Difficulty** — success = total ≥ target. Tiers per `DifficultyTier`.
3. **Damage** — `resolveDamage` rolls damage and resistance, margin =
   damageTotal − resistTotal, wound via `WoundLevel.fromDamageMargin`. The tracker's
   `applyHit` escalates against the target's existing wound via `escalate`.
4. **Multiple actions** — declaring N actions subtracts (N−1)D from every action
   that round. *(1st ed. differs — flagged; default R&E.)*
5. **Wound penalties** — Stunned/Wounded −1D, Wounded Twice −2D, applied via
   `effectiveAttackCode`. Incapacitated+ cannot act.
6. **Points** — Character Points add +1D each when spent on a roll (support a
   `bonusDice` path in the roller UI); Force Points double the whole code
   (`DiceCode.doubled`) for a round. Track counts on `PlayerCharacter`.

## Coordination rules

- If you believe the `api` contract is wrong or missing something, **do not edit
  api** — `send_prompt` the orchestrator describing the gap and proceed against the
  current contract where possible.
- Each task provides a `*SelfCheck`/preview `main` so it can be run in isolation.
  Task 3 must run standalone using its own `StubContent`/`StubEngine` fakes, so UI
  work does not block on Tasks 1–2.
- Compile before reporting: `mvn -q -o compile` from `swd6/` (or `javac` targeting
  1.8). Report compile status in your send_prompt.
