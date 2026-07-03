# ARPG — Game Systems / Engine (`com.arpg.engine`)

Fully-implemented headless rules engine for the single-player ARPG. Java 8, zero external
libraries, **no Swing/AWT anywhere**. This package imports only `java.util`, `java.io`,
`java.util.concurrent` and `com.arpg.model` (the shared contract owned by Task 1).

> The local `com.arpg.model` stubs used to compile-check this package have been **deleted** as
> required; the pushed engine imports the real model. This file records the EXACT model API the
> engine consumes so consolidation is mechanical. Verified locally against faithful stubs with a
> headless smoke test (`test/com/arpg/engine/EngineSmokeTest.java`) — all 13 checks pass, a fresh
> Warrior clears two realms incl. bosses, gains levels, loots, and save/load round-trips. Compiled
> with `javac -source 8 -target 8`.

## Public facade — `GameEngine`
```
public GameEngine(long seed)
public void addEventListener(GameEventListener l)
public java.util.List getAvailableClasses()
public java.util.List getAvailableRealms()
public GameStateSnapshot startNewGame(String playerName, CharacterClass clazz)
public GameStateSnapshot getSnapshot()
public void processPlayerAction(PlayerAction action)
public void tick()
public boolean saveGame(java.io.File f)
public GameStateSnapshot loadGame(java.io.File f)
```
Combat cadence: UI calls `processPlayerAction(...)` for the hero's move, then `tick()` to advance
the enemy + pet + damage-over-time + cooldown side of the round.

## GameEventListener callbacks — where fired
- `onDamageDealt` — CombatEngine.deal / dealDamage / basicAttack / periodic debuff tick
- `onHealed` — CombatEngine.heal and periodic-heal in tickBuffs
- `onBuffApplied` — CombatEngine.applyBuff
- `onBuffExpired` — CombatEngine.tickBuffs when a buff's remaining ticks hit 0
- `onLevelUp` — ProgressionEngine.levelUp
- `onLootDropped` — GameEngine.processDeaths after LootEngine roll
- `onParticipantDeath` — CombatEngine.deal / tickBuffs on lethal blow
- `onCombatLog` — EventBus.log (every rolling-log line)
- `onGameStateChanged` — GameEngine after every action and every tick (and start/load)

## Model API consumed (Task 1 contract — coordinate on these)
- **CombatParticipant**: getName, getCurrentHealth/getMaxHealth, getCurrentResource/getMaxResource,
  isAlive, applyDamage, applyHealing, spendResource, restoreResource, getAttackPower, getDefense,
  getAbilities, getActiveBuffs, addBuff, removeBuff.
- **Ability**: getId, getName, getResourceCost, getCooldown, getEffectType, getMagnitude,
  getTargetType; enums `EffectType{DAMAGE,HEAL,BUFF,DEBUFF,SUMMON}`, `TargetType{SELF,SINGLE_ENEMY,AOE_ENEMIES,ALLY}`.
- **BuffDebuff** (engine CONSTRUCTS these): `new BuffDebuff(String name, boolean debuff, int attackMod,
  int defenseMod, int periodicDamage, int periodicHeal, int durationTicks)`, plus getAttackModifier,
  getDefenseModifier, getPeriodicDamage, getPeriodicHeal, getName, isDebuff, decrementRemaining,
  isExpired, copy. *(Main coordination point — see deviations.)*
- **Character**: getCharacterClass, getLevel/setLevel, getXp/setXp, getUnspentAttributePoints/
  setUnspentAttributePoints/addUnspentAttributePoints, getCurrency/setCurrency, getAttribute/setAttribute,
  getMaxHealth/setMaxHealth, setCurrentHealth, getMaxResource/setMaxResource, setCurrentResource,
  getEquipped()->Map<EquipmentSlot,Item>, getInventory()->List<Item>.
- **CharacterClass**: getDisplayName, getBaseHealth/Resource/Attack/Defense, getAbilityIds. (>=4 values)
- **Item**: getId, getName, getSlot, isEquipment, getRarity, getAttackModifier, getDefenseModifier,
  getVitalityModifier, getLevelRequirement, getSellValue, withScaledStats(double,int).
- **Rarity**: getWeight, getStatMultiplier (enum).
- **EquipmentSlot**, **AttributeType** (enums).
- **LootTable**: getDropChancePercent, getEntries; `LootTable.Entry`: getItem, getWeight.
- **Enemy**: getId, isBoss, getXpReward, getLootTableId, spawnCopy, getAbilities.
- **Pet**: getId, spawnCopy, getAbilities, getCurrentHealth.
- **Realm**: getId, getName, getDifficultyTier, getEncounters, getEncounterCount;
  `Realm.Encounter`: getDescription, getEnemyTemplateIds, isBoss, getCurrencyReward.
- **PlayerAction**: getType + getAbilityId/getItemId/getRealmId/getTargetIndex/getAttribute.
- **GameContent**: getClasses, getAbility(id), getItem(id), getLootTable(id), getEnemyTemplate(id),
  getPetTemplate(id), getSummonedPet(), getRealms(), getRealm(id).
- **GameStateSnapshot**: interface only — the engine ships its own read-only `EngineSnapshot`.

## Contract deviations / assumptions to reconcile at consolidation
1. **BuffDebuff is engine-authored from abilities.** The contract gives Ability only a single
   `magnitude`; the engine maps `BUFF -> +mag defense, +mag/2 attack` and `DEBUFF -> -mag/2 atk/def
   + mag/2 periodic damage`, both for a fixed 3-tick duration. Requires the constructor + `copy()` /
   `decrementRemaining()` shown above. If Task 1's BuffDebuff differs, adjust `CombatEngine.buffTemplate/
   debuffTemplate` and `tickBuffs`.
2. **Character mutators.** Progression/save-load need setters not in the CombatParticipant contract
   (setLevel/setXp/setMaxHealth/setCurrentHealth/setMaxResource/setCurrentResource/setUnspentAttributePoints/
   setCurrency/setAttribute). If the real Character is immutable or uses different names, adapt
   ProgressionEngine + GameEngine.readSave.
3. **spawnCopy() on Enemy/Pet, withScaledStats() on Item, getSummonedPet() on GameContent** are
   engine conveniences; if absent, the engine can construct fresh instances via the model constructors.
4. Effective attack/defense fold active-buff modifiers **in the engine** (via getActiveBuffs), so the
   model's getAttackPower/getDefense need NOT pre-apply buffs.
