package com.whim.kenshi.domain;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums.SkillType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per-character trainable skills, each an integer level in
 * {@code [SKILL_MIN, SKILL_MAX]} (1..100) plus fractional XP toward the next
 * level. XP-to-level uses a gently rising curve so early levels come fast and
 * high levels are a grind (see RESEARCH.md).
 *
 * <p>Pure state: {@link #addXp} accumulates XP and promotes the level; the
 * engine's SkillSystem decides <i>when</i> to award XP.
 */
public final class Skills {

    /** XP required to advance from level L to L+1 == BASE + PER_LEVEL*L. */
    public static final double XP_BASE = 10.0;
    public static final double XP_PER_LEVEL = 2.5;

    private final Map<SkillType, Integer> level = new EnumMap<SkillType, Integer>(SkillType.class);
    private final Map<SkillType, Double> xp = new EnumMap<SkillType, Double>(SkillType.class);

    /** All skills at {@link Config#SKILL_MIN}. */
    public Skills() {
        for (SkillType s : SkillType.values()) {
            level.put(s, Config.SKILL_MIN);
            xp.put(s, 0.0);
        }
    }

    /** Current integer level (1..100) of a skill. */
    public int level(SkillType skill) {
        return level.get(skill);
    }

    /** Fractional XP banked toward the next level (>= 0). */
    public double xp(SkillType skill) {
        return xp.get(skill);
    }

    /** XP needed to move from {@code fromLevel} to {@code fromLevel + 1}. */
    public static double xpForNext(int fromLevel) {
        return XP_BASE + XP_PER_LEVEL * fromLevel;
    }

    /** Force a skill to a specific level (clamped 1..100); resets banked XP. */
    public void setLevel(SkillType skill, int value) {
        if (value < Config.SKILL_MIN) value = Config.SKILL_MIN;
        if (value > Config.SKILL_MAX) value = Config.SKILL_MAX;
        level.put(skill, value);
        xp.put(skill, 0.0);
    }

    /**
     * Add {@code amount} XP to a skill, promoting the level while enough XP has
     * accumulated. Caps at {@link Config#SKILL_MAX}. Negative/zero is a no-op.
     */
    public void addXp(SkillType skill, double amount) {
        if (amount <= 0.0) return;
        int lvl = level.get(skill);
        double banked = xp.get(skill) + amount;
        while (lvl < Config.SKILL_MAX && banked >= xpForNext(lvl)) {
            banked -= xpForNext(lvl);
            lvl++;
        }
        if (lvl >= Config.SKILL_MAX) {
            lvl = Config.SKILL_MAX;
            banked = 0.0;
        }
        level.put(skill, lvl);
        xp.put(skill, banked);
    }
}
