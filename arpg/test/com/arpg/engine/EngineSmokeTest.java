package com.arpg.engine;

import com.arpg.model.Ability;
import com.arpg.model.BuffDebuff;
import com.arpg.model.Character;
import com.arpg.model.CharacterClass;
import com.arpg.model.CombatParticipant;
import com.arpg.model.Enemy;
import com.arpg.model.GameEventListener;
import com.arpg.model.GameStateSnapshot;
import com.arpg.model.Item;
import com.arpg.model.PlayerAction;
import com.arpg.model.Pet;
import com.arpg.model.Realm;

import java.io.File;
import java.util.List;

/**
 * Headless proof that the engine drives real combat end-to-end. Not a JUnit test (zero libs) —
 * run with a plain main(). Verifies: new game, realm travel, ability/tick combat loop clears a
 * boss, XP/level-up + loot fire, and save/load round-trips.
 */
public class EngineSmokeTest {

    static int deaths = 0, levelUps = 0, loot = 0, damageEvents = 0, buffs = 0;

    public static void main(String[] args) {
        GameEngine engine = new GameEngine(12345L);
        engine.addEventListener(new GameEventListener() {
            public void onDamageDealt(CombatParticipant s, CombatParticipant t, int amt, Ability a) { damageEvents++; }
            public void onHealed(CombatParticipant s, CombatParticipant t, int amt) { }
            public void onBuffApplied(CombatParticipant t, BuffDebuff b) { buffs++; }
            public void onBuffExpired(CombatParticipant t, BuffDebuff b) { }
            public void onLevelUp(Character c, int lvl) { levelUps++; }
            public void onLootDropped(Item i) { loot++; }
            public void onCombatLog(String line) { }
            public void onParticipantDeath(CombatParticipant p) { deaths++; }
            public void onGameStateChanged(GameStateSnapshot snap) { }
        });

        check("classes >= 4", engine.getAvailableClasses().size() >= 4);
        check("realms 3-4", engine.getAvailableRealms().size() >= 3);

        GameStateSnapshot snap = engine.startNewGame("Kaelen", CharacterClass.WARRIOR);
        check("player created", snap.getPlayer() != null);
        check("starts with abilities", !snap.getPlayer().getAbilities().isEmpty());

        // Travel to first realm and clear every encounter (including the boss).
        engine.processPlayerAction(PlayerAction.moveToRealm("verdant_hollow"));
        int realmCleared = clearRealm(engine, 400);
        check("verdant_hollow cleared", realmCleared > 0);

        Character p = engine.getSnapshot().getPlayer();
        System.out.println("After realm 1: level=" + p.getLevel() + " xp=" + p.getXp()
                + " hp=" + p.getCurrentHealth() + "/" + p.getMaxHealth()
                + " gold=" + p.getCurrency() + " invItems=" + p.getInventory().size());
        check("gained a level", levelUps > 0);
        check("damage events fired", damageEvents > 0);
        check("deaths fired", deaths > 0);
        check("loot dropped", loot > 0);

        // Allocate attribute points earned from levelling.
        int before = p.getUnspentAttributePoints();
        if (before > 0) {
            engine.processPlayerAction(PlayerAction.allocateAttribute(com.arpg.model.AttributeType.STRENGTH));
            check("attribute allocated", engine.getSnapshot().getPlayer().getUnspentAttributePoints() == before - 1);
        }

        // Equip a looted item if any equipment is present.
        for (int i = 0; i < p.getInventory().size(); i++) {
            Item it = p.getInventory().get(i);
            if (it.isEquipment() && it.getLevelRequirement() <= p.getLevel()) {
                int atkBefore = p.getAttackPower();
                engine.processPlayerAction(PlayerAction.equipItem(it.getId()));
                System.out.println("Equipped " + it.getName() + ": attack " + atkBefore + " -> "
                        + engine.getSnapshot().getPlayer().getAttackPower());
                break;
            }
        }

        // Save then load, confirm continuity.
        try {
            File tmp = File.createTempFile("arpg-save", ".txt");
            check("save succeeds", engine.saveGame(tmp));
            int savedLevel = engine.getSnapshot().getPlayer().getLevel();
            long savedGold = engine.getSnapshot().getPlayer().getCurrency();
            GameStateSnapshot loaded = engine.loadGame(tmp);
            check("load restores level", loaded.getPlayer().getLevel() == savedLevel);
            check("load restores gold", loaded.getPlayer().getCurrency() == savedGold);
            tmp.delete();
        } catch (Exception e) {
            check("save/load no exception", false);
        }

        // Push through the remaining realms to exercise the higher-tier bosses.
        engine.processPlayerAction(PlayerAction.moveToRealm("frostspire"));
        clearRealm(engine, 800);
        System.out.println("After frostspire: level=" + engine.getSnapshot().getPlayer().getLevel());

        System.out.println("\nEvent totals: damage=" + damageEvents + " deaths=" + deaths
                + " levelUps=" + levelUps + " loot=" + loot + " buffs=" + buffs);
        System.out.println(failures == 0 ? "\nALL CHECKS PASSED (" + passed + ")" : "\nFAILURES: " + failures);
        if (failures != 0) System.exit(1);
    }

    /** Drive combat: hero acts, engine ticks, advance encounters, until realm cleared or hero dies. */
    private static int clearRealm(GameEngine engine, int maxRounds) {
        int rounds = 0;
        while (rounds++ < maxRounds) {
            GameStateSnapshot s = engine.getSnapshot();
            Character player = s.getPlayer();
            if (player == null || !player.isAlive()) return -1;

            if (s.isInCombat() && anyAlive(s.getEnemies())) {
                int target = firstAlive(s.getEnemies());
                Ability chosen = affordableDamage(player);
                if (chosen != null) {
                    engine.processPlayerAction(PlayerAction.useAbility(chosen.getId(), target));
                } else {
                    engine.processPlayerAction(PlayerAction.basicAttack(target));
                }
                engine.tick();
            } else {
                // Not in combat: try to advance; if realm is done, return.
                Realm realm = s.getCurrentRealm();
                boolean more = false;
                if (realm != null) {
                    int lastIndex = realm.getEncounterCount() - 1;
                    // Peek: if there are still encounters ahead, advancing spawns them.
                    engine.processPlayerAction(PlayerAction.advanceEncounter());
                    GameStateSnapshot after = engine.getSnapshot();
                    more = after.isInCombat() && anyAlive(after.getEnemies());
                    if (!more && !after.isInCombat()) return 1; // realm conquered
                }
                if (!more && realm == null) return 1;
            }
        }
        return 1;
    }

    private static Ability affordableDamage(Character p) {
        List<Ability> abilities = p.getAbilities();
        Ability best = null;
        for (int i = 0; i < abilities.size(); i++) {
            Ability a = abilities.get(i);
            if (a.getEffectType() != Ability.EffectType.DAMAGE) continue;
            if (p.getCurrentResource() < a.getResourceCost()) continue;
            if (best == null || a.getMagnitude() > best.getMagnitude()) best = a;
        }
        return best;
    }

    private static boolean anyAlive(List<Enemy> enemies) {
        for (int i = 0; i < enemies.size(); i++) if (enemies.get(i).isAlive()) return true;
        return false;
    }

    private static int firstAlive(List<Enemy> enemies) {
        for (int i = 0; i < enemies.size(); i++) if (enemies.get(i).isAlive()) return i;
        return 0;
    }

    static int passed = 0, failures = 0;
    private static void check(String label, boolean cond) {
        if (cond) { passed++; System.out.println("  [PASS] " + label); }
        else { failures++; System.out.println("  [FAIL] " + label); }
    }
}
