package com.whim.kenshi.domain;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums.BodyPart;

import java.util.EnumMap;
import java.util.Map;

/**
 * The seven-part limb-damage model. Each {@link BodyPart} has an independent
 * current-HP pool and a per-part maximum. Parts start at max, can be healed up
 * to max, and can fall as low as {@code -max} (the death floor). A part is
 * <b>disabled</b> once its HP is {@code <= 0}.
 *
 * <p>Pure state + derivations only — no combat, AI, or survival logic lives
 * here (that is the engine's job). The engine mutates via {@link #damage} /
 * {@link #heal}; Views read via {@link #hp} / {@link #max} / {@link #disabled}.
 */
public final class Anatomy {

    private final Map<BodyPart, Double> hp = new EnumMap<BodyPart, Double>(BodyPart.class);
    private final Map<BodyPart, Double> max = new EnumMap<BodyPart, Double>(BodyPart.class);

    /** New anatomy with all parts full at the contract default maxima. */
    public Anatomy() {
        for (BodyPart p : BodyPart.values()) {
            double m = p.vital() ? Config.TORSO_PART_MAX : Config.LIMB_PART_MAX;
            max.put(p, m);
            hp.put(p, m);
        }
    }

    /** Current HP of a part (may be negative, down to {@code -max}). */
    public double hp(BodyPart part) {
        return hp.get(part);
    }

    /** Maximum HP of a part. */
    public double max(BodyPart part) {
        return max.get(part);
    }

    /** Fraction of max in [-1, 1] — handy for HUD colour grading. */
    public double fraction(BodyPart part) {
        double m = max.get(part);
        return m == 0.0 ? 0.0 : hp.get(part) / m;
    }

    /**
     * Apply {@code amount} of damage to a part (positive lowers HP). Clamps at
     * the {@code -max} floor. Negative amounts heal (and are clamped at max).
     */
    public void damage(BodyPart part, double amount) {
        double m = max.get(part);
        double floor = m * Config.PART_MIN_FRACTION; // == -max
        double next = hp.get(part) - amount;
        if (next < floor) next = floor;
        if (next > m) next = m;
        hp.put(part, next);
    }

    /** Heal a part by {@code amount} (positive raises HP), clamped to max. */
    public void heal(BodyPart part, double amount) {
        damage(part, -amount);
    }

    /** Directly set a part's current HP, clamped to [-max, max]. */
    public void setHp(BodyPart part, double value) {
        double m = max.get(part);
        double floor = m * Config.PART_MIN_FRACTION;
        if (value < floor) value = floor;
        if (value > m) value = m;
        hp.put(part, value);
    }

    /** True once a part's HP has fallen to/below the disabled threshold. */
    public boolean disabled(BodyPart part) {
        return hp.get(part) <= Config.PART_DISABLED_AT;
    }

    /** True when both legs are disabled → the character can only crawl. */
    public boolean bothLegsDown() {
        return disabled(BodyPart.LEFT_LEG) && disabled(BodyPart.RIGHT_LEG);
    }

    /** True when at least one arm is disabled → cannot wield two-handed. */
    public boolean anyArmDown() {
        return disabled(BodyPart.LEFT_ARM) || disabled(BodyPart.RIGHT_ARM);
    }

    /** True when a vital part (Head or Chest) has hit its {@code -max} floor. */
    public boolean isDead() {
        for (BodyPart p : BodyPart.values()) {
            if (!p.vital()) continue;
            double floor = max.get(p) * Config.PART_MIN_FRACTION;
            if (hp.get(p) <= floor) return true;
        }
        return false;
    }

    /**
     * Unconscious-but-alive test. A character is downed if the Stomach is
     * disabled or blood has fallen too low (the blood test is supplied by the
     * caller since blood lives on {@link Character}).
     */
    public boolean isDowned(boolean bloodLow) {
        return !isDead() && (disabled(BodyPart.STOMACH) || bloodLow);
    }
}
