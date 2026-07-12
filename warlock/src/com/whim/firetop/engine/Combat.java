package com.whim.firetop.engine;

import com.whim.firetop.model.Character;
import com.whim.firetop.model.Monster;

/**
 * Fighting Fantasy combat: each round both sides roll 2d6 + SKILL for an Attack
 * Strength; the higher wounds the other for {@value #BASE_DAMAGE} STAMINA, ties
 * deal nothing. Luck may then be applied to press an attack or soften a blow.
 *
 * <p>The round-resolution method rolls dice; the individual damage-application
 * helpers are pure so unit tests can assert exact numbers without RNG.
 */
public final class Combat {

    /** Base STAMINA lost by the loser of a round. */
    public static final int BASE_DAMAGE = 2;

    private Combat() { }

    /** Who won a single attack round. */
    public enum Outcome { PLAYER_WINS, MONSTER_WINS, TIE }

    /** Full record of one attack round, before any luck is applied. */
    public static final class RoundResult {
        private final int playerAttackStrength;
        private final int monsterAttackStrength;
        private final Outcome outcome;

        public RoundResult(int playerAttackStrength, int monsterAttackStrength, Outcome outcome) {
            this.playerAttackStrength = playerAttackStrength;
            this.monsterAttackStrength = monsterAttackStrength;
            this.outcome = outcome;
        }

        public int getPlayerAttackStrength() { return playerAttackStrength; }
        public int getMonsterAttackStrength() { return monsterAttackStrength; }
        public Outcome getOutcome() { return outcome; }
    }

    /**
     * Rolls one attack round and applies the base {@value #BASE_DAMAGE} damage to
     * the loser. Does not apply luck (the caller decides whether to test).
     */
    public static RoundResult resolveRound(Character player, Monster monster, Dice dice) {
        int playerAS = dice.roll2d6() + player.getSkillCurrent();
        int monsterAS = dice.roll2d6() + monster.getSkill();
        Outcome outcome;
        if (playerAS > monsterAS) {
            outcome = Outcome.PLAYER_WINS;
            monster.wound(BASE_DAMAGE);
        } else if (monsterAS > playerAS) {
            outcome = Outcome.MONSTER_WINS;
            player.loseStamina(BASE_DAMAGE);
        } else {
            outcome = Outcome.TIE;
        }
        return new RoundResult(playerAS, monsterAS, outcome);
    }

    /**
     * Applies press-attack luck after the player has wounded the monster.
     * Lucky = 2 extra damage (4 total); Unlucky = the monster recovers 1 (net 1).
     * Assumes the base {@value #BASE_DAMAGE} was already applied by the round.
     *
     * @return true if lucky
     */
    public static boolean applyLuckToAttack(Character player, Monster monster, Dice dice) {
        LuckTest.Result r = LuckTest.test(player, dice);
        if (r.isLucky()) {
            monster.wound(2); // extra 2 → 4 total
        } else {
            monster.heal(1); // graze → only 1 net
        }
        return r.isLucky();
    }

    /**
     * Applies soften-blow luck after the monster has wounded the player.
     * Lucky = recover 1 (net 1 lost); Unlucky = lose 1 more (net 3 lost).
     * Assumes the base {@value #BASE_DAMAGE} was already applied by the round.
     *
     * @return true if lucky
     */
    public static boolean applyLuckToDefense(Character player, Dice dice) {
        LuckTest.Result r = LuckTest.test(player, dice);
        if (r.isLucky()) {
            player.gainStamina(1); // recover 1 → net 1 lost
        } else {
            player.loseStamina(1); // net 3 lost
        }
        return r.isLucky();
    }
}
