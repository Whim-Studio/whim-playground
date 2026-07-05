package com.whim.kenshi.engine;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums.BodyPart;
import com.whim.kenshi.api.Enums.MoveState;
import com.whim.kenshi.domain.Anatomy;
import com.whim.kenshi.domain.Character;

/**
 * Per-world-second survival bookkeeping: hunger decay, starvation damage, bleed
 * drain, natural clotting, and — when fed and out of combat — HP healing and
 * blood regeneration. The DOWNED/DEAD state itself is derived by the domain's
 * {@link Character#moveState()} from anatomy + blood; this system only maintains
 * the inputs and logs the transitions it causes.
 */
final class SurvivalSystem {

    /** Bleed rate lost to natural clotting per world-second. */
    static final double CLOT_PER_SEC = 0.20;
    /** Starvation HP damage per world-second (applied to Stomach) at empty hunger. */
    static final double STARVE_DMG_PER_SEC = 0.15;
    /** Hunger must exceed this fraction of max for passive healing to occur. */
    static final double WELL_FED_FRACTION = 0.25;

    private final EventLog log;

    SurvivalSystem(EventLog log) {
        this.log = log;
    }

    /**
     * Advance one character by {@code dtWorld} world-seconds.
     *
     * @param inCombat true if the character attacked or was attacked this step
     *                 (suppresses passive healing / blood regen).
     */
    void step(long tick, Character c, double dtWorld, boolean inCombat) {
        MoveState before = c.moveState();
        boolean wasAlive = before != MoveState.DEAD;
        if (!wasAlive) {
            return; // the dead stay dead
        }

        // --- Hunger ---
        double hunger = c.hunger() - Config.HUNGER_DECAY_PER_SEC * dtWorld;
        if (hunger < 0) { hunger = 0; }
        c.setHunger(hunger);

        // --- Starvation ---
        if (hunger <= 0.0) {
            c.anatomy().damage(BodyPart.STOMACH, STARVE_DMG_PER_SEC * dtWorld);
        }

        // --- Bleeding ---
        double bleed = c.bleedRate();
        if (bleed > 0.0) {
            double blood = c.blood() - bleed * dtWorld;
            if (blood < 0) { blood = 0; }
            c.setBlood(blood);
            double newBleed = bleed - CLOT_PER_SEC * dtWorld;
            if (newBleed < 0) { newBleed = 0; }
            c.setBleedRate(newBleed);
        } else if (!inCombat) {
            // --- Blood regen (only when stable and safe) ---
            double blood = c.blood() + Config.BLOOD_REGEN_PER_SEC * dtWorld;
            if (blood > Config.BLOOD_MAX) { blood = Config.BLOOD_MAX; }
            c.setBlood(blood);
        }

        // --- Passive healing when fed and not fighting ---
        if (!inCombat && c.bleedRate() <= 0.0 && hunger > Config.HUNGER_MAX * WELL_FED_FRACTION) {
            Anatomy a = c.anatomy();
            BodyPart[] parts = BodyPart.values();
            for (int i = 0; i < parts.length; i++) {
                a.heal(parts[i], Config.HEAL_PER_SEC * dtWorld);
            }
        }

        // --- Transition logging ---
        MoveState after = c.moveState();
        if (after != before) {
            if (after == MoveState.DEAD) {
                log.add(tick, c.name() + " has died");
            } else if (after == MoveState.DOWNED) {
                log.add(tick, c.name() + " collapses, unconscious");
            } else if (before == MoveState.DOWNED) {
                log.add(tick, c.name() + " regains consciousness");
            } else if (after == MoveState.CRAWLING) {
                log.add(tick, c.name() + " can no longer stand and starts crawling");
            }
        }
    }
}
