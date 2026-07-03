package com.arpg.engine;

import com.arpg.model.Ability;
import com.arpg.model.AttributeType;
import com.arpg.model.Character;
import com.arpg.model.CharacterClass;
import com.arpg.model.GameContent;

import java.util.List;

/**
 * XP gain, level-up thresholds, attribute allocation and milestone ability unlocks.
 * Pure state transitions on a {@link Character}; fires level-up events through the bus.
 */
final class ProgressionEngine {
    static final int MAX_LEVEL = 30;
    private static final int ATTR_POINTS_PER_LEVEL = 3;
    /** Class ability list indices unlock at these levels (5 abilities per class). */
    private static final int[] ABILITY_UNLOCK_LEVELS = {1, 2, 3, 5, 7};

    private final EventBus bus;
    private final GameContent content;

    ProgressionEngine(EventBus bus, GameContent content) {
        this.bus = bus;
        this.content = content;
    }

    /** Cumulative-free per-level requirement: XP needed to advance FROM {@code level} to the next. */
    static int xpToNext(int level) {
        return 40 + level * 40;
    }

    /** Grant XP and process any resulting level-ups (may be several at once). */
    void grantXp(Character player, int amount) {
        if (amount <= 0 || player.getLevel() >= MAX_LEVEL) return;
        player.setXp(player.getXp() + amount);
        bus.log(player.getName() + " gains " + amount + " XP.");
        while (player.getLevel() < MAX_LEVEL && player.getXp() >= xpToNext(player.getLevel())) {
            player.setXp(player.getXp() - xpToNext(player.getLevel()));
            levelUp(player);
        }
    }

    private void levelUp(Character player) {
        int newLevel = player.getLevel() + 1;
        player.setLevel(newLevel);

        int hpGain = 12 + player.getAttribute(AttributeType.VITALITY);
        int resGain = 6 + player.getAttribute(AttributeType.INTELLIGENCE) / 2;
        player.setMaxHealth(player.getMaxHealth() + hpGain);
        player.setMaxResource(player.getMaxResource() + resGain);
        // Level-up fully restores the hero.
        player.setCurrentHealth(player.getMaxHealth());
        player.setCurrentResource(player.getMaxResource());
        player.addUnspentAttributePoints(ATTR_POINTS_PER_LEVEL);

        syncAbilities(player);
        bus.log(player.getName() + " reached level " + newLevel + "!");
        bus.levelUp(player, newLevel);
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
                Ability a = content.getAbility(abilityId);
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

    /** Spend one unspent attribute point. Returns false when none are available. */
    boolean allocate(Character player, AttributeType attr) {
        if (player.getUnspentAttributePoints() <= 0 || attr == null) return false;
        player.setAttribute(attr, player.getAttribute(attr) + 1);
        player.setUnspentAttributePoints(player.getUnspentAttributePoints() - 1);
        if (attr == AttributeType.VITALITY) {
            player.setMaxHealth(player.getMaxHealth() + 8);
        } else if (attr == AttributeType.INTELLIGENCE) {
            player.setMaxResource(player.getMaxResource() + 4);
        }
        bus.log(player.getName() + " allocates a point into " + attr + ".");
        return true;
    }
}
