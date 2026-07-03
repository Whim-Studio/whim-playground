package com.arpg.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * A tiny standalone smoke test for the domain layer. Constructs the seed
 * content, exercises a few data operations, and round-trips a {@link Character}
 * through Java serialization to confirm the save/load contract holds. Contains
 * no gameplay logic — it only asserts that the data model behaves as data.
 *
 * <p>Run: {@code java -cp out com.arpg.model.DomainSelfCheck}</p>
 */
public final class DomainSelfCheck {

    private DomainSelfCheck() {
    }

    public static void main(String[] args) throws Exception {
        int checks = 0;

        // Content loads.
        require(GameContent.getRealms().size() >= 3, "expected 3+ realms");
        require(!GameContent.getAllAbilities().isEmpty(), "expected abilities");
        require(!GameContent.getAllItems().isEmpty(), "expected items");
        checks += 3;

        // Every class's ability ids resolve.
        for (int i = 0; i < CharacterClass.values().length; i++) {
            CharacterClass cls = CharacterClass.values()[i];
            List<String> ids = cls.getAbilityIds();
            for (int j = 0; j < ids.size(); j++) {
                require(GameContent.getAbility(ids.get(j)) != null,
                        "missing ability " + ids.get(j) + " for " + cls);
                checks++;
            }
        }

        // Boss present in at least one realm.
        boolean bossFound = false;
        for (Realm r : GameContent.getRealms()) {
            for (Realm.EncounterDef enc : r.getEncounters()) {
                if (enc.getType() == Realm.EncounterType.BOSS) {
                    Enemy boss = GameContent.spawnEnemy(enc.getBossEnemyId());
                    require(boss != null && boss.isBoss(), "boss id must resolve to a boss enemy");
                    bossFound = true;
                    checks++;
                }
            }
        }
        require(bossFound, "expected at least one boss encounter");
        checks++;

        // Character build + combat data mutation.
        Character hero = GameContent.createStartingCharacter("Vael", CharacterClass.IRONCLAD_VANGUARD);
        int max = hero.getMaxHealth();
        hero.applyDamage(9999);
        require(hero.getCurrentHealth() == 0 && !hero.isAlive(), "damage should clamp to 0");
        hero.applyHealing(9999);
        require(hero.getCurrentHealth() == max, "healing should clamp to max");
        checks += 2;

        // Equipping changes derived stats.
        Equipment chest = GameContent.getItem("item.plate_cuirass");
        int before = hero.getMaxHealth();
        hero.equip(chest);
        require(hero.getMaxHealth() > before, "equipping vitality gear should raise max health");
        require(hero.getEquipped(EquipmentSlot.CHEST) != null, "chest slot should be filled");
        checks += 2;

        // Attribute allocation.
        hero.grantAttributePoints(1);
        int str = hero.getStrength();
        require(hero.allocateAttribute("strength"), "allocation should succeed with a point");
        require(hero.getStrength() == str + 1, "strength should increase");
        checks += 2;

        // Serialization round-trip.
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(hero);
        oos.close();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        Character restored = (Character) ois.readObject();
        ois.close();
        require(restored.getName().equals(hero.getName()), "name survives serialization");
        require(restored.getMaxHealth() == hero.getMaxHealth(), "max health survives serialization");
        require(restored.getEquipped(EquipmentSlot.CHEST) != null, "equipment survives serialization");
        checks += 3;

        // LootTable weighted pick is deterministic given the roll.
        LootTable table = GameContent.getEnemyTemplate("mob.mire_rat").getLootTable();
        require(table.pick(0) != null, "loot pick at roll 0 should return an item");
        require(table.getTotalWeight() > 0, "loot table should have weight");
        checks += 2;

        System.out.println("com.arpg.model self-check PASSED (" + checks + " assertions).");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("SELF-CHECK FAILED: " + message);
        }
    }
}
