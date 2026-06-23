package com.tiwas.rpg.engine;

import com.tiwas.rpg.domain.Character;
import com.tiwas.rpg.domain.Skill;

/**
 * Core skill-test loop: roll-under resolution with exertion, pool overflow to HP,
 * margin, failure XP, doubles, advanced-skill unlock, and end-of-action recovery.
 * All math rounds DOWN.
 */
public final class ActionResolver {
    private final Dice dice;

    public ActionResolver(Dice dice) {
        if (dice == null) {
            throw new IllegalArgumentException("dice must not be null");
        }
        this.dice = dice;
    }

    public ActionResult resolve(Character actor, Skill skill) {
        return resolve(actor, skill, 0);
    }

    public ActionResult resolve(Character actor, Skill skill, int dm) {
        boolean usedMind = skill.isMind();

        int effectiveSkill = clamp(skill.getValue() + dm, 1, 99);
        int roll = dice.d100();
        boolean success = roll <= effectiveSkill;

        int pool = usedMind ? actor.getCurrentMP() : actor.getCurrentPE();
        int poolMax = usedMind ? actor.getMaxMP() : actor.getMaxPhysicalEnergy();
        int regen = usedMind ? actor.getMpRegen() : actor.getEnergyRegen();

        int cost = roll;
        int overflow;
        if (cost <= pool) {
            pool -= cost;
            overflow = 0;
        } else {
            overflow = cost - pool;
            pool = 0;
        }

        int hp = actor.getCurrentHP();
        if (overflow > 0) {
            hp -= overflow;
            if (hp < 0) {
                hp = 0;
            }
        }

        int margin = success ? (effectiveSkill - roll) / 10 : 0;
        boolean doubles = isDoubles(roll);
        int failureXP = success ? 0 : Math.max(0, roll - effectiveSkill);
        boolean unlockedAdvancedSkill = !success && doubles;

        int recovered = regen / 2;
        pool = Math.min(pool + recovered, poolMax);

        if (usedMind) {
            actor.setCurrentMP(pool);
        } else {
            actor.setCurrentPE(pool);
        }
        actor.setCurrentHP(hp);

        return new ActionResult(skill.getName(), roll, effectiveSkill, success, cost,
                overflow, margin, doubles, failureXP, unlockedAdvancedSkill, recovered,
                usedMind, pool, hp);
    }

    private static int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    private static boolean isDoubles(int roll) {
        return roll == 11 || roll == 22 || roll == 33 || roll == 44 || roll == 55
                || roll == 66 || roll == 77 || roll == 88 || roll == 99;
    }
}
