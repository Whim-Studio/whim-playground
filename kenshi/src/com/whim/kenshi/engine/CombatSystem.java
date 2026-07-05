package com.whim.kenshi.engine;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums.BodyPart;
import com.whim.kenshi.api.Enums.SkillType;
import com.whim.kenshi.api.Enums.WeaponClass;
import com.whim.kenshi.domain.Anatomy;
import com.whim.kenshi.domain.Character;

import java.util.Random;

/**
 * Resolves a single melee swing from attacker to defender per the contract's
 * hit-chance formula, allocates the damage to ONE weighted-random body part,
 * applies bleed, awards skill XP, and emits a log line.
 *
 * <pre>
 *   hitChance = clamp(BASE_HIT_CHANCE
 *                     + HIT_CHANCE_PER_SKILL*(atkMeleeAttack - defMeleeDefence),
 *                     MIN_HIT_CHANCE, MAX_HIT_CHANCE)
 * </pre>
 */
final class CombatSystem {

    /** Weighted-random target weights per body part (torsos favoured over limbs). */
    private static final BodyPart[] PARTS = BodyPart.values();
    private static final int[] WEIGHTS = buildWeights();

    private static int[] buildWeights() {
        int[] w = new int[PARTS.length];
        for (int i = 0; i < PARTS.length; i++) {
            BodyPart p = PARTS[i];
            switch (p) {
                case HEAD:    w[i] = 8;  break;
                case CHEST:   w[i] = 18; break;
                case STOMACH: w[i] = 15; break;
                default:      w[i] = 12; break; // each arm / leg
            }
        }
        return w;
    }

    private static final double WEIGHT_TOTAL = sum(WEIGHTS);

    private static double sum(int[] a) {
        double s = 0;
        for (int i = 0; i < a.length; i++) {
            s += a[i];
        }
        return s;
    }

    private final Random rng;
    private final EventLog log;

    CombatSystem(Random rng, EventLog log) {
        this.rng = rng;
        this.log = log;
    }

    double hitChance(Character attacker, Character defender) {
        int atk = attacker.skills().level(SkillType.MELEE_ATTACK);
        int def = defender.skills().level(SkillType.MELEE_DEFENCE);
        double raw = Config.BASE_HIT_CHANCE + Config.HIT_CHANCE_PER_SKILL * (atk - def);
        if (raw < Config.MIN_HIT_CHANCE) { raw = Config.MIN_HIT_CHANCE; }
        if (raw > Config.MAX_HIT_CHANCE) { raw = Config.MAX_HIT_CHANCE; }
        return raw;
    }

    private double rollDamage(Character attacker) {
        WeaponClass w = attacker.effectiveWeapon();
        double mult;
        switch (w) {
            case TWO_HANDED: mult = 1.6; break;
            case ONE_HANDED: mult = 1.0; break;
            default:         mult = 0.65; break; // UNARMED
        }
        int str = attacker.skills().level(SkillType.STRENGTH);
        double base = 8.0 + 0.18 * (str - Config.SKILL_MIN);
        double variance = 0.7 + 0.6 * rng.nextDouble(); // 0.7 .. 1.3
        return base * mult * variance;
    }

    private BodyPart pickPart() {
        double r = rng.nextDouble() * WEIGHT_TOTAL;
        double acc = 0;
        for (int i = 0; i < PARTS.length; i++) {
            acc += WEIGHTS[i];
            if (r <= acc) {
                return PARTS[i];
            }
        }
        return BodyPart.CHEST;
    }

    /**
     * Resolve one swing. Returns true if the swing connected. Mutates the
     * defender's anatomy/bleed and both fighters' skills, and logs the outcome.
     */
    boolean resolveSwing(long tick, Character attacker, Character defender) {
        // Attacker always trains attack a little; defender trains defence for
        // facing a swing.
        SkillSystem.award(attacker, SkillType.MELEE_ATTACK, SkillSystem.XP_SWING);
        SkillSystem.award(defender, SkillType.MELEE_DEFENCE, SkillSystem.XP_DEFEND);

        double chance = hitChance(attacker, defender);
        if (rng.nextDouble() > chance) {
            log.add(tick, attacker.name() + " misses " + defender.name());
            return false;
        }

        double dmg = rollDamage(attacker);
        BodyPart part = pickPart();
        Anatomy a = defender.anatomy();
        double before = a.hp(part);
        a.damage(part, dmg);
        double actual = before - a.hp(part);
        if (actual < 0) { actual = 0; }

        // Bleed: a fraction of the landed damage becomes added bleed rate.
        double addedBleed = dmg * Config.BLEED_FROM_DAMAGE;
        defender.setBleedRate(defender.bleedRate() + addedBleed);

        // XP: attacker melee/strength/dexterity on a hit; defender toughness.
        SkillSystem.award(attacker, SkillType.MELEE_ATTACK, SkillSystem.XP_HIT);
        SkillSystem.award(attacker, SkillType.STRENGTH, SkillSystem.XP_STRENGTH_PER_HIT);
        SkillSystem.award(attacker, SkillType.DEXTERITY, SkillSystem.XP_DEXTERITY_PER_HIT);
        SkillSystem.award(defender, SkillType.TOUGHNESS, actual * SkillSystem.XP_TOUGHNESS_PER_DMG);

        String msg = attacker.name() + " hits " + defender.name()
                + " in the " + part.label() + " (" + fmt(dmg) + ")";
        // The DOWNED/DEAD recompute itself belongs to SurvivalSystem; here we
        // only annotate the log with the immediate consequence.
        if (a.isDead()) {
            msg = msg + " — a killing blow";
        } else if (a.disabled(part)) {
            msg = msg + " — " + part.label() + " disabled";
        }
        log.add(tick, msg);
        return true;
    }

    private static String fmt(double d) {
        long r = Math.round(d);
        return Long.toString(r);
    }
}
