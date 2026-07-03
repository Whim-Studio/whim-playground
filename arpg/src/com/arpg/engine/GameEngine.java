package com.arpg.engine;

import com.arpg.model.Ability;
import com.arpg.model.Character;
import com.arpg.model.CharacterClass;
import com.arpg.model.CombatParticipant;
import com.arpg.model.Enemy;
import com.arpg.model.Equipment;
import com.arpg.model.EquipmentSlot;
import com.arpg.model.GameContent;
import com.arpg.model.GameEventListener;
import com.arpg.model.GameStateSnapshot;
import com.arpg.model.Item;
import com.arpg.model.LootTable;
import com.arpg.model.Pet;
import com.arpg.model.PlayerAction;
import com.arpg.model.Realm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The single public entry point the UI (Task 3) talks to. Owns all mutable game state and the
 * one seedable {@link Random}; delegates rules to the sub-engines and fans events out via {@link EventBus}.
 *
 * Combat cadence: the UI issues {@link #processPlayerAction} for the hero's move, then calls
 * {@link #tick()} to advance the enemy + pet + damage-over-time + cooldown side of the round.
 */
public class GameEngine {
    private static final int OUT_OF_COMBAT_REGEN = 8;
    private static final int IN_COMBAT_RESOURCE_REGEN = 4;
    private static final int AGGRO_PLAYER_PERCENT = 70;

    private final Random rng;
    private final EventBus bus;

    private final CombatEngine combat;
    private final ProgressionEngine progression;
    private final LootEngine loot;
    private final EconomyEngine economy;
    private final RealmEngine realmEngine;

    private Character player;
    private Pet activePet;
    private Realm currentRealm;
    private int encounterIndex;
    private final List<Enemy> enemies = new ArrayList<Enemy>();
    private boolean inCombat;
    private boolean gameOver;

    private final Set<Enemy> rewarded =
            java.util.Collections.newSetFromMap(new IdentityHashMap<Enemy, Boolean>());

    public GameEngine(long seed) {
        this.rng = new Random(seed);
        this.bus = new EventBus();
        this.combat = new CombatEngine(rng, bus);
        this.progression = new ProgressionEngine(bus);
        this.loot = new LootEngine(rng);
        this.economy = new EconomyEngine(rng, bus);
        this.realmEngine = new RealmEngine(rng);
    }

    // ---- public API ---------------------------------------------------------------------------

    public void addEventListener(GameEventListener l) {
        bus.addListener(l);
    }

    public List<CharacterClass> getAvailableClasses() {
        return GameContent.getPlayableClasses();
    }

    public List<Realm> getAvailableRealms() {
        return realmEngine.getRealms();
    }

    public GameStateSnapshot startNewGame(String playerName, CharacterClass clazz) {
        this.player = GameContent.createStartingCharacter(playerName, clazz);
        progression.syncAbilities(player);           // ensure level-appropriate abilities
        this.activePet = GameContent.defaultStartingPet();
        this.currentRealm = null;
        this.encounterIndex = 0;
        this.enemies.clear();
        this.rewarded.clear();
        this.inCombat = false;
        this.gameOver = false;
        bus.log("A new " + clazz.getDisplayName() + " named " + playerName + " begins their journey.");
        GameStateSnapshot snap = buildSnapshot();
        bus.stateChanged(snap);
        return snap;
    }

    public GameStateSnapshot getSnapshot() {
        return buildSnapshot();
    }

    public void processPlayerAction(PlayerAction action) {
        if (action == null || player == null || gameOver) { fireState(); return; }
        switch (action.getType()) {
            case USE_ABILITY:    doUseAbility(action.getAbilityId(), action.getTargetIndex()); break;
            case BASIC_ATTACK:   doBasicAttack(action.getTargetIndex()); break;
            case MOVE_TO_REALM:  doMoveToRealm(action.getRealmId()); break;
            case EQUIP_ITEM:     doEquip(action.getItemId()); break;
            case UNEQUIP_ITEM:   doUnequip(action.getItemId()); break;
            case USE_PET_ABILITY:doPetAbility(action.getAbilityId(), action.getTargetIndex()); break;
            case REFORGE_ITEM:   doReforge(action.getItemId()); break;
            case ADVANCE_ENCOUNTER: doAdvanceEncounter(); break;
            case ALLOCATE_ATTRIBUTE: progression.allocate(player, action.getAttribute()); break;
            default: break;
        }
        fireState();
    }

    /** Advance the enemy/pet/DoT/cooldown side of one combat round (or passive regen out of combat). */
    public void tick() {
        if (player == null || gameOver) { fireState(); return; }

        if (!inCombat) {
            regen(player, OUT_OF_COMBAT_REGEN, OUT_OF_COMBAT_REGEN);
            if (activePet != null && activePet.isAlive()) regen(activePet, OUT_OF_COMBAT_REGEN, OUT_OF_COMBAT_REGEN);
            fireState();
            return;
        }

        // 1. Enemies act.
        List<Enemy> actors = new ArrayList<Enemy>(enemies);
        for (int i = 0; i < actors.size() && !gameOver; i++) {
            Enemy e = actors.get(i);
            if (e.isAlive() && player.isAlive()) enemyAct(e);
        }
        // 2. Pet acts.
        if (!gameOver && activePet != null && activePet.isAlive() && anyEnemyAlive()) petAct(activePet);

        // 3. Periodic buff/debuff effects + expiry for everyone.
        combat.tickBuffs(player);
        if (activePet != null) combat.tickBuffs(activePet);
        for (int i = 0; i < enemies.size(); i++) combat.tickBuffs(enemies.get(i));

        // 4. Cooldowns age, hero regenerates a little resource.
        combat.tickCooldowns();
        player.restoreResource(IN_COMBAT_RESOURCE_REGEN);
        if (activePet != null && activePet.isAlive()) activePet.restoreResource(2);

        // 5. Resolve deaths / rewards / combat end.
        processDeaths();
        checkCombatEnd();
        fireState();
    }

    public boolean saveGame(File f) {
        if (player == null) return false;
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(f));
            SaveData d = new SaveData();
            d.player = player;
            d.realmId = currentRealm == null ? null : currentRealm.getId();
            d.encounterIndex = encounterIndex;
            d.inCombat = inCombat;
            d.gameOver = gameOver;
            d.activePet = activePet;
            d.enemies = new ArrayList<Enemy>(enemies);
            oos.writeObject(d);
            oos.flush();
            bus.log("Game saved.");
            return true;
        } catch (IOException ex) {
            bus.log("Save failed: " + ex.getMessage());
            return false;
        } finally {
            if (oos != null) try { oos.close(); } catch (IOException ignored) { }
        }
    }

    public GameStateSnapshot loadGame(File f) {
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(f));
            SaveData d = (SaveData) ois.readObject();
            this.player = d.player;
            this.currentRealm = d.realmId == null ? null : realmEngine.getRealm(d.realmId);
            this.encounterIndex = d.encounterIndex;
            this.inCombat = d.inCombat;
            this.gameOver = d.gameOver;
            this.activePet = d.activePet;
            this.enemies.clear();
            if (d.enemies != null) this.enemies.addAll(d.enemies);
            this.rewarded.clear();
            combat.resetCooldowns();
            bus.log("Game loaded.");
        } catch (IOException ex) {
            bus.log("Load failed: " + ex.getMessage());
        } catch (ClassNotFoundException ex) {
            bus.log("Load failed: corrupt save (" + ex.getMessage() + ")");
        } finally {
            if (ois != null) try { ois.close(); } catch (IOException ignored) { }
        }
        GameStateSnapshot snap = buildSnapshot();
        bus.stateChanged(snap);
        return snap;
    }

    // ---- action handlers ----------------------------------------------------------------------

    private void doUseAbility(String abilityId, int targetIndex) {
        Ability a = findAbility(player.getAbilities(), abilityId);
        if (a == null) { bus.log("Unknown ability: " + abilityId); return; }
        boolean summon = combat.resolveAbility(player, a, opposingForPlayer(), playerSideLiving(), targetIndex);
        if (summon) summonPet(a.getSummonPetId());
        postPlayerCombat();
    }

    private void doBasicAttack(int targetIndex) {
        if (!inCombat || !anyEnemyAlive()) { bus.log("There is nothing to attack."); return; }
        combat.basicAttack(player, opposingForPlayer(), targetIndex);
        postPlayerCombat();
    }

    private void doPetAbility(String abilityId, int targetIndex) {
        if (activePet == null || !activePet.isAlive()) { bus.log("No active pet."); return; }
        Ability a = findAbility(activePet.getAbilities(), abilityId);
        if (a == null) { bus.log("Pet lacks ability: " + abilityId); return; }
        combat.resolveAbility(activePet, a, opposingForPlayer(), playerSideLiving(), targetIndex);
        postPlayerCombat();
    }

    private void postPlayerCombat() {
        processDeaths();
        checkCombatEnd();
    }

    private void doMoveToRealm(String realmId) {
        if (inCombat && anyEnemyAlive()) { bus.log("You cannot travel while enemies still stand."); return; }
        Realm realm = realmEngine.getRealm(realmId);
        if (realm == null) { bus.log("No such realm: " + realmId); return; }
        this.currentRealm = realm;
        this.encounterIndex = 0;
        bus.log("You travel to " + realm.getName() + " (tier " + realm.getDifficultyTier() + ").");
        startEncounter();
    }

    private void doAdvanceEncounter() {
        if (currentRealm == null) { bus.log("You are not in a realm."); return; }
        if (inCombat && anyEnemyAlive()) { bus.log("Defeat the current enemies first."); return; }
        int next = encounterIndex + 1;
        if (realmEngine.hasEncounter(currentRealm, next)) {
            encounterIndex = next;
            startEncounter();
        } else {
            inCombat = false;
            enemies.clear();
            bus.log("You have conquered " + currentRealm.getName() + "!");
        }
    }

    private void startEncounter() {
        enemies.clear();
        rewarded.clear();
        combat.resetCooldowns();
        List<Enemy> spawned = realmEngine.spawnEncounter(currentRealm, encounterIndex);
        enemies.addAll(spawned);
        inCombat = !spawned.isEmpty();
        Realm.EncounterDef enc = realmEngine.encounterAt(currentRealm, encounterIndex);
        if (enc != null) bus.log(enc.getName() + " — " + enc.getDescription());
        if (realmEngine.isBossEncounter(currentRealm, encounterIndex)) {
            bus.log("BOSS ENCOUNTER — steel yourself!");
        }
        if (!inCombat) bus.log("The area is quiet. ADVANCE_ENCOUNTER to press onward.");
    }

    private void doEquip(String itemId) {
        Item item = player.getInventory().findById(itemId);
        if (item == null || !item.isEquipment()) { bus.log("No equippable item '" + itemId + "' in inventory."); return; }
        Equipment eq = (Equipment) item;
        if (eq.getLevelRequirement() > player.getLevel()) {
            bus.log(eq.getName() + " requires level " + eq.getLevelRequirement() + ".");
            return;
        }
        player.getInventory().remove(item);
        Equipment previous = player.equip(eq);
        if (previous != null) player.getInventory().add(previous);
        bus.log("Equipped " + eq.getName() + " (" + eq.getSlot() + ").");
    }

    private void doUnequip(String itemId) {
        for (Map.Entry<EquipmentSlot, Equipment> e : player.getEquipped().entrySet()) {
            if (e.getValue() != null && e.getValue().getId().equals(itemId)) {
                Equipment removed = player.unequip(e.getKey());
                if (removed != null) {
                    player.getInventory().add(removed);
                    bus.log("Unequipped " + removed.getName() + ".");
                }
                return;
            }
        }
        bus.log("No equipped item '" + itemId + "'.");
    }

    private void doReforge(String itemId) {
        Item target = player.getInventory().findById(itemId);
        EquipmentSlot equippedSlot = null;
        if (target == null) {
            for (Map.Entry<EquipmentSlot, Equipment> e : player.getEquipped().entrySet()) {
                if (e.getValue() != null && e.getValue().getId().equals(itemId)) {
                    target = e.getValue(); equippedSlot = e.getKey(); break;
                }
            }
        }
        if (target == null) { bus.log("No item '" + itemId + "' to reforge."); return; }

        ReforgeResult result = economy.reforge(player, target);
        if (!result.isSuccess()) { bus.log(result.getMessage()); return; }

        // The reforge mutates the equipment in place; only a shatter removes it.
        if (result.getOutcome() == ReforgeResult.Outcome.SHATTERED || result.getResultItem() == null) {
            if (equippedSlot != null) player.unequip(equippedSlot);
            else player.getInventory().remove(target);
        } else if (equippedSlot != null) {
            // Re-run derived-stat recompute now that the equipped item changed.
            player.recalculateDerivedStats();
        }
    }

    // ---- combat helpers -----------------------------------------------------------------------

    private void enemyAct(Enemy e) {
        List<CombatParticipant> playerSide = playerSideLiving();
        if (playerSide.isEmpty()) return;
        Ability a = combat.chooseAbility(e);
        int tgt = chooseAggroIndex(playerSide);
        if (a != null) {
            combat.resolveAbility(e, a, playerSide, livingEnemiesExcept(e), tgt);
        } else {
            combat.basicAttack(e, playerSide, tgt);
        }
    }

    private void petAct(Pet pet) {
        List<CombatParticipant> opposing = opposingForPlayer();
        Ability a = combat.chooseAbility(pet);
        if (a != null) {
            combat.resolveAbility(pet, a, opposing, playerSideLiving(), firstLivingEnemyIndex());
        } else {
            combat.basicAttack(pet, opposing, firstLivingEnemyIndex());
        }
    }

    private void summonPet(String petId) {
        Pet pet = petId == null ? GameContent.defaultStartingPet() : GameContent.spawnPet(petId);
        if (pet == null) return;
        this.activePet = pet;
        bus.log(activePet.getName() + " joins the fight!");
    }

    private int chooseAggroIndex(List<CombatParticipant> playerSide) {
        if (playerSide.size() <= 1) return 0;
        return rng.nextInt(100) < AGGRO_PLAYER_PERCENT ? 0 : 1;
    }

    private void processDeaths() {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (!e.isAlive() && !rewarded.contains(e)) {
                rewarded.add(e);
                progression.grantXp(player, e.getExperienceReward());
                LootTable lt = e.getLootTable();
                int gold = loot.rollGold(lt);
                if (gold > 0) {
                    player.addGold(gold);
                    bus.log("Found " + gold + " gold.");
                }
                Item drop = loot.rollLoot(lt, player.getLevel());
                if (drop != null) {
                    player.getInventory().add(drop);
                    bus.lootDropped(drop);
                    bus.log("Looted " + drop.getName() + " (" + drop.getRarity() + ").");
                }
            }
        }
    }

    private void checkCombatEnd() {
        if (player != null && !player.isAlive()) {
            gameOver = true;
            inCombat = false;
            bus.log(player.getName() + " has fallen. Game over.");
            return;
        }
        if (inCombat && !anyEnemyAlive()) {
            inCombat = false;
            int reward = 5 * Math.max(1, currentRealm == null ? 1 : currentRealm.getDifficultyTier());
            economy.addCurrency(player, reward);
            bus.log("Encounter cleared! +" + reward + " gold.");
            if (realmEngine.hasEncounter(currentRealm, encounterIndex + 1)) {
                bus.log("ADVANCE_ENCOUNTER to press onward.");
            } else if (currentRealm != null) {
                bus.log(currentRealm.getName() + " is cleared — the realm is yours!");
            }
        }
    }

    private void regen(CombatParticipant p, int hp, int res) {
        p.applyHealing(hp);
        p.restoreResource(res);
    }

    private List<CombatParticipant> opposingForPlayer() {
        List<CombatParticipant> out = new ArrayList<CombatParticipant>();
        for (int i = 0; i < enemies.size(); i++) out.add(enemies.get(i));
        return out;
    }

    private List<CombatParticipant> playerSideLiving() {
        List<CombatParticipant> out = new ArrayList<CombatParticipant>();
        if (player != null && player.isAlive()) out.add(player);
        if (activePet != null && activePet.isAlive()) out.add(activePet);
        return out;
    }

    private List<CombatParticipant> livingEnemiesExcept(Enemy self) {
        List<CombatParticipant> out = new ArrayList<CombatParticipant>();
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e != self && e.isAlive()) out.add(e);
        }
        return out;
    }

    private int firstLivingEnemyIndex() {
        for (int i = 0; i < enemies.size(); i++) if (enemies.get(i).isAlive()) return i;
        return 0;
    }

    private boolean anyEnemyAlive() {
        for (int i = 0; i < enemies.size(); i++) if (enemies.get(i).isAlive()) return true;
        return false;
    }

    private static Ability findAbility(List<Ability> list, String id) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).getId().equals(id)) return list.get(i);
        return null;
    }

    private GameStateSnapshot buildSnapshot() {
        return new EngineSnapshot(player, currentRealm, enemies, bus.recentLog(), inCombat, activePet);
    }

    private void fireState() {
        bus.stateChanged(buildSnapshot());
    }

    /** Serializable snapshot of everything needed to resume a run. Realm is stored by id. */
    private static final class SaveData implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        Character player;
        String realmId;
        int encounterIndex;
        boolean inCombat;
        boolean gameOver;
        Pet activePet;
        ArrayList<Enemy> enemies;
    }
}
