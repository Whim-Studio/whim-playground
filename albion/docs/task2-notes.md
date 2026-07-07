# Task 2 — Engine & Systems: design notes, formulas, assumptions

Packages owned: `com.whim.albion.engine`, `.combat`, `.dialogue`, `.persistence`
(plus `engine.dev`, an in-package fake used only for the headless smoke test).

Depends on `com.whim.albion.api` **only**. The concrete model is injected as a
`ModelFactory` in `new GameEngine(ModelFactory)`; the engine never imports a Task 1
class. Every public mutation calls `ChangeListener.onStateChanged()`.

## State machine (`GameEngine`)

`GameStateType` transitions driven by the engine:

- `TITLE` → `newGame(seed)` → `OVERWORLD`/`DUNGEON` (derived from the loaded map's
  `MapType`: `OUTDOOR_2D`→OVERWORLD, `INDOOR_3D`→DUNGEON).
- Exploration step onto an `encounterAt` cell → `COMBAT`; onto a `transitionAt`
  cell → `loadMap` + re-derive OVERWORLD/DUNGEON.
- `interact()` on the faced tile → `DIALOGUE` (NPC) or loots an `Interactable`
  (chest) into the active member's pack.
- Combat end → back to the stored exploration state on victory/flee; `GAME_OVER`
  on party wipe.
- Overlays (`INVENTORY`, `CHARACTER_SHEET`, `JOURNAL`, `MENU`) via
  `openState`/`closeOverlay`, which remember and restore the exploration state.

`state()` returns a live `GameStateView` snapshot: `world()` is the model world only
in explore/overlay states (null in COMBAT/DIALOGUE/TITLE/GAME_OVER), `combat()`/
`dialogue()` are non-null only in their states, `party()`/`journal()`/`gold()` proxy
the model, `menuOptions()` supplies TITLE/MENU/GAME_OVER lists.

### Movement conventions
- **OVERWORLD**: `move(dir)` is an absolute grid step; `moveTo(x,y)` takes one greedy
  step toward the target (larger-magnitude axis first, other axis as fallback).
- **DUNGEON** (first-person): `move(NORTH)` = step forward along facing,
  `move(SOUTH)` = step backward, `move(WEST)`/`move(EAST)` = turn left/right in place.

## Combat (`combat.CombatEngine`)

Grid **6 cols × 5 rows**; party placed on the bottom rows, enemies on the top.
Initiative is recomputed **each round** by `StatType.SPEED` (desc; ties: player side
first, then stable index). Enemy turns auto-resolve; the engine pauses on each living
player-side combatant awaiting `combatAction`.

### Formulas
- **hit%** = `clamp(55 + skill/2 + (attDEX − defDEX)*2, 5, 95)`, where `skill` is
  `RANGED` if the attacker is ranged else `MELEE`; if that skill is 0 (typical for
  monsters) it falls back to `attDEX*3`.
- **crit%** = `clamp(CRITICAL/2 + LUCK, 0, 60)`; a crit **doubles** raw damage.
- **raw damage** = `max(1, attackPower + max(0,(STR−10)/2) + rand(0..2))`. The final
  LP loss is produced by `Combatant.takeDamage`, which the api specifies applies
  defense + defending mitigation — so the engine **never subtracts defense itself**
  (avoids double counting). `min 1` damage is therefore enforced inside `takeDamage`
  by the model as well.
- **spell** = `magnitude + MAGIC_TALENT/4`. DAMAGE/DEBUFF route through
  `takeDamage` (DEBUFF at half magnitude); HEAL through `heal`; BUFF sets the
  target's `defending` (a protective ward); UTILITY logs only.
- **flee%** = `clamp(40 + SPEED*2, 5, 95)`. Success ends combat as an escape (not a
  wipe); the triggering encounter cell is cleared so it won't immediately re-fire.

### Actions
`ATTACK` (melee requires manhattan ≤ 1 unless `ranged()`; ranged hits any range),
`CAST` (spends SP, resolves by `TargetType`), `ITEM` (delegates to
`PartyModel.useItem`), `MOVE` (repositions to an empty in-bounds cell; `targetIndex`
is encoded as `y*cols + x`), `DEFEND` (sets `defending` until the combatant's next
turn), `FLEE`. `availableActions()` offers CAST only when the actor has spells and SP.

### Enemy AI (`EnemyBehaviorType`)
- **AGGRESSIVE**: target the lowest-LP living player; strike if in reach (or ranged),
  else step toward and strike if it closes.
- **RANGED**: attack the lowest-LP living player from anywhere.
- **SUPPORT**: heal the most-wounded ally — casting a known HEAL spell if it has one
  and the SP, otherwise an innate "mend" for `SUPPORT_MEND` (6) LP; if no ally needs
  healing it falls back to aggressive behavior.

### Rewards
On victory the engine awards `PartyModel.awardXp(totalXp)` and `addGold(totalGold)`.
**API limitation:** `Combatant` exposes no xp/gold reward, and `MonsterDef` has no
loot list. The engine first tries `content.monster(combatant.id())` and uses that
def's `xpReward`/`goldReward` when found (so Task 1 need only make an enemy's `id()`
equal its monster-def id); otherwise it derives a fallback from combat weight:
`xp = maxLp + attackPower*2`, `gold = maxLp/2`. No item loot is dropped by monsters
(there is no loot source on `Combatant`/`MonsterDef`); chests remain the loot source.

## Dialogue (`dialogue.DialogueRunner`)

Wraps a `Content.DialogueTree`, tracks the current node, and projects a
`DialogueView`. Only options whose `available(ctx)` is true are shown, so the UI's
selection index refers to the **filtered** list (re-filtered on every read/select).
Selecting applies the option's `GameContext` effects, then advances to `next()` (or
ends on null). A node with **no available options is terminal** — any acknowledgement
ends the conversation (supports "…farewell" leaf nodes with a Continue button).

`GameContextImpl` implements the mutation seam: flags/quests via `JournalModel`,
economy/inventory via `PartyModel` (giveItem targets the active member), `startCombat`
and `teleport` via the engine, `notify` sets the status banner.

## Persistence (`persistence.SaveManager`)

Hand-rolled UTF-8 text format (`java.io` only), 5 slots under
`~/.albion/saves/slotN.sav`. The engine owns model access and hands `SaveManager` a
public `Snapshot`; `SaveManager` owns the on-disk format, escaping, and slot labels.

A save records the **seed** plus the deltas that can be re-applied through the api:
map id + player position/facing, gold, per-member LP/SP, flags, and quests
(id/title/status/objectives). On load the engine rebuilds a fresh model from the seed
(`factory.newGame(seed)`) and replays those deltas.

### Assumptions / API limitations (documented, not worked around by editing api)
1. **No model setters.** The api has no way to set a character's LP/level/xp or to
   serialize the model. Therefore:
   - **SP** is restored exactly via `spendSp`/`restoreSp`.
   - **LP** is *best-effort*: we deal the missing amount via `takeDamage` (which the
     model mitigates), so a loaded member may retain a few LP more than saved. It is
     never fatal for a member saved alive. **xp/level and full inventory diffs are not
     restored** beyond what `newGame(seed)` reproduces.
   A clean fix would be either `Combatant.setLp(int)` / a `PartyModel` vitals setter,
   or a Task 1 model (de)serialization hook. Reported to the orchestrator.
2. **Flag keys / quest ids aren't enumerable via the views** (`JournalView` exposes
   quest *titles* but no ids, and no flag list). The engine keeps a small persistence
   *shadow* of every flag/quest it sets through `GameContext`, purely so a save can
   enumerate them. Quests round-trip by id; `title`/objectives are preserved.
3. **`GameContext.startQuest(id)` carries no title**, but `JournalModel.startQuest`
   wants a title + first objective. The engine synthesizes a readable title from the
   id (e.g. `recover_relic` → "Recover Relic") and a generic first objective
   "Quest started."; content can append real objectives via `addObjective`.

## Testing

`engine.dev.FakeModelFactory` is a tiny in-package fake model (a 6×6 town + 5×5 vault,
3 party members across two magic schools, 2 spells / 3 items, rat/guard encounters,
and an elder quest-giver dialogue) used only to drive the engine before Task 1 lands.
`engine.dev.SmokeTest` (a `main`, no JUnit — keeps `mvn compile` self-contained)
exercises the full contract loop and prints PASS/PASS…:

```
mvn -q compile
java -cp target/classes com.whim.albion.engine.dev.SmokeTest
```

Covered: new game → move → trigger combat → win (xp/gold awarded) → walk to NPC →
dialogue (quest started, key granted) → save → mutate → load (gold + quest restored).
All 19 checks pass. When Task 1's `AlbionModelFactory` merges, swap it in for
`FakeModelFactory`; the engine is unchanged.
