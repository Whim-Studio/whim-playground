# CHECKLIST — researched rule → code → test

Maps each documented rule (see `RESEARCH.md`) to its implementation and, where it
is pure logic, its automated test. ✅ implemented faithfully · 🔧 adapted/adopted
ruling (documented in RESEARCH.md).

## Fighting Fantasy core

| Rule | Code | Test | Status |
|------|------|------|--------|
| SKILL = 1d6+6 (7–12) | `engine/Dice.rollSkill` | `DiceTest.rollSkillRange` | ✅ |
| STAMINA = 2d6+12 (14–24) | `engine/Dice.rollStamina` | `DiceTest.rollStaminaRange` | ✅ |
| LUCK = 1d6+6 (7–12) | `engine/Dice.rollLuck` | `DiceTest.rollLuckRange` | ✅ |
| Seedable / reproducible dice | `engine/Dice(long)` | `DiceTest.seededReproducible` | ✅ |
| nDm roller, 2d6, d6 bounds | `engine/Dice.roll/roll2d6/d6` | `DiceTest.roll2d6Range`, `rollSingleWithinBounds`, `d6Range` | ✅ |
| Attack Strength = 2d6 + SKILL | `engine/Combat.resolveRound` | `CombatTest.playerWins/monsterWins` | ✅ |
| Higher AS wounds for 2 STAMINA | `engine/Combat.resolveRound` + `BASE_DAMAGE` | `CombatTest.playerWinsDealsBaseDamage` | ✅ |
| Tie = no damage | `engine/Combat.resolveRound` | `CombatTest.tieDealsNoDamage` | ✅ |
| Test Luck: 2d6 ≤ LUCK = Lucky | `engine/LuckTest.test` | `LuckTestTest.luckyWhenRollAtMostLuck`, `unluckyWhenRollExceedsLuck` | ✅ |
| Each Luck test spends 1 LUCK | `model/Character.spendLuck` | `LuckTestTest.luckSpentEachTest`, `luckNeverBelowZero` | ✅ |
| Luck when wounding: +2 / −1(graze) | `engine/Combat.applyLuckToAttack` | `CombatTest.luckToAttackLucky/Unlucky` | ✅ |
| Luck when wounded: −1 loss / +1 loss | `engine/Combat.applyLuckToDefense` | `CombatTest.luckToDefenseLucky/Unlucky` | ✅ |
| STAMINA 0 = death | `model/Character.loseStamina` | (covered via smoke + combat) | ✅ |
| Current never exceeds Initial | `model/Character.gain*` clamps | — | ✅ |
| Provisions restore STAMINA | `model/Character.eatProvision` (+4) | smoke | 🔧 (+4 adopted) |

## 1986 boardgame structure

| Rule | Code | Status |
|------|------|--------|
| Dungeon = room graph, entrance → lair | `model/Board`, `model/Content.buildDungeon` (16 rooms) | 🔧 (layout adopted) |
| Room types (monster/treasure/trap/event/special/lair) | `model/RoomType`, `engine/GameEngine.resolveRoom` | ✅ role / 🔧 contents |
| Dice movement (1d6 steps) | `engine/GameEngine.rollMovement/reachableRooms/moveTo` | 🔧 (dice movement adopted) |
| Sequence of play: move → resolve → end turn | `engine/GameEngine` + `ui/GameFrame` turn loop | ✅ |
| Three card decks, shuffle/draw/discard/reshuffle | `engine/Deck`, `model/Content.build*Deck` | ✅ mechanics / 🔧 card set |
| Card effects resolved | `engine/GameEngine.resolveCard`, `model/CardEffect` | ✅ |
| Win: defeat Zagor in the lair | `engine/GameEngine.onMonsterDefeated` | ✅ |
| Lose: whole party wiped | `engine/GameEngine.checkDefeat`, `GameState.anyAlive` | ✅ |
| Zagor stats | `model/Content.zagor` (SKILL 12 / STAMINA 18) | 🔧 (adopted) |
| 1–4 hot-seat players | `engine/GameEngine.endTurn` (skips dead), `ui/SetupPanel` | ✅ |

## App / UX requirements

| Requirement | Code | Status |
|-------------|------|--------|
| Swing board with rooms + tokens (programmatic art) | `ui/BoardPanel` | ✅ |
| Per-player status meters + inventory | `ui/StatusPanel` | ✅ |
| Combat dialog: both AS, log, Test Luck at right moment | `ui/CombatDialog` | ✅ |
| Card-draw dialog with resolved effect | `ui/CardDialog` | ✅ |
| Event log | `ui/GameFrame` log area + `GameState.log` | ✅ |
| Inventory / item-use | `ui/GameFrame.doUseItem`, provision button | ✅ |
| Menu New/Save/Load/Rules/Exit — all working | `ui/GameFrame.buildMenuBar` | ✅ |
| Save/Load to local file | `persistence/SaveGame` (Java serialization) | ✅ (round-trip in smoke) |
| How to Play in own words + disclaimer | `ui/HowToPlayDialog`, `ui/SetupPanel` | ✅ |
| Win/lose end screen | `ui/EndScreen` | ✅ |
| Keyboard + mouse accessibility | mnemonics (Alt+R/E/P/U/A/L/C), Space, room buttons, clicks | ✅ |
| Edge cases (0 STAMINA mid-combat, empty deck reshuffle, invalid move) | clamps in `Character`/`Monster`, `Deck.draw` reshuffle, `moveTo` guard | ✅ |
| Java 8 only, offline, no GW IP | `--release 8` clean, no network, original content | ✅ |
