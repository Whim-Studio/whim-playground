package com.whim.alganon.combat;

import com.whim.alganon.api.CharacterModel;
import com.whim.alganon.api.Content;
import com.whim.alganon.api.Defs;
import com.whim.alganon.api.Enums.ChatChannel;
import com.whim.alganon.api.Enums.StatType;
import com.whim.alganon.api.GameContext;

/**
 * Level/XP curve and level-up growth. {@link CharacterModel#xp()} is treated as XP
 * banked toward the <em>current</em> level (it resets to 0 on ding).
 *
 * <p>[Gap — my design] Curve and growth numbers are tuned for a short demo playthrough
 * (fast early levels), not MMO-length grind.</p>
 */
public final class Progression {
    private Progression() {}

    /** XP required to advance from {@code level} to {@code level+1}. */
    public static long xpToNext(int level) {
        if (level < 1) level = 1;
        return 60L + 40L * level + 10L * (long) level * level;
    }

    /** Grant XP and apply as many level-ups as the banked total allows. */
    public static void grantXp(CharacterModel p, long xp, GameContext ctx, Content content) {
        if (xp <= 0) return;
        p.setXp(p.xp() + xp);
        if (ctx != null) ctx.log(ChatChannel.COMBAT, p.getName() + " gains " + xp + " XP.");
        while (p.xp() >= xpToNext(p.level())) {
            p.setXp(p.xp() - xpToNext(p.level()));
            levelUp(p, ctx, content);
        }
    }

    /** Grant class abilities whose level requirement the character now meets. */
    public static void grantEligibleAbilities(CharacterModel p, Content content, GameContext ctx) {
        if (content == null) return;
        Defs.ClassDef c = content.clazz(p.classId());
        if (c == null) return;
        for (String abilityId : c.abilityIds) {
            if (p.knownAbilityIds().contains(abilityId)) continue;
            Defs.AbilityDef a = content.ability(abilityId);
            if (a != null && p.level() >= a.levelReq) {
                p.learnAbility(abilityId);
                if (ctx != null) ctx.log(ChatChannel.SYSTEM, "You learn " + a.name + "!");
            }
        }
    }

    private static void levelUp(CharacterModel p, GameContext ctx, Content content) {
        int lvl = p.level() + 1;
        p.setLevel(lvl);

        Integer stamina = p.stats().get(StatType.STAMINA);
        int sta = stamina == null ? 0 : stamina;

        int hpGain = 12 + sta / 2;
        p.setMaxHp(p.maxHp() + hpGain);
        p.setHp(p.maxHp());

        p.setMaxResource(p.maxResource() + 4);
        p.setResource(p.maxResource());

        // Every other level bumps the two most relevant stats a touch. [Gap — my design]
        if (lvl % 2 == 0) {
            bump(p, StatType.STAMINA, 1);
            bump(p, StatType.MIGHT, 1);
        } else {
            bump(p, StatType.INTELLECT, 1);
            bump(p, StatType.SPIRIT, 1);
        }

        grantEligibleAbilities(p, content, ctx);

        if (ctx != null) {
            ctx.log(ChatChannel.SYSTEM, p.getName() + " reaches level " + lvl + "!");
            ctx.toast("Level up! You are now level " + lvl + ".");
        }
    }

    private static void bump(CharacterModel p, StatType s, int delta) {
        Integer cur = p.stats().get(s);
        p.stats().put(s, (cur == null ? 0 : cur) + delta);
    }
}
