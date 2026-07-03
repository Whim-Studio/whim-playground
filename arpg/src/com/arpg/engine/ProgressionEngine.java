package com.arpg.engine;

import com.arpg.model.Ability;
import com.arpg.model.Character;
import com.arpg.model.CharacterClass;
import com.arpg.model.GameContent;

import java.util.List;

/**
 * XP gain, level-up thresholds, attribute allocation and milestone ability unlocks.
 * Level thresholds and pool recomputation live on {@link Character}; this engine drives WHEN
 * to level up and fans out the resulting events.
 */
final class ProgressionEngine {
    static final int MAX_LEVEL = 30;
    /** Class ability list indices unlock at these levels (5 abilities per class). */
    private static final int[] ABILITY_UNLOCK_LEVELS = {1, 2, 3, 5, 7};

    private final EventBus bus;

    ProgressionEngine(EventBus bus) {
        this.bus = bus;
    }

    /** Grant XP and process any resulting level-ups (may be several at once). */
    void grantXp(Character player, long amount) {
        if (amount <= 0 || player.getLevel() >= MAX_LEVEL) return;
        player.addExperience(amount);
        bus.log(player.getName() + " gains " + amount + " XP.");
        while (player.getLevel() < MAX_LEVEL && player.canLevelUp()) {
            player.levelUp();
            syncAbilities(player);
            bus.log(player.getName() + " reached level " + player.getLevel() + "!");
            bus.levelUp(player, player.getLevel());
        }
    }

    /** Ensure the character has every ability unlocked at or below its current level. */
    void syncAbilities(Character player) {
        CharacterClass clazz = player.getCharacterClass();
        List<String> classAbilities = clazz.getAbilityIds();
        List<Ability> owned = player.getAbilities();
        for (int i = 0; i < classAbilities.size(); i++) {
            int unlockLevel = i < ABILITY_UNLOCK_LEVELS.length ? ABILITY_UNLOCK_LEVELS[i] : (i + 1);
            if (player.getLevel() < unlockLevel) continue;
            String abilityId = classAbilities.get(i);
            if (!hasAbility(owned, abilityId)) {
                Ability a = GameContent.getAbility(abilityId);
                if (a != null) {
                    owned.add(a);
                    bus.log(player.getName() + " unlocked ability: " + a.getName() + ".");
                }
            }
        }
    }

    private static boolean hasAbility(List<Ability> owned, String id) {
        for (int i = 0; i < owned.size(); i++) if (owned.get(i).getId().equals(id)) return true;
        return false;
    }

    /** Spend one unspent attribute point on the named attribute. Returns false when none are available. */
    boolean allocate(Character player, String attribute) {
        if (attribute == null || player.getUnspentAttributePoints() <= 0) return false;
        boolean ok = player.allocateAttribute(attribute);
        if (ok) bus.log(player.getName() + " allocates a point into " + attribute + ".");
        return ok;
    }
}
