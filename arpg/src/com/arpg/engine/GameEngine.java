package com.arpg.engine;

import com.arpg.model.Ability;
import com.arpg.model.AttributeType;
import com.arpg.model.Character;
import com.arpg.model.CharacterClass;
import com.arpg.model.CombatParticipant;
import com.arpg.model.Enemy;
import com.arpg.model.EquipmentSlot;
import com.arpg.model.GameContent;
import com.arpg.model.GameEventListener;
import com.arpg.model.GameStateSnapshot;
import com.arpg.model.Item;
import com.arpg.model.Pet;
import com.arpg.model.PlayerAction;
import com.arpg.model.Rarity;
import com.arpg.model.Realm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
    private final GameContent content;
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
        this.content = new GameContent();
        this.bus = new EventBus();
        this.combat = new CombatEngine(rng, bus);
        this.progression = new ProgressionEngine(bus, content);
        this.loot = new LootEngine(rng);
        this.economy = new EconomyEngine(rng, bus);
        this.realmEngine = new RealmEngine(content);
    }

    // ---- public API ---------------------------------------------------------------------------

    public void addEventListener(GameEventListener l) {
        bus.addListener(l);
    }

    @SuppressWarnings("rawtypes")
    public java.util.List getAvailableClasses() {
        return content.getClasses();
    }

    @SuppressWarnings("rawtypes")
    public java.util.List getAvailableRealms() {
        return realmEngine.getRealms();
    }

    public GameStateSnapshot startNewGame(String playerName, CharacterClass clazz) {
        this.player = new Character(playerName, clazz);
        progression.syncAbilities(player);        // grant level-1 abilities
        economy.addCurrency(player, 50);
        player.getInventory().add(content.getItem("rusty_sword"));
        this.activePet = null;
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
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new FileWriter(f));
            writeSave(w);
            w.flush();
            bus.log("Game saved.");
            return true;
        } catch (IOException ex) {
            bus.log("Save failed: " + ex.getMessage());
            return false;
        } finally {
            if (w != null) try { w.close(); } catch (IOException ignored) { }
        }
    }

    public GameStateSnapshot loadGame(File f) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(f));
            readSave(r);
            bus.log("Game loaded.");
        } catch (IOException ex) {
            bus.log("Load failed: " + ex.getMessage());
        } catch (RuntimeException ex) {
            bus.log("Load failed: corrupt save (" + ex.getMessage() + ")");
        } finally {
            if (r != null) try { r.close(); } catch (IOException ignored) { }
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
        if (summon) summonPet();
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
        Realm.Encounter enc = realmEngine.encounterAt(currentRealm, encounterIndex);
        if (enc != null) bus.log(enc.getDescription());
        if (realmEngine.isBossEncounter(currentRealm, encounterIndex)) {
            bus.log("BOSS ENCOUNTER — steel yourself!");
        }
        if (!inCombat) bus.log("The area is quiet.");
    }

    private void doEquip(String itemId) {
        Item item = removeFromInventory(itemId, true);
        if (item == null) { bus.log("No equippable item '" + itemId + "' in inventory."); return; }
        if (item.getLevelRequirement() > player.getLevel()) {
            player.getInventory().add(item);
            bus.log(item.getName() + " requires level " + item.getLevelRequirement() + ".");
            return;
        }
        EquipmentSlot slot = item.getSlot();
        Item previous = player.getEquipped().put(slot, item);
        if (previous != null) player.getInventory().add(previous);
        bus.log("Equipped " + item.getName() + " (" + slot + ").");
    }

    private void doUnequip(String itemId) {
        for (Map.Entry<EquipmentSlot, Item> e : player.getEquipped().entrySet()) {
            if (e.getValue() != null && e.getValue().getId().equals(itemId)) {
                Item removed = e.getValue();
                player.getEquipped().remove(e.getKey());
                player.getInventory().add(removed);
                bus.log("Unequipped " + removed.getName() + ".");
                return;
            }
        }
        bus.log("No equipped item '" + itemId + "'.");
    }

    private void doReforge(String itemId) {
        Item target = findInventoryItem(itemId);
        boolean fromInventory = target != null;
        EquipmentSlot equippedSlot = null;
        if (target == null) {
            for (Map.Entry<EquipmentSlot, Item> e : player.getEquipped().entrySet()) {
                if (e.getValue() != null && e.getValue().getId().equals(itemId)) {
                    target = e.getValue(); equippedSlot = e.getKey(); break;
                }
            }
        }
        if (target == null) { bus.log("No item '" + itemId + "' to reforge."); return; }

        ReforgeResult result = economy.reforge(player, target);
        if (!result.isSuccess()) { bus.log(result.getMessage()); return; }

        if (result.getOutcome() == ReforgeResult.Outcome.SHATTERED || result.getResultItem() == null) {
            if (fromInventory) player.getInventory().remove(target);
            else if (equippedSlot != null) player.getEquipped().remove(equippedSlot);
        } else {
            if (fromInventory) {
                int idx = player.getInventory().indexOf(target);
                if (idx >= 0) player.getInventory().set(idx, result.getResultItem());
            } else if (equippedSlot != null) {
                player.getEquipped().put(equippedSlot, result.getResultItem());
            }
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

    private void summonPet() {
        Pet template = content.getSummonedPet();
        if (template == null) return;
        this.activePet = template.spawnCopy();
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
                progression.grantXp(player, e.getXpReward());
                Item drop = loot.rollLoot(content.getLootTable(e.getLootTableId()), player.getLevel());
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
            Realm.Encounter enc = realmEngine.encounterAt(currentRealm, encounterIndex);
            if (enc != null && enc.getCurrencyReward() > 0) {
                economy.addCurrency(player, enc.getCurrencyReward());
                bus.log("Encounter cleared! +" + enc.getCurrencyReward() + " gold.");
            } else {
                bus.log("Encounter cleared!");
            }
            if (realmEngine.hasEncounter(currentRealm, encounterIndex + 1)) {
                bus.log("ADVANCE_ENCOUNTER to press onward.");
            } else {
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

    private Item findInventoryItem(String id) {
        List<Item> inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) if (inv.get(i).getId().equals(id)) return inv.get(i);
        return null;
    }

    private Item removeFromInventory(String id, boolean equipmentOnly) {
        List<Item> inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            Item it = inv.get(i);
            if (it.getId().equals(id) && (!equipmentOnly || it.isEquipment())) {
                inv.remove(i);
                return it;
            }
        }
        return null;
    }

    private GameStateSnapshot buildSnapshot() {
        return new EngineSnapshot(player, currentRealm, enemies, bus.recentLog(), inCombat, activePet);
    }

    private void fireState() {
        bus.stateChanged(buildSnapshot());
    }

    // ---- save / load --------------------------------------------------------------------------

    private void writeSave(BufferedWriter w) throws IOException {
        line(w, "ARPG_SAVE v1");
        line(w, "player.name=" + player.getName());
        line(w, "player.class=" + player.getCharacterClass().name());
        line(w, "player.level=" + player.getLevel());
        line(w, "player.xp=" + player.getXp());
        line(w, "player.attrPoints=" + player.getUnspentAttributePoints());
        line(w, "player.currency=" + player.getCurrency());
        line(w, "player.maxhp=" + player.getMaxHealth());
        line(w, "player.hp=" + player.getCurrentHealth());
        line(w, "player.maxres=" + player.getMaxResource());
        line(w, "player.res=" + player.getCurrentResource());
        for (AttributeType t : AttributeType.values()) line(w, "attr." + t.name() + "=" + player.getAttribute(t));
        for (int i = 0; i < player.getAbilities().size(); i++) line(w, "ability=" + player.getAbilities().get(i).getId());
        for (Map.Entry<EquipmentSlot, Item> e : player.getEquipped().entrySet()) {
            line(w, "equip=" + e.getKey().name() + ">" + serializeItem(e.getValue()));
        }
        for (int i = 0; i < player.getInventory().size(); i++) line(w, "inv=" + serializeItem(player.getInventory().get(i)));
        line(w, "realm=" + (currentRealm == null ? "NONE" : currentRealm.getId()));
        line(w, "encounterIndex=" + encounterIndex);
        line(w, "inCombat=" + inCombat);
        line(w, "gameOver=" + gameOver);
        if (activePet == null) line(w, "pet=NONE");
        else line(w, "pet=" + activePet.getId() + "|" + activePet.getCurrentHealth());
        if (inCombat) {
            for (int i = 0; i < enemies.size(); i++) {
                Enemy e = enemies.get(i);
                line(w, "enemy=" + e.getId() + "|" + e.getCurrentHealth());
            }
        }
    }

    private void readSave(BufferedReader r) throws IOException {
        String header = r.readLine();
        if (header == null || !header.startsWith("ARPG_SAVE")) {
            throw new RuntimeException("bad header");
        }
        String name = "Hero";
        CharacterClass clazz = CharacterClass.WARRIOR;
        // First pass to discover name+class so we can build the Character.
        List<String> lines = new ArrayList<String>();
        String ln;
        while ((ln = r.readLine()) != null) {
            lines.add(ln);
            if (ln.startsWith("player.name=")) name = value(ln);
            else if (ln.startsWith("player.class=")) clazz = CharacterClass.valueOf(value(ln));
        }
        Character p = new Character(name, clazz);
        p.getAbilities().clear();
        p.getInventory().clear();
        p.getEquipped().clear();

        this.enemies.clear();
        this.rewarded.clear();
        this.activePet = null;
        this.currentRealm = null;
        this.encounterIndex = 0;
        this.inCombat = false;
        this.gameOver = false;

        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            String v = value(l);
            if (l.startsWith("player.level=")) p.setLevel(Integer.parseInt(v));
            else if (l.startsWith("player.xp=")) p.setXp(Integer.parseInt(v));
            else if (l.startsWith("player.attrPoints=")) p.setUnspentAttributePoints(Integer.parseInt(v));
            else if (l.startsWith("player.currency=")) p.setCurrency(Long.parseLong(v));
            else if (l.startsWith("player.maxhp=")) p.setMaxHealth(Integer.parseInt(v));
            else if (l.startsWith("player.hp=")) p.setCurrentHealth(Integer.parseInt(v));
            else if (l.startsWith("player.maxres=")) p.setMaxResource(Integer.parseInt(v));
            else if (l.startsWith("player.res=")) p.setCurrentResource(Integer.parseInt(v));
            else if (l.startsWith("attr.")) {
                String key = l.substring("attr.".length(), l.indexOf('='));
                p.setAttribute(AttributeType.valueOf(key), Integer.parseInt(v));
            } else if (l.startsWith("ability=")) {
                Ability a = content.getAbility(v);
                if (a != null) p.getAbilities().add(a);
            } else if (l.startsWith("equip=")) {
                int gt = v.indexOf('>');
                EquipmentSlot slot = EquipmentSlot.valueOf(v.substring(0, gt));
                Item it = parseItem(v.substring(gt + 1));
                if (it != null) p.getEquipped().put(slot, it);
            } else if (l.startsWith("inv=")) {
                Item it = parseItem(v);
                if (it != null) p.getInventory().add(it);
            } else if (l.startsWith("realm=")) {
                this.currentRealm = "NONE".equals(v) ? null : realmEngine.getRealm(v);
            } else if (l.startsWith("encounterIndex=")) {
                this.encounterIndex = Integer.parseInt(v);
            } else if (l.startsWith("inCombat=")) {
                this.inCombat = Boolean.parseBoolean(v);
            } else if (l.startsWith("gameOver=")) {
                this.gameOver = Boolean.parseBoolean(v);
            } else if (l.startsWith("pet=")) {
                if (!"NONE".equals(v)) {
                    String[] parts = v.split("\\|");
                    Pet tmpl = content.getPetTemplate(parts[0]);
                    if (tmpl != null) {
                        Pet pet = tmpl.spawnCopy();
                        int hp = parts.length > 1 ? Integer.parseInt(parts[1]) : pet.getMaxHealth();
                        pet.applyDamage(pet.getMaxHealth() - hp);
                        this.activePet = pet;
                    }
                }
            } else if (l.startsWith("enemy=")) {
                String[] parts = v.split("\\|");
                Enemy tmpl = content.getEnemyTemplate(parts[0]);
                if (tmpl != null) {
                    Enemy e = tmpl.spawnCopy();
                    int hp = parts.length > 1 ? Integer.parseInt(parts[1]) : e.getMaxHealth();
                    e.applyDamage(e.getMaxHealth() - hp);
                    this.enemies.add(e);
                }
            }
        }
        this.player = p;
        combat.resetCooldowns();
    }

    private static String serializeItem(Item it) {
        String slot = it.getSlot() == null ? "NONE" : it.getSlot().name();
        return it.getId() + "\t" + it.getName() + "\t" + slot + "\t" + it.getRarity().name() + "\t"
                + it.getAttackModifier() + "\t" + it.getDefenseModifier() + "\t" + it.getVitalityModifier()
                + "\t" + it.getLevelRequirement() + "\t" + it.getSellValue();
    }

    private static Item parseItem(String s) {
        String[] p = s.split("\t");
        if (p.length < 9) return null;
        EquipmentSlot slot = "NONE".equals(p[2]) ? null : EquipmentSlot.valueOf(p[2]);
        return new Item(p[0], p[1], slot, Rarity.valueOf(p[3]),
                Integer.parseInt(p[4]), Integer.parseInt(p[5]), Integer.parseInt(p[6]),
                Integer.parseInt(p[7]), Integer.parseInt(p[8]));
    }

    private static void line(BufferedWriter w, String s) throws IOException {
        w.write(s);
        w.newLine();
    }

    private static String value(String kv) {
        int idx = kv.indexOf('=');
        return idx < 0 ? "" : kv.substring(idx + 1);
    }
}
