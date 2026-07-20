package com.heroquest.logic;

import com.heroquest.model.CombatDie;
import com.heroquest.model.Entity;

/**
 * Resolves opposed combat-dice rolls. The attacker rolls Attack Dice and counts
 * Skulls; the defender rolls Defend Dice and counts their own shield face. Each
 * shield cancels one Skull; the remaining Skulls are Body Points lost.
 */
public final class CombatEngine {
    private final Dice dice;

    public CombatEngine(Dice dice) {
        this.dice = dice;
    }

    /** Immutable summary of a single attack, for logging and UI feedback. */
    public static final class Result {
        public final int skulls;
        public final int blocks;
        public final int damage;
        public final boolean fatal;

        public Result(int skulls, int blocks, int damage, boolean fatal) {
            this.skulls = skulls;
            this.blocks = blocks;
            this.damage = damage;
            this.fatal = fatal;
        }
    }

    public Result resolveAttack(Entity attacker, Entity defender) {
        return resolveAttack(attacker, defender, attacker.getAttackDice(), defender.getDefendDice());
    }

    /** Resolve with explicit dice counts (used by spells / buffs). */
    public Result resolveAttack(Entity attacker, Entity defender, int attackDice, int defendDice) {
        int skulls = count(dice.rollCombat(attackDice), CombatDie.SKULL);
        int blocks = count(dice.rollCombat(defendDice), defender.defendingShield());
        int damage = Math.max(0, skulls - blocks);
        int dealt = defender.wound(damage);
        return new Result(skulls, blocks, dealt, !defender.isAlive());
    }

    private static int count(CombatDie[] roll, CombatDie face) {
        int n = 0;
        for (CombatDie d : roll) {
            if (d == face) {
                n++;
            }
        }
        return n;
    }
}
