package com.whim.kenshi.engine;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums.SkillType;
import com.whim.kenshi.domain.Character;
import com.whim.kenshi.domain.Skills;

/**
 * Awards experience for engine events. The XP -&gt; level curve itself lives in
 * {@link Skills} (Task 1 / domain): this class only decides HOW MUCH XP a given
 * action grants and forwards it via {@link Skills#addXp}. Keeping the curve in
 * the domain avoids duplicating it here.
 *
 * <p>All amounts are tuned so that a few minutes of the self-check produce
 * visible level-ups without trivialising the 1..100 range.</p>
 */
final class SkillSystem {

    /** XP granted to the attacker's MELEE_ATTACK for a landed hit. */
    static final double XP_HIT = 6.0;
    /** XP granted to the attacker's MELEE_ATTACK for a swing that missed. */
    static final double XP_SWING = 1.5;
    /** XP granted to the defender's MELEE_DEFENCE for surviving/blocking a swing. */
    static final double XP_DEFEND = 3.0;
    /** Toughness XP per point of damage actually taken. */
    static final double XP_TOUGHNESS_PER_DMG = 0.5;
    /** Strength XP per landed hit (swinging a weapon in anger). */
    static final double XP_STRENGTH_PER_HIT = 2.0;
    /** Athletics XP per world-unit moved. */
    static final double XP_ATHLETICS_PER_UNIT = 0.04;
    /** Dexterity XP per landed hit. */
    static final double XP_DEXTERITY_PER_HIT = 1.0;

    private SkillSystem() {}

    static void award(Character c, SkillType skill, double amount) {
        if (c == null || amount <= 0.0) {
            return;
        }
        c.skills().addXp(skill, amount);
    }

    /** Train Athletics for a distance moved this step. */
    static void trainMovement(Character c, double distanceMoved) {
        if (distanceMoved <= 0.0) {
            return;
        }
        award(c, SkillType.ATHLETICS, distanceMoved * XP_ATHLETICS_PER_UNIT);
    }

    /**
     * Effective move speed in world units per world-second, modulated by the
     * mover's Athletics skill. Crawling is applied by the caller.
     */
    static double moveSpeed(Character c) {
        int ath = c.skills().level(SkillType.ATHLETICS);
        double factor = 1.0 + 0.008 * (ath - Config.SKILL_MIN); // up to ~1.8x at 100
        return Config.BASE_MOVE_SPEED * factor;
    }
}
